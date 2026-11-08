package info.wylan.devace.persistence;

import org.jspecify.annotations.Nullable;

/**
 * UC-008 step 2: eine MAG der Übersicht mit der Teamleitung, falls zugeordnet.
 */
public record MagListItem(long id, String scrumTeam, String linienteam, @Nullable Long teamleitungId,
		@Nullable String teamleitung) {
}
