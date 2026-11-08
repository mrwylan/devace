package info.wylan.devace.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import info.wylan.devace.persistence.FragenkatalogRepository;
import info.wylan.devace.persistence.Handlungsfeld;
import info.wylan.devace.persistence.HandlungsfeldRepository;
import info.wylan.devace.persistence.KatalogFrage;
import info.wylan.devace.persistence.RolleListItem;
import info.wylan.devace.persistence.RolleRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Erstellung von Fragenkatalog und Entscheidungsmodell (UC-006).
 */
// UC-006 GR-009: Nur der angemeldete Administrator erstellt Fragenkataloge und Entscheidungsmodelle.
@PreAuthorize("hasRole('ADMINISTRATOR')")
@Service
@Validated
@Transactional(readOnly = true)
public class FragenkatalogService {

	static final int STUFEN = 6;

	static final int MAX_LAENGE = 500;

	private final RolleRepository rolleRepository;

	private final HandlungsfeldRepository handlungsfeldRepository;

	private final FragenkatalogRepository katalogRepository;

	private final KiModellClient kiModell;

	public FragenkatalogService(RolleRepository rolleRepository, HandlungsfeldRepository handlungsfeldRepository,
			FragenkatalogRepository katalogRepository, KiModellClient kiModell) {
		this.rolleRepository = rolleRepository;
		this.handlungsfeldRepository = handlungsfeldRepository;
		this.katalogRepository = katalogRepository;
		this.kiModell = kiModell;
	}

	/**
	 * UC-006 step 2: die Rollen mit Version und Status ihrer Fragenkataloge.
	 */
	public List<RolleListItem> rollen() {
		return this.rolleRepository.findAll();
	}

	/**
	 * UC-006 steps 4 and 7: die sechs Handlungsfelder.
	 */
	public List<Handlungsfeld> handlungsfelder() {
		return this.handlungsfeldRepository.findAll();
	}

	/**
	 * UC-006 step 3: womit der Administrator für die Rolle beginnen kann.
	 */
	public Set<Ausgangspunkt> verfuegbareAusgangspunkte(long rolleId) {
		pruefeRolle(rolleId);
		// UC-006 GR-004: Besteht ein Entwurf, wird er fortgesetzt statt ein weiterer begonnen.
		if (this.katalogRepository.findEntwurf(rolleId).isPresent()) {
			return EnumSet.of(Ausgangspunkt.VORHANDENER_ENTWURF);
		}
		Set<Ausgangspunkt> punkte = EnumSet.of(Ausgangspunkt.LEER);
		// UC-006 GR-004: Die freigegebene Version steht nur zur Verfügung, wenn die Rolle eine hat.
		if (this.katalogRepository.findFreigegeben(rolleId).isPresent()) {
			punkte.add(Ausgangspunkt.FREIGEGEBENE_VERSION);
		}
		return punkte;
	}

	/**
	 * UC-006 step 4: die Fragen des gewählten Ausgangspunkts.
	 */
	public List<KatalogFrage> ausgangspunkt(long rolleId, Ausgangspunkt ausgangspunkt) {
		if (!verfuegbareAusgangspunkte(rolleId).contains(ausgangspunkt)) {
			throw new AusgangspunktNichtVerfuegbarException();
		}
		return switch (ausgangspunkt) {
			case LEER -> List.of();
			case FREIGEGEBENE_VERSION -> this.katalogRepository.findFreigegeben(rolleId).orElseThrow().fragen();
			case VORHANDENER_ENTWURF -> this.katalogRepository.findEntwurf(rolleId).orElseThrow().fragen();
		};
	}

