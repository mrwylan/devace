package info.wylan.devace.client.kimodell;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Antwortformat des KI-Modells für die Zuordnung einer Antwort zu einer Stufe.
 */
record KiBewertungAntwort(
		@JsonPropertyDescription("Die zugeordnete Stufe, eine ganze Zahl von 0 bis 5") Integer stufe) {
}
