package info.wylan.devace.service;

import java.util.List;

import info.wylan.devace.persistence.KatalogFrage;

/**
 * UC-006 step 6 und A6: Ergebnis der KI-Anforderung.
 */
public sealed interface KiErgebnis {

	/** Der Entwurf ist um die Fragen des KI-Modells ergänzt. */
	record Ergaenzt(List<KatalogFrage> entwurf) implements KiErgebnis {
	}

	/** A6: Jedes Handlungsfeld hat bereits mindestens {@code anzahl} Fragen; das KI-Modell wurde nicht gefragt. */
	record GenuegendFragen(int anzahl) implements KiErgebnis {
	}

}
