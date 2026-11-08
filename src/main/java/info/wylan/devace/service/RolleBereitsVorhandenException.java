package info.wylan.devace.service;

/**
 * UC-008 A2: Der Name gehört bereits zu einer Rolle.
 */
public class RolleBereitsVorhandenException extends BusinessRuleException {

	public RolleBereitsVorhandenException() {
		super("Diese Rolle besteht bereits.");
	}

}
