package info.wylan.devace.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import info.wylan.devace.service.KiModellClient.Bewertung;
import info.wylan.devace.service.KiModellClient.EntwurfAnfrage;
import info.wylan.devace.service.KiModellClient.HandlungsfeldAnfrage;
import info.wylan.devace.service.KiModellClient.VorgeschlageneFrage;

/**
 * Fake des KI-Modells für die Tests der Schichten darüber: kein Aufruf eines echten Modells.
 *
 * <p>
 * Ohne Vorgabe liefert es je angefragtem Handlungsfeld die verlangte Zahl Fragen mit sechs Vorgaben.
 * {@link #failNext()} löst A2 aus, {@link #antwortet} legt die nächste Antwort fest, {@link #anfragen()} zeigt, was
 * gesendet wurde.
 */
public class KiModellClientFake implements KiModellClient {

	private final List<EntwurfAnfrage> anfragen = new ArrayList<>();

	private RuntimeException naechsterFehler;

	private List<VorgeschlageneFrage> naechsteAntwort;

	@Override
	public List<VorgeschlageneFrage> fragenEntwerfen(EntwurfAnfrage anfrage) {
		this.anfragen.add(anfrage);
		if (this.naechsterFehler != null) {
			RuntimeException fehler = this.naechsterFehler;
			this.naechsterFehler = null;
			throw fehler;
		}
		if (this.naechsteAntwort != null) {
			List<VorgeschlageneFrage> antwort = this.naechsteAntwort;
			this.naechsteAntwort = null;
			return antwort;
		}
		List<VorgeschlageneFrage> fragen = new ArrayList<>();
		for (HandlungsfeldAnfrage feld : anfrage.handlungsfelder()) {
			for (int i = 1; i <= feld.anzahl(); i++) {
				fragen.add(new VorgeschlageneFrage(feld.nummer(), "KI-Frage " + feld.nummer() + "." + i + "?",
						List.of("Vorgabe 0", "Vorgabe 1", "Vorgabe 2", "Vorgabe 3", "Vorgabe 4", "Vorgabe 5")));
			}
		}
		return fragen;
	}

	private final List<BewertungAnfrage> bewertungen = new ArrayList<>();

	private RuntimeException naechsterBewertungsfehler;

	private OptionalInt naechsteStufe;

	/** UC-009: der Reihe nach je Bewertung eine Stufe, leer heißt keine Zuordnung; danach wieder die Vorgabe. */
	private final java.util.ArrayDeque<OptionalInt> stufenFolge = new java.util.ArrayDeque<>();

	/** UC-009 step 8: das Modell, das der Fake als verwendet meldet. */
	public static final String MODELL = "test/jev-fake";

	@Override
	public OptionalInt antwortBewerten(BewertungAnfrage anfrage) {
		this.bewertungen.add(anfrage);
		if (this.naechsterBewertungsfehler != null) {
			RuntimeException fehler = this.naechsterBewertungsfehler;
			this.naechsterBewertungsfehler = null;
			throw fehler;
		}
		if (this.naechsteStufe != null) {
			OptionalInt stufe = this.naechsteStufe;
			this.naechsteStufe = null;
			return stufe;
		}
		if (!this.stufenFolge.isEmpty()) {
			return this.stufenFolge.poll();
		}
		return OptionalInt.of(3);
	}

	/** UC-009 A4: die nächste Bewertung meldet kein Modell. */
	private boolean naechsteOhneModell;

	@Override
	public Optional<Bewertung> bewerten(BewertungAnfrage anfrage) {
		OptionalInt stufe = antwortBewerten(anfrage);
		String modell = this.naechsteOhneModell ? null : MODELL;
		this.naechsteOhneModell = false;
		return stufe.isPresent() ? Optional.of(new Bewertung(stufe.getAsInt(), modell)) : Optional.empty();
	}

	/** UC-009 A4: die nächste Bewertung ordnet eine Stufe zu, meldet aber kein Modell. */
	public void meldetBeiDerNaechstenKeinModell() {
		this.naechsteOhneModell = true;
	}

