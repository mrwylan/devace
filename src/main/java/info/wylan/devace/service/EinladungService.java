package info.wylan.devace.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import info.wylan.devace.persistence.BenutzerKonto;
import info.wylan.devace.persistence.BenutzerRepository;
import info.wylan.devace.persistence.EinladungRepository;
import info.wylan.devace.persistence.MagListItem;
import info.wylan.devace.persistence.MagRepository;
import info.wylan.devace.persistence.RolleOption;
import info.wylan.devace.persistence.RolleRepository;
import info.wylan.devace.service.EinladungsangabenUngueltigException.Fehler;
import info.wylan.devace.service.EmailSystemClient.Einladungsmail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Einladungen erstellen und versenden (UC-001).
 */
// UC-001 BR-009: Nur die angemeldete Teamleitung erstellt Einladungen.
@PreAuthorize("hasRole('TEAMLEITUNG')")
@Service
@Transactional(readOnly = true)
public class EinladungService {

	static final int HOECHSTZAHL_ADRESSEN = 50;

	static final String BETREFF = "Einladung zum devace-Assessment";

	private static final String TEXT = "Sie sind zu einem devace-Assessment eingeladen. "
			+ "Über den folgenden Link starten Sie es:";

	// UC-001 BR-006: lokaler Teil, @, Domänenteil mit mindestens einem Punkt, keine Leerzeichen.
	private static final Pattern ADRESSE = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

	private final EinladungRepository einladungRepository;

	private final MagRepository magRepository;

	private final RolleRepository rolleRepository;

	private final BenutzerRepository benutzerRepository;

	private final EmailSystemClient emailSystem;

	private final Clock clock;

	private final String basisUrl;

	public EinladungService(EinladungRepository einladungRepository, MagRepository magRepository,
			RolleRepository rolleRepository, BenutzerRepository benutzerRepository, EmailSystemClient emailSystem,
			Clock clock, @Value("${devace.einladung.basis-url}") String basisUrl) {
		this.einladungRepository = einladungRepository;
		this.magRepository = magRepository;
		this.rolleRepository = rolleRepository;
		this.benutzerRepository = benutzerRepository;
		this.emailSystem = emailSystem;
		this.clock = clock;
		this.basisUrl = basisUrl.endsWith("/") ? basisUrl.substring(0, basisUrl.length() - 1) : basisUrl;
	}

	/**
	 * UC-001 step 2: die MAGs der Teamleitung und die Rollen mit freigegebenem Fragenkatalog; A2 wenn es keine
	 * solche Rolle gibt.
	 */
	public Auswahl auswahl() {
		List<RolleOption> rollen = this.rolleRepository.findMitFreigegebenemFragenkatalog();
		if (rollen.isEmpty()) {
			throw new KeineRolleMitFreigabeException();
		}
		return new Auswahl(this.magRepository.findByTeamleitung(aktuelleTeamleitung().id()), rollen);
	}

	/**
	 * UC-001 steps 5–6: prüft die Angaben und liefert die Zusammenfassung; A1 bei fehlenden oder ungültigen
	 * Angaben.
	 */
	public Zusammenfassung pruefen(EinladungsAngaben angaben) {
		Gepruefte gepruefte = pruefe(angaben, aktuelleTeamleitung());
		return new Zusammenfassung(gepruefte.adressen().size(), gepruefte.mag(), gepruefte.rolle(),
				angaben.gueltigkeitTage());
	}

