package info.wylan.devace.persistence;

import org.jspecify.annotations.Nullable;

/**
 * Rolle zum Anlegen und Ändern. Die id ist beim Anlegen {@code null}.
 */
public record Rolle(@Nullable Long id, String name) {
}
