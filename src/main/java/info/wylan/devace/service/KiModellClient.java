package info.wylan.devace.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import org.jspecify.annotations.Nullable;

/**
 * Das KI-Modell, wie UC-006, UC-002 und UC-003 es aufrufen.
 */
public interface KiModellClient {

	/**
	 * UC-006 step 6: lässt zusätzliche Fragen mit je einem Entscheidungsmodell entwerfen.
	 *
	 * @throws KiModellNichtErreichbarException wenn das KI-Modell nicht antwortet (A2)
	 */
	List<VorgeschlageneFrage> fragenEntwerfen(EntwurfAnfrage anfrage);

	/**
	 * UC-002 step 6: lässt eine Antwort nach den Stufenvorgaben der Frage einer Stufe von 0 bis 5 zuordnen. Liefert
	 * leer, wenn das KI-Modell keine gültige Stufe zuordnet (A5).
	 *
	 * @throws KiModellNichtErreichbarException wenn das KI-Modell nicht antwortet (A5)
	 */
	OptionalInt antwortBewerten(BewertungAnfrage anfrage);

	/**
	 * UC-002 BR-007: nur die Frage, ihre Stufenvorgaben und der Text der Antwort, nichts zu einer Person und keine
	 * Einladung. {@code vorgaben} hat die Beschreibung je Stufe, der Index ist die Stufe.
	 */
	record BewertungAnfrage(String frage, List<String> vorgaben, String antwort) {
	}

	/**
	 * UC-003 step 3: lässt den Textreport erstellen. Liefert leer, wenn das KI-Modell keinen verwertbaren Textreport
	 * liefert (A1).
	 *
	 * @throws KiModellNichtErreichbarException wenn das KI-Modell nicht antwortet (A1)
	 */
	Optional<Textreport> textreportErstellen(TextreportAnfrage anfrage);

	/**
	 * UC-003 BR-004: nur die Rolle, die Handlungsfelder mit Score, Fragen, Antworten und Stufen; keine Einladung,
	 * keine E-Mail-Adresse, keine Angabe zur Person.
	 */
	record TextreportAnfrage(String rolle, List<HandlungsfeldAuswertung> handlungsfelder) {
	}

	record HandlungsfeldAuswertung(String name, BigDecimal score, List<BewerteteFrage> fragen) {
	}

	record BewerteteFrage(String frage, String antwort, int stufe) {
	}

	/** UC-003 BR-003: Überblick und je Handlungsfeld die Würdigung mit Begründung und der nächste Schritt. */
	record Textreport(String ueberblick, List<Abschnitt> abschnitte) {
	}

	record Abschnitt(String handlungsfeld, String wuerdigung, String naechsterSchritt) {
	}

	/**
	 * UC-006 BR-007: nur Rolle, Handlungsfelder, Schwerpunkte und Zahl der Fragen, nichts zu einer Person.
	 */
	record EntwurfAnfrage(String rolle, List<HandlungsfeldAnfrage> handlungsfelder, @Nullable String schwerpunkte) {
	}

	record HandlungsfeldAnfrage(int nummer, String name, @Nullable String fokus, int anzahl) {
	}

	/** {@code vorgaben} hat immer sechs Einträge (Stufe 0 bis 5); eine fehlende Vorgabe ist leer. */
	record VorgeschlageneFrage(int handlungsfeldNummer, String text, List<String> vorgaben) {
	}

}
