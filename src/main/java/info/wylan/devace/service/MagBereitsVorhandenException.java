package info.wylan.devace.service;

/**
 * UC-008 A2: Die Kombination aus Scrum Team und Linienteam gehört bereits zu einer MAG.
 */
public class MagBereitsVorhandenException extends BusinessRuleException {

	public MagBereitsVorhandenException() {
		super("Diese MAG besteht bereits.");
	}

}
