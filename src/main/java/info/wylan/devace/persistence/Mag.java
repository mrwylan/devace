package info.wylan.devace.persistence;

import org.jspecify.annotations.Nullable;

/**
 * MAG zum Anlegen und Ändern. Die id ist beim Anlegen {@code null}.
 */
public record Mag(@Nullable Long id, String scrumTeam, String linienteam, @Nullable Long teamleitungId) {
}
