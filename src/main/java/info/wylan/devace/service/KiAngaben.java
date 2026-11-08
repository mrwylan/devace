package info.wylan.devace.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * UC-006 step 5: Zahl der Fragen je Handlungsfeld und optionale Schwerpunkte für den KI-Entwurf.
 */
public record KiAngaben(
		// UC-006 GR-003: Die Zahl der Fragen je Handlungsfeld ist eine ganze Zahl von 1 bis 10.
		@Min(value = 1, message = "Die Zahl muss zwischen 1 und 10 liegen.") @Max(value = 10,
				message = "Die Zahl muss zwischen 1 und 10 liegen.") int anzahl,
		// UC-006 GR-003: Die Schwerpunkte sind optional und haben höchstens 2000 Zeichen.
		@Nullable @Size(max = 2000, message = "Höchstens 2000 Zeichen.") String schwerpunkte) {

	public KiAngaben {
		schwerpunkte = (schwerpunkte != null && !schwerpunkte.isBlank()) ? schwerpunkte.strip() : null;
	}

}
