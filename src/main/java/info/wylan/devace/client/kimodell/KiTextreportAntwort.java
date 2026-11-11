package info.wylan.devace.client.kimodell;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Antwortformat des KI-Modells für den Textreport (UC-003 step 3).
 */
record KiTextreportAntwort(
		@JsonPropertyDescription("Zwei bis drei Sätze Überblick über die Stärken und die größten Chancen") String ueberblick,
		@JsonPropertyDescription("Ein Abschnitt je Handlungsfeld, in der Reihenfolge der Anfrage") List<Abschnitt> abschnitte) {

	record Abschnitt(@JsonPropertyDescription("Name des Handlungsfelds, genau wie in der Anfrage") String handlungsfeld,
			@JsonPropertyDescription("Würdigung der Leistung und Begründung der Einstufung, ohne wörtliche Antworttexte") String wuerdigung,
			@JsonPropertyDescription("Ein konkreter nächster Schritt zur Weiterentwicklung") String naechsterSchritt) {
	}

}
