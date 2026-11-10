package info.wylan.devace.service;

/**
 * UC-006 step 3: Der gewählte Ausgangspunkt steht für die Rolle nicht zur Verfügung.
 */
public class AusgangspunktNichtVerfuegbarException extends BusinessRuleException {

	public AusgangspunktNichtVerfuegbarException() {
		super("Dieser Ausgangspunkt steht nicht zur Verfügung.");
	}

}