	/**
	 * UC-006 steps 5–6: lässt das KI-Modell Fragen ergänzen, bis jedes Handlungsfeld die angegebene Zahl erreicht;
	 * A3 bei ungültigen Angaben, A2 wenn kein Entwurf entsteht, A6 wenn es nichts zu ergänzen gibt.
	 */
	public KiErgebnis kiEntwurf(long rolleId, List<KatalogFrage> aktuell, @Valid KiAngaben angaben) {
		List<Handlungsfeld> felder = this.handlungsfelder();
		Map<Long, Integer> vorhanden = zaehleJeHandlungsfeld(aktuell);
		List<KiModellClient.HandlungsfeldAnfrage> anfrage = new ArrayList<>();
		for (Handlungsfeld feld : felder) {
			// UC-006 GR-010: Je Handlungsfeld werden so viele Fragen ergänzt, bis die Zahl erreicht ist.
			int fehlend = angaben.anzahl() - vorhanden.getOrDefault(feld.id(), 0);
			if (fehlend > 0) {
				anfrage.add(new KiModellClient.HandlungsfeldAnfrage(feld.nummer(), feld.name(), feld.fokus(), fehlend));
			}
		}
		// UC-006 GR-010: Hat jedes Handlungsfeld genug Fragen, wird das KI-Modell nicht gefragt.
		if (anfrage.isEmpty()) {
			return new KiErgebnis.GenuegendFragen(angaben.anzahl());
		}
		return anfordern(rolleId, aktuell, felder, anfrage, angaben);
	}

	/**
	 * UC-006 A6: lässt das KI-Modell die angegebene Zahl weiterer Fragen je Handlungsfeld entwerfen.
	 */
	public KiErgebnis weitereFragen(long rolleId, List<KatalogFrage> aktuell, @Valid KiAngaben angaben) {
		List<Handlungsfeld> felder = this.handlungsfelder();
		List<KiModellClient.HandlungsfeldAnfrage> anfrage = felder.stream()
			.map(f -> new KiModellClient.HandlungsfeldAnfrage(f.nummer(), f.name(), f.fokus(), angaben.anzahl()))
			.toList();
		return anfordern(rolleId, aktuell, felder, anfrage, angaben);
	}

	private KiErgebnis anfordern(long rolleId, List<KatalogFrage> aktuell, List<Handlungsfeld> felder,
			List<KiModellClient.HandlungsfeldAnfrage> anfrage, KiAngaben angaben) {
		String rolle = rolleName(rolleId);
		// UC-006 GR-007: Die Anfrage enthält nur Rolle, Handlungsfelder, Schwerpunkte und Zahl der Fragen.
		KiModellClient.EntwurfAnfrage anfragen = new KiModellClient.EntwurfAnfrage(rolle, anfrage,
				angaben.schwerpunkte());
		List<KiModellClient.VorgeschlageneFrage> antwort;
		try {
			antwort = this.kiModell.fragenEntwerfen(anfragen);
		}
		catch (KiModellNichtErreichbarException ex) {
			throw new KeinKiEntwurfException();
		}
		Map<Integer, Handlungsfeld> nachNummer = new HashMap<>();
		felder.forEach(f -> nachNummer.put(f.nummer(), f));
		Map<Integer, Integer> angefordert = new HashMap<>();
		anfrage.forEach(a -> angefordert.put(a.nummer(), a.anzahl()));
		Map<Integer, Integer> uebernommen = new HashMap<>();
		// UC-006 GR-010: Der KI-Entwurf ändert und entfernt keine vorhandene Frage, er ergänzt nur.
		List<KatalogFrage> ergebnis = new ArrayList<>(aktuell);
		int neu = 0;
		for (KiModellClient.VorgeschlageneFrage frage : antwort) {
			Handlungsfeld feld = nachNummer.get(frage.handlungsfeldNummer());
			int grenze = angefordert.getOrDefault(frage.handlungsfeldNummer(), 0);
			if (feld == null || uebernommen.merge(feld.nummer(), 1, Integer::sum) > grenze) {
				continue;
			}
			// UC-006 GR-011: Eine unvollständige Frage erscheint im Entwurf und wird vor dem Speichern ergänzt.
			ergebnis.add(new KatalogFrage(feld.id(), frage.text(), frage.vorgaben()));
			neu++;
		}
		// UC-006 A2: Liefert das KI-Modell keine Frage, gilt der KI-Entwurf als nicht verwertbar.
		if (neu == 0) {
			throw new KeinKiEntwurfException();
		}
		return new KiErgebnis.Ergaenzt(List.copyOf(ergebnis));
	}

