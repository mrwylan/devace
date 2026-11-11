package info.wylan.devace.persistence;

/**
 * Eine Antwort mit ihrer Stufe und dem Handlungsfeld ihrer Frage (UC-003 step 2).
 */
public record BewerteteAntwort(long handlungsfeldId, String frage, String antwort, int stufe) {
}
