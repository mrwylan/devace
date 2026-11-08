package info.wylan.devace.service;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Das KI-Modell, wie UC-006 es aufruft.
 */
public interface KiModellClient {

	/**
	 * UC-006 step 6: lässt zusätzliche Fragen mit je einem Entscheidungsmodell entwerfen.
	 *
	 * @throws KiModellNichtErreichbarException wenn das KI-Modell nicht antwortet (A2)
	 */
	List<VorgeschlageneFrage> fragenEntwerfen(EntwurfAnfrage anfrage);

	/**
	 * UC-006 GR-007: nur Rolle, Handlungsfelder, Schwerpunkte und Zahl der Fragen, nichts zu einer Person.
	 */
	record EntwurfAnfrage(String rolle, List<HandlungsfeldAnfrage> handlungsfelder, @Nullable String schwerpunkte) {
	}

	record HandlungsfeldAnfrage(int nummer, String name, @Nullable String fokus, int anzahl) {
	}

	/** {@code vorgaben} hat immer sechs Einträge (Stufe 0 bis 5); eine fehlende Vorgabe ist leer. */
	record VorgeschlageneFrage(int handlungsfeldNummer, String text, List<String> vorgaben) {
	}

}
