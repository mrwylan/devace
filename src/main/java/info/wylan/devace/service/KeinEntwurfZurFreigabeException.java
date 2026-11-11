package info.wylan.devace.service;

/**
 * UC-007 A1: Es gibt keine Rolle mit einem Entwurf, oder der gewählte Entwurf liegt nicht mehr vor.
 */
public class KeinEntwurfZurFreigabeException extends BusinessRuleException {

	public KeinEntwurfZurFreigabeException() {
		super("Es liegt kein Entwurf zur Freigabe vor.");
	}

}
