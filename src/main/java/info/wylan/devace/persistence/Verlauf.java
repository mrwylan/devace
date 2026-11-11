package info.wylan.devace.persistence;

/**
 * Eine beantwortete Frage im Dialog (UC-002 step 7), ohne die zugeordnete Stufe (BR-008).
 */
public record Verlauf(int position, String frage, String antwort) {
}
