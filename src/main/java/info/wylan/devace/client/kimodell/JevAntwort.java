package info.wylan.devace.client.kimodell;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Antwort von Jev (ADR-003): je Frage die Wahrscheinlichkeit jeder Stufe, über den Namen der Stufe ("0" bis "5").
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record JevAntwort(String model, Map<String, Ergebnis> answers) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Ergebnis(String type, Double score, Map<String, Double> probabilities, Double confidence) {
	}

}
