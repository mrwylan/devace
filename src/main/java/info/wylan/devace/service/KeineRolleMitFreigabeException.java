package info.wylan.devace.service;

/**
 * UC-001 A2: Es gibt keine Rolle mit freigegebenem Fragenkatalog, noch keine Einladung ist möglich.
 */
public class KeineRolleMitFreigabeException extends BusinessRuleException {

	public static final String TITEL = "Noch keine Einladung möglich";

	public KeineRolleMitFreigabeException() {
		super("Es gibt noch keine Rolle mit freigegebenem Fragenkatalog. Bitte wenden Sie sich an einen Administrator.");
	}

}