	/** UC-009: die folgenden Bewertungen liefern der Reihe nach diese Stufen; -1 heißt keine Zuordnung (A4). */
	public void bewertetDerReiheNachMit(int... stufen) {
		for (int stufe : stufen) {
			this.stufenFolge.add((stufe < 0) ? OptionalInt.empty() : OptionalInt.of(stufe));
		}
	}

	/** UC-002 A5: das KI-Modell ist bei der nächsten Bewertung nicht erreichbar. */
	public void failNextBewertung() {
		this.naechsterBewertungsfehler = new KiModellNichtErreichbarException(new IllegalStateException("Test"));
	}

	/** UC-002 BR-006: die nächste Bewertung liefert diese Stufe, leer heißt keine Zuordnung. */
	public void bewertetNaechsteMit(OptionalInt stufe) {
		this.naechsteStufe = stufe;
	}

	/** UC-002 BR-007: die Anfragen der Bewertung. */
	public List<BewertungAnfrage> bewertungen() {
		return this.bewertungen;
	}

	private final List<TextreportAnfrage> textreportAnfragen = new ArrayList<>();

	private RuntimeException naechsterTextreportFehler;

	private boolean naechsterTextreportLeer;

	private Textreport naechsterTextreport;

	@Override
	public Optional<Textreport> textreportErstellen(TextreportAnfrage anfrage) {
		this.textreportAnfragen.add(anfrage);
		if (this.naechsterTextreportFehler != null) {
			RuntimeException fehler = this.naechsterTextreportFehler;
			this.naechsterTextreportFehler = null;
			throw fehler;
		}
		if (this.naechsterTextreportLeer) {
			this.naechsterTextreportLeer = false;
			return Optional.empty();
		}
		if (this.naechsterTextreport != null) {
			Textreport textreport = this.naechsterTextreport;
			this.naechsterTextreport = null;
			return Optional.of(textreport);
		}
		return Optional.of(new Textreport("Überblick des KI-Modells.", anfrage.handlungsfelder()
			.stream()
			.map(feld -> new Abschnitt(feld.name(), "Würdigung " + feld.name() + ".", "Nächster Schritt " + feld.name() + "."))
			.toList()));
	}

	/** UC-003 A1: das KI-Modell ist beim nächsten Textreport nicht erreichbar. */
	public void failNextTextreport() {
		this.naechsterTextreportFehler = new KiModellNichtErreichbarException(new IllegalStateException("Test"));
	}

	/** UC-003 A1: das KI-Modell liefert beim nächsten Mal keinen verwertbaren Textreport. */
	public void liefertNaechstenTextreportNicht() {
		this.naechsterTextreportLeer = true;
	}

	/** UC-003 BR-003: der nächste Textreport hat diesen Inhalt. */
	public void liefertNaechstenTextreport(Textreport textreport) {
		this.naechsterTextreport = textreport;
	}

	/** UC-003 BR-004: die Anfragen für den Textreport. */
	public List<TextreportAnfrage> textreportAnfragen() {
		return this.textreportAnfragen;
	}

	/** A2: das KI-Modell ist nicht erreichbar. */
	public void failNext() {
		this.naechsterFehler = new KiModellNichtErreichbarException(new IllegalStateException("stubbed"));
	}

	/** Legt die nächste Antwort fest, zum Beispiel leer (kein Entwurf) oder mit unvollständigen Vorgaben (BR-011). */
	public void antwortet(List<VorgeschlageneFrage> antwort) {
		this.naechsteAntwort = antwort;
	}

	public List<EntwurfAnfrage> anfragen() {
		return List.copyOf(this.anfragen);
	}

	public void reset() {
		this.anfragen.clear();
		this.naechsterFehler = null;
		this.naechsteAntwort = null;
		this.bewertungen.clear();
		this.naechsterBewertungsfehler = null;
		this.naechsteStufe = null;
		this.stufenFolge.clear();
		this.naechsteOhneModell = false;
		this.textreportAnfragen.clear();
		this.naechsterTextreportFehler = null;
		this.naechsterTextreportLeer = false;
		this.naechsterTextreport = null;
	}

}
