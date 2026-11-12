package info.wylan.devace.service;

import java.util.List;

/**
 * UC-007 step 6: Rolle, Version und Zahl der Fragen je Handlungsfeld des Entwurfs, der freigegeben werden soll.
 */
public record FreigabeZusammenfassung(long rolleId, String rolle, int version, List<HandlungsfeldAnzahl> fragen) {

	/** Die Zahl der Fragen eines Handlungsfelds. */
	public record HandlungsfeldAnzahl(String handlungsfeld, int anzahl) {
	}

}
