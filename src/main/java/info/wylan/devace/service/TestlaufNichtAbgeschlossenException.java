package info.wylan.devace.service;

/**
 * UC-009 A4: Das KI-Modell ist nicht erreichbar oder liefert zu mindestens einem Testfall keine Stufe; es ist kein
 * Testlauf gespeichert (OQ-088).
 */
public class TestlaufNichtAbgeschlossenException extends BusinessRuleException {

	public TestlaufNichtAbgeschlossenException() {
		super("Der Testlauf konnte nicht abgeschlossen werden. Es wurde nichts gespeichert.");
	}

}
