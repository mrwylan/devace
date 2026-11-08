package info.wylan.devace.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * UC-008 step 4: Angaben einer Rolle. Umgebende Leerzeichen werden entfernt.
 */
public record RolleEingabe(
		// UC-008 GR-001: Der Name einer Rolle ist Pflichtangabe mit höchstens 50 Zeichen.
		@NotBlank(message = "Pflichtangabe fehlt.") @Size(max = 50, message = "Höchstens 50 Zeichen.") String name) {

	public RolleEingabe {
		name = (name != null) ? name.strip() : null;
	}

}