	/**
	 * UC-001 steps 8–10: erstellt je Adresse eine Einladung, versendet sie und liefert die Zahl der versendeten
	 * Einladungen und die Adressen, bei denen der Versand scheiterte (A4). Jede Einladung hat ihre eigene
	 * Transaktion, damit kein Datenbankzugriff auf die Antwort des E-Mail-Systems wartet.
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public Ergebnis erstellenUndVersenden(EinladungsAngaben angaben) {
		BenutzerKonto teamleitung = aktuelleTeamleitung();
		Gepruefte gepruefte = pruefe(angaben, teamleitung);
		int versendet = 0;
		List<String> nichtVersendet = new ArrayList<>();
		for (String adresse : gepruefte.adressen()) {
			Angelegt einladung = legeEinladungAn(gepruefte, teamleitung, angaben.gueltigkeitTage());
			try {
				// UC-001 BR-004: Die Adresse dient nur dem Versand; sie wird weder gespeichert noch weitergegeben.
				this.emailSystem.einladungVersenden(new Einladungsmail(adresse, BETREFF, text(einladung.referenz())));
				this.einladungRepository.markiereVersendet(einladung.id());
				versendet++;
			}
			catch (EmailVersandFehlgeschlagenException ex) {
				// UC-001 A4: Die Einladung dieser Adresse wird verworfen, die übrigen werden weiter versendet.
				this.einladungRepository.loesche(einladung.id());
				nichtVersendet.add(adresse);
			}
		}
		return new Ergebnis(versendet, nichtVersendet);
	}

	private Angelegt legeEinladungAn(Gepruefte gepruefte, BenutzerKonto teamleitung, int gueltigkeitTage) {
		for (int versuch = 0;; versuch++) {
			String referenz = UUID.randomUUID().toString();
			try {
				// UC-001 BR-001: Die Referenz ist zufällig und eindeutig, ohne Namen oder Adresse einer Person.
				LocalDateTime erstelltAm = LocalDateTime.now(this.clock);
				// UC-001 BR-008: Ablaufdatum bei der Erstellung; der Erstellungstag ist der erste Tag, bis 24 Uhr.
				LocalDate laeuftAbAm = erstelltAm.toLocalDate().plusDays(gueltigkeitTage - 1L);
				long id = this.einladungRepository.insert(referenz, gepruefte.mag().id(), gepruefte.rolle().id(),
						teamleitung.id(), erstelltAm, gueltigkeitTage, laeuftAbAm);
				return new Angelegt(id, referenz);
			}
			catch (DuplicateKeyException ex) {
				// eine bereits vergebene Referenz (praktisch ausgeschlossen): eine neue ziehen
				if (versuch >= 2) {
					throw ex;
				}
			}
		}
	}

	private String text(String referenz) {
		// UC-001 BR-007: Betreff, ein kurzer Text und der Link zur Einladung, sonst nichts.
		return TEXT + "\n\n" + this.basisUrl + "/assessment/" + referenz;
	}

	private Gepruefte pruefe(EinladungsAngaben angaben, BenutzerKonto teamleitung) {
		List<String> adressen = pruefeAdressen(angaben, new ArrayList<>());
		// UC-001 BR-002: Einladungen nur für MAGs, die der Teamleitung zugeordnet sind.
		MagListItem mag = this.magRepository.findByTeamleitung(teamleitung.id())
			.stream()
			.filter((kandidat) -> kandidat.id() == angaben.magId())
			.findFirst()
			.orElseThrow(MagNichtZugeordnetException::new);
		// UC-001 BR-003: Nur Rollen mit freigegebenem Fragenkatalog sind wählbar.
		RolleOption rolle = this.rolleRepository.findMitFreigegebenemFragenkatalog()
			.stream()
			.filter((kandidat) -> kandidat.id() == angaben.rolleId())
			.findFirst()
			.orElseThrow(RolleOhneFreigabeException::new);
		return new Gepruefte(mag, rolle, adressen);
	}

	private static List<String> pruefeAdressen(EinladungsAngaben angaben, List<Fehler> fehler) {
		// UC-001 BR-005: Dieselbe Adresse mehrfach ergibt eine Einladung (ohne Schreibweise der Groß- und Kleinbuchstaben).
		Map<String, String> verschiedene = new LinkedHashMap<>();
		for (String roh : angaben.adressen()) {
			String adresse = (roh != null) ? roh.strip() : "";
			if (!adresse.isEmpty()) {
				verschiedene.putIfAbsent(adresse.toLowerCase(Locale.ROOT), adresse);
			}
		}
		List<String> adressen = new ArrayList<>(verschiedene.values());
		// UC-001 BR-006: Eine leere Angabe enthält keine Adresse.
		if (adressen.isEmpty()) {
			fehler.add(new Fehler(EinladungsangabenUngueltigException.ADRESSEN, "Bitte mindestens eine Adresse eingeben."));
		}
		// UC-001 BR-010: Ein Durchgang enthält höchstens 50 Adressen; doppelte zählen einmal.
		else if (adressen.size() > HOECHSTZAHL_ADRESSEN) {
			fehler.add(new Fehler(EinladungsangabenUngueltigException.ADRESSEN, "Höchstens 50 Adressen je Durchgang."));
		}
		else {
			adressen.stream()
				.filter((adresse) -> !ADRESSE.matcher(adresse).matches())
				.findFirst()
				.ifPresent((ungueltig) -> fehler.add(new Fehler(EinladungsangabenUngueltigException.ADRESSEN,
						"Mindestens eine Adresse ist ungültig: " + ungueltig + ". Bitte korrigieren.")));
		}
		// UC-001 BR-008: Die Gültigkeitsdauer ist eine ganze Zahl von 1 bis 30 Tagen.
		if (angaben.gueltigkeitTage() < 1 || angaben.gueltigkeitTage() > 30) {
			fehler.add(new Fehler(EinladungsangabenUngueltigException.GUELTIGKEIT,
					"Die Zahl muss zwischen 1 und 30 liegen."));
		}
		if (!fehler.isEmpty()) {
			throw new EinladungsangabenUngueltigException(fehler);
		}
		return adressen;
	}

	private BenutzerKonto aktuelleTeamleitung() {
		String name = SecurityContextHolder.getContext().getAuthentication().getName();
		return this.benutzerRepository.findByBenutzername(name)
			.orElseThrow(() -> new IllegalStateException("Angemeldeter Benutzer ohne Konto"));
	}

	/** Die Auswahl der Teamleitung: ihre MAGs und die Rollen mit freigegebenem Fragenkatalog. */
	public record Auswahl(List<MagListItem> mags, List<RolleOption> rollen) {
	}

	/** Die Angaben der Teamleitung; die Adressen stehen so, wie sie sie eingegeben hat. */
	public record EinladungsAngaben(long magId, long rolleId, List<String> adressen, int gueltigkeitTage) {
	}

	/** UC-001 step 6: Anzahl der Einladungen, MAG, Rolle und Gültigkeitsdauer. */
	public record Zusammenfassung(int anzahl, MagListItem mag, RolleOption rolle, int gueltigkeitTage) {
	}

	/** UC-001 step 10: die Zahl der versendeten Einladungen und die Adressen, bei denen der Versand scheiterte. */
	public record Ergebnis(int versendet, List<String> nichtVersendet) {
	}

	private record Angelegt(long id, String referenz) {
	}

	private record Gepruefte(MagListItem mag, RolleOption rolle, List<String> adressen) {
	}

}
