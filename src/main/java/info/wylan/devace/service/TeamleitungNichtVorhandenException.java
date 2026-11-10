package info.wylan.devace.service;

/**
 * UC-008 BR-004: Die gewählte Teamleitung ist keine vorhandene Teamleitung. Die Oberfläche bietet nur
 * vorhandene Teamleitungen an, die Ausnahme schützt vor anderen Aufrufern.
 */
public class TeamleitungNichtVorhandenException extends BusinessRuleException {

	public TeamleitungNichtVorhandenException() {
		super("Die Teamleitung ist nicht vorhanden.");
	}

}
