package info.wylan.devace.persistence;

/**
 * Eine Version des Starthinweises (UC-002 step 2).
 */
public record Starthinweis(long id, int version, String text, String kiModelle) {
}
