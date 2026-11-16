package info.wylan.devace.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import info.wylan.devace.persistence.BenutzerKonto;
import info.wylan.devace.persistence.BenutzerRepository;
import info.wylan.devace.persistence.TestfallRepository;
import info.wylan.devace.persistence.TestfallRepository.AuszufuehrenderTestfall;
import info.wylan.devace.persistence.TestfallRepository.PruefbarerKatalog;
import info.wylan.devace.persistence.TestfallRepository.TestFrage;
import info.wylan.devace.persistence.TestlaufRepository;
import info.wylan.devace.persistence.TestlaufRepository.Testergebnis;
import info.wylan.devace.persistence.TestlaufRepository.Testlauf;
import info.wylan.devace.service.KiModellClient.Bewertung;
import info.wylan.devace.service.KiModellClient.BewertungAnfrage;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * UC-009 KI-Bewertung mit Testfällen absichern. Der Administrator pflegt Testfälle zu den Fragen eines prüfbaren
 * Fragenkatalogs und führt sie aus; die Ausführung ändert weder Fragen noch Entscheidungsmodell (BR-009).
 */
@Service
// UC-009 BR-011: Nur der angemeldete Administrator pflegt und führt Testfälle aus.
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class TestfallService {

	/** UC-009 BR-001: höchstens 500 Zeichen für die vorgegebene Antwort. */
	static final int MAX_ANTWORT = 500;

	/** UC-009 BR-006: Zielwert der Trefferquote in Prozent. */
	public static final BigDecimal ZIELWERT = new BigDecimal("90");

	private final TestfallRepository testfallRepository;

	private final TestlaufRepository testlaufRepository;

	private final BenutzerRepository benutzerRepository;

	private final KiModellClient kiModell;

	private final TransactionTemplate transaktion;

	private final Clock clock;

	public TestfallService(TestfallRepository testfallRepository, TestlaufRepository testlaufRepository,
			BenutzerRepository benutzerRepository, KiModellClient kiModell, TransactionTemplate transaktion,
			Clock clock) {
		this.testfallRepository = testfallRepository;
		this.testlaufRepository = testlaufRepository;
		this.benutzerRepository = benutzerRepository;
		this.kiModell = kiModell;
		this.transaktion = transaktion;
		this.clock = clock;
	}

	/**
	 * UC-009 step 2: die prüfbaren Fragenkataloge mit der Zahl ihrer Testfälle und dem jüngsten Testlauf; leer bei A1.
	 */
	@Transactional(readOnly = true)
	public List<PruefbarerKatalog> pruefbareKataloge() {
		return this.testfallRepository.pruefbareKataloge();
	}

	/** UC-009 step 4: die Testfälle je Frage des gewählten Fragenkatalogs. */
	@Transactional(readOnly = true)
	public List<TestFrage> testfaelle(long katalogId) {
		pruefePruefbar(katalogId);
		return this.testfallRepository.fragenMitTestfaellen(katalogId);
	}

	/** UC-009 step 4 und BR-007: die bisherigen Testläufe des Fragenkatalogs, der jüngste zuerst. */
	@Transactional(readOnly = true)
	public List<Testlauf> testlaeufe(long katalogId) {
		pruefePruefbar(katalogId);
		return this.testlaufRepository.laeufe(katalogId);
	}

	/** UC-009 BR-007: die Ergebnisse eines früheren Testlaufs. */
	@Transactional(readOnly = true)
	public List<Testergebnis> ergebnisse(long laufId) {
		return this.testlaufRepository.ergebnisse(laufId);
	}

	/** UC-009 step 5: legt einen Testfall zu einer Frage des Fragenkatalogs an; A2 bei ungültigen Angaben. */
	@Transactional
	public long testfallAnlegen(long frageId, @Nullable String antwort, @Nullable Integer erwarteteStufe) {
		long katalogId = this.testfallRepository.katalogDerFrage(frageId)
			.orElseThrow(FragenkatalogNichtPruefbarException::new);
		pruefePruefbar(katalogId);
		pruefe(antwort, erwarteteStufe);
		return this.testfallRepository.insert(frageId, antwort.strip(), erwarteteStufe);
	}

	/** UC-009 step 5: ändert Antwort und erwartete Stufe eines Testfalls; A2 bei ungültigen Angaben. */
	@Transactional
	public void testfallAendern(long testfallId, @Nullable String antwort, @Nullable Integer erwarteteStufe) {
		pruefePruefbar(katalogDesTestfalls(testfallId));
		pruefe(antwort, erwarteteStufe);
		this.testfallRepository.update(testfallId, antwort.strip(), erwarteteStufe);
	}

	/** UC-009 step 5: entfernt einen Testfall; frühere Testläufe behalten ihr Ergebnis (BR-007). */
	@Transactional
	public void testfallEntfernen(long testfallId) {
		pruefePruefbar(katalogDesTestfalls(testfallId));
		this.testfallRepository.delete(testfallId);
	}

	/**
	 * UC-009 steps 6–9: führt alle Testfälle des Fragenkatalogs aus und speichert den Testlauf; A3 ohne Testfall, A4
	 * wenn das KI-Modell zu einem Testfall keine Stufe liefert. Die Aufrufe des KI-Modells liegen außerhalb der
	 * Transaktion; gespeichert wird nur ein vollständiger Testlauf.
	 */
	// UC-009 BR-009: Die Ausführung ändert weder Fragen noch Entscheidungsmodell, sie liest nur.
	public TestlaufErgebnis ausfuehren(long katalogId) {
		pruefePruefbar(katalogId);
		long administratorId = aktuellerAdministrator().id();
		// UC-009 BR-003: Ein Testlauf führt alle Testfälle des gewählten Fragenkatalogs aus.
		List<AuszufuehrenderTestfall> testfaelle = this.testfallRepository.auszufuehrende(katalogId);
		if (testfaelle.isEmpty()) {
			// UC-009 A3: Es gibt nichts auszuführen.
			throw new KeinTestfallException();
		}
		List<Testergebnis> ergebnisse = new ArrayList<>();
		Set<String> modelle = new LinkedHashSet<>();
		for (AuszufuehrenderTestfall testfall : testfaelle) {
			// UC-009 BR-004: Bewertung wie im Assessment (UC-002 BR-006).
			// UC-009 BR-008: nur Frage, Entscheidungsmodell und vorgegebene Antwort, nichts zu einer Person
			Optional<Bewertung> bewertung;
			try {
				bewertung = this.kiModell
					.bewerten(new BewertungAnfrage(testfall.frage(), testfall.vorgaben(), testfall.antworttext()));
			}
			catch (KiModellNichtErreichbarException ex) {
				// UC-009 A4: nicht erreichbar; es wird kein Testlauf gespeichert
				throw new TestlaufNichtAbgeschlossenException();
			}
			// UC-009 A4: Ohne Stufe zu einem Testfall wird der Testlauf nicht abgeschlossen.
			Bewertung ermittelt = bewertung.orElseThrow(TestlaufNichtAbgeschlossenException::new);
			String gemeldet = ermittelt.modell();
			if (gemeldet == null || gemeldet.isBlank()) {
				// UC-009 A4: Ohne gemeldete Version des KI-Modells wird der Testlauf nicht abgeschlossen (Schritt 8).
				throw new TestlaufNichtAbgeschlossenException();
			}
			modelle.add(gemeldet);
			ergebnisse.add(new Testergebnis(testfall.testfallId(), testfall.frage(), testfall.antworttext(),
					testfall.erwarteteStufe(), ermittelt.stufe()));
		}
		BigDecimal trefferquote = trefferquote(ergebnisse);
		String modell = kurz(String.join(", ", modelle), 100);
		LocalDateTime jetzt = LocalDateTime.now(this.clock);
		// UC-009 step 8: der Testlauf wird nur vollständig gespeichert (Fehlerfall: kein unvollständiger Testlauf).
		Long laufId = this.transaktion.execute((status) -> this.testlaufRepository.insert(katalogId, administratorId,
				jetzt, modell, trefferquote, ergebnisse));
		List<Testergebnis> abweichungen = ergebnisse.stream().filter(Testergebnis::abweichung).toList();
		return new TestlaufErgebnis((laufId != null) ? laufId : 0, trefferquote, bestanden(trefferquote), abweichungen);
	}

	/** UC-009 BR-006: Ein Testlauf unter 90 Prozent ist nicht bestanden. */
	public static boolean bestanden(BigDecimal trefferquote) {
		// UC-009 BR-006: Zielwert der Trefferquote ist 90 Prozent.
		return trefferquote.compareTo(ZIELWERT) >= 0;
	}

	// UC-009 BR-005: Anteil der Treffer in Prozent, kaufmännisch auf zwei Nachkommastellen (OQ-091).
	static BigDecimal trefferquote(List<Testergebnis> ergebnisse) {
		long treffer = ergebnisse.stream().filter((e) -> !e.abweichung()).count();
		return BigDecimal.valueOf(treffer * 100L).divide(BigDecimal.valueOf(ergebnisse.size()), 2, RoundingMode.HALF_UP);
	}

	// UC-009 BR-001: Antwort Pflicht, höchstens 500 Zeichen; erwartete Stufe 0 bis 5.
	private static void pruefe(@Nullable String antwort, @Nullable Integer erwarteteStufe) {
		Map<String, String> fehler = new LinkedHashMap<>();
		if (antwort == null || antwort.isBlank()) {
			fehler.put(TestfallUngueltigException.ANTWORT, "Antwort fehlt.");
		}
		else if (antwort.strip().length() > MAX_ANTWORT) {
			fehler.put(TestfallUngueltigException.ANTWORT, "Höchstens 500 Zeichen.");
		}
		if (erwarteteStufe == null || erwarteteStufe < 0 || erwarteteStufe > 5) {
			fehler.put(TestfallUngueltigException.STUFE, "Stufe fehlt.");
		}
		if (!fehler.isEmpty()) {
			// UC-009 A2: Das System speichert den Testfall nicht.
			throw new TestfallUngueltigException(fehler);
		}
	}

	private long katalogDesTestfalls(long testfallId) {
		return this.testfallRepository.katalogDesTestfalls(testfallId)
			.orElseThrow(FragenkatalogNichtPruefbarException::new);
	}

	// UC-009 BR-002: Nur Entwurf und freigegebene Version sind prüfbar.
	private void pruefePruefbar(long katalogId) {
		if (!this.testfallRepository.istPruefbar(katalogId)) {
			throw new FragenkatalogNichtPruefbarException();
		}
	}

	private BenutzerKonto aktuellerAdministrator() {
		String name = SecurityContextHolder.getContext().getAuthentication().getName();
		return this.benutzerRepository.findByBenutzername(name)
			.orElseThrow(() -> new IllegalStateException("Angemeldeter Benutzer ohne Konto"));
	}

	private static String kurz(String text, int laenge) {
		return (text.length() <= laenge) ? text : text.substring(0, laenge);
	}

	/** UC-009 step 9: Trefferquote, Bestanden nach BR-006 und die Abweichungen. */
	public record TestlaufErgebnis(long laufId, BigDecimal trefferquote, boolean bestanden,
			List<Testergebnis> abweichungen) {
	}

}
