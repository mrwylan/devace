package info.wylan.devace.client.kimodell;

import java.util.List;
import java.util.Map;

/**
 * Anfrage an Jev über die System-One-Route von OpenRouter (ADR-003). Die Schlüssel der Fragen gehen nicht ans
 * Modell; alles Nötige steht in {@code instructions} und {@code criteria}.
 */
record JevAnfrage(String state, String model, Map<String, Frage> questions) {

	/** Fragetyp {@code score}: {@code criteria} sind die Stufen von niedrig nach hoch. */
	record Frage(String type, String instructions, List<String> criteria) {
	}

}
