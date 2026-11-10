package info.wylan.devace.service;

/**
 * UC-001 BR-002: Die MAG ist der Teamleitung nicht zugeordnet oder gibt es nicht. Die Oberfläche bietet nur eigene MAGs
 * an; der Text ist technisch (OQ).
 */
public class MagNichtZugeordnetException extends BusinessRuleException {

	public MagNichtZugeordnetException() {
		super("Diese MAG steht nicht zur Verfügung.");
	}

}
