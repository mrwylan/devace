package info.wylan.devace.client.kimodell;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Antwortformat des KI-Modells für den Fragenkatalog-Entwurf.
 */
record KiEntwurfAntwort(@JsonPropertyDescription("Die entworfenen Fragen") List<Frage> fragen) {

	record Frage(@JsonPropertyDescription("Nummer des Handlungsfelds, 1 bis 6") int handlungsfeld,
			@JsonPropertyDescription("Wortlaut der Frage") String frage,
			@JsonPropertyDescription("Genau sechs Beschreibungen, Index 0 bis 5 für die Stufen 0 bis 5") List<String> stufen) {
	}

}