	/**
	 * UC-006 steps 9–11: prüft den Entwurf und speichert ihn als Fragenkatalog der Rolle mit dem Status Entwurf; A4
	 * bei unvollständigem oder ungültigem Entwurf.
	 */
	@Transactional
	// UC-006 GR-005: Nur der Entwurf ändert sich; freigegebene und archivierte Kataloge bleiben unverändert.
	public void speichern(long rolleId, List<KatalogFrage> entwurf) {
		pruefeRolle(rolleId);
		pruefeEntwurf(entwurf);
		Map<Long, Integer> reihenfolge = new HashMap<>();
		this.handlungsfelder().forEach(f -> reihenfolge.put(f.id(), f.nummer()));
		// UC-006 GR-002: Die Position ist eindeutig; die Fragen stehen nach Handlungsfeld geordnet.
		List<KatalogFrage> geordnet = entwurf.stream()
			.sorted(Comparator.comparingInt(f -> reihenfolge.getOrDefault(f.handlungsfeldId(), Integer.MAX_VALUE)))
			.map(f -> new KatalogFrage(f.handlungsfeldId(), f.text().strip(),
					f.vorgaben().stream().limit(STUFEN).map(String::strip).toList()))
			.toList();
		long katalogId = this.katalogRepository.findEntwurf(rolleId)
			.map(k -> k.id())
			// UC-006 GR-004: Ein neuer Katalog erhält die um 1 höhere Nummer als die höchste Version der Rolle.
			.orElseGet(() -> this.katalogRepository.insertEntwurf(rolleId,
					this.katalogRepository.hoechsteVersion(rolleId) + 1, LocalDateTime.now()));
		this.katalogRepository.ersetzeFragen(katalogId, geordnet);
	}

	private void pruefeEntwurf(List<KatalogFrage> entwurf) {
		List<EntwurfUngueltigException.Fehler> fehler = new ArrayList<>();
		for (int i = 0; i < entwurf.size(); i++) {
			KatalogFrage frage = entwurf.get(i);
			// UC-006 GR-002: Der Wortlaut ist eine Pflichtangabe mit höchstens 500 Zeichen.
			if (frage.text() == null || frage.text().isBlank()) {
				fehler.add(new EntwurfUngueltigException.Fehler(i, EntwurfUngueltigException.Fehler.WORTLAUT,
						"Wortlaut fehlt."));
			}
			else if (frage.text().strip().length() > MAX_LAENGE) {
				fehler.add(new EntwurfUngueltigException.Fehler(i, EntwurfUngueltigException.Fehler.WORTLAUT,
						"Höchstens 500 Zeichen."));
			}
			for (int stufe = 0; stufe < STUFEN; stufe++) {
				// UC-006 GR-001: Genau eine Vorgabe je Stufe 0 bis 5, Pflichtangabe mit höchstens 500 Zeichen.
				String vorgabe = (stufe < frage.vorgaben().size()) ? frage.vorgaben().get(stufe) : null;
				if (vorgabe == null || vorgabe.isBlank()) {
					fehler.add(new EntwurfUngueltigException.Fehler(i, stufe, "Vorgabe fehlt."));
				}
				else if (vorgabe.strip().length() > MAX_LAENGE) {
					fehler.add(new EntwurfUngueltigException.Fehler(i, stufe, "Höchstens 500 Zeichen."));
				}
			}
		}
		if (!fehler.isEmpty()) {
			throw new EntwurfUngueltigException(fehler);
		}
	}

	private static Map<Long, Integer> zaehleJeHandlungsfeld(List<KatalogFrage> fragen) {
		Map<Long, Integer> zaehler = new HashMap<>();
		fragen.forEach(f -> zaehler.merge(f.handlungsfeldId(), 1, Integer::sum));
		return zaehler;
	}

	private void pruefeRolle(long rolleId) {
		if (!this.rolleRepository.existsById(rolleId)) {
			throw new RolleNichtGefundenException();
		}
	}

	private String rolleName(long rolleId) {
		return this.rolleRepository.findAll()
			.stream()
			.filter(r -> r.id() == rolleId)
			.findFirst()
			.orElseThrow(RolleNichtGefundenException::new)
			.name();
	}

}
