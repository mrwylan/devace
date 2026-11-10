package info.wylan.devace.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * UC-008 step 4: Angaben einer MAG. Umgebende Leerzeichen werden entfernt.
 */
public record MagEingabe(
		// UC-008 BR-001: Scrum Team ist Pflichtangabe mit höchstens 100 Zeichen.
		@NotBlank(message = "Pflichtangabe fehlt.") @Size(max = 100, message = "Höchstens 100 Zeichen.") String scrumTeam,
		// UC-008 BR-001: Linienteam ist Pflichtangabe mit höchstens 100 Zeichen.
		@NotBlank(message = "Pflichtangabe fehlt.") @Size(max = 100, message = "Höchstens 100 Zeichen.") String linienteam,
		@Nullable Long teamleitungId) {

	public MagEingabe {
		scrumTeam = (scrumTeam != null) ? scrumTeam.strip() : null;
		linienteam = (linienteam != null) ? linienteam.strip() : null;
	}

}
