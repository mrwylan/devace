package info.wylan.devace.client.kimodell;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Zugang zum KI-Modell über OpenRouter. Der Schlüssel kommt aus der Umgebungsvariablen OPENROUTER_API_KEY, die
 * Modell-ID je Aufgabe aus der Konfiguration (OQ-034).
 */
@ConfigurationProperties("devace.ki")
record KiProperties(String baseUrl, String apiKey, Modelle modelle) {

	/** Modell-ID je Aufgabe; leer, solange keines eingerichtet ist. */
	record Modelle(String fragenkatalog) {
	}

}
