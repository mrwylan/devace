package info.wylan.devace.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * UC-010 step 3: Angaben einer Zuordnung. Umgebende Leerzeichen werden entfernt, eine leere Begründung entfällt.
 */
public record MatrixZuordnungEingabe(
		// UC-010 BR-001: Die Spalte gehört zu genau einem Handlungsfeld (OQ-096: Pflichtangabe).
		@NotNull(message = "Pflichtangabe fehlt.") @Nullable Long handlungsfeldId,
		// UC-010 BR-001: Der Name der Spalte ist Pflichtangabe mit höchstens 200 Zeichen.
		@NotBlank(message = "Pflichtangabe fehlt.") @Size(max = 200,
				message = "Höchstens 200 Zeichen.") @Nullable String matrixSpalte,
		// UC-010 BR-001: Die Begründung ist optional mit höchstens 500 Zeichen.
		@Size(max = 500, message = "Höchstens 500 Zeichen.") @Nullable String begruendung) {

	public MatrixZuordnungEingabe {
		matrixSpalte = (matrixSpalte != null) ? matrixSpalte.strip() : null;
		begruendung = (begruendung != null && !begruendung.isBlank()) ? begruendung.strip() : null;
	}

}
