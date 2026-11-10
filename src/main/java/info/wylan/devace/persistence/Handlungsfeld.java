package info.wylan.devace.persistence;

import org.jspecify.annotations.Nullable;

/**
 * Eines der sechs festen Handlungsfelder (Stammdaten).
 */
public record Handlungsfeld(long id, int nummer, String name, @Nullable String fokus) {
}
