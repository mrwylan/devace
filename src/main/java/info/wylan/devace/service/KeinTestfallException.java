package info.wylan.devace.service;

/**
 * UC-009 A3: Der gewählte Fragenkatalog hat keinen Testfall, es gibt nichts auszuführen (OQ-087).
 */
public class KeinTestfallException extends BusinessRuleException {

	public KeinTestfallException() {
		super("Der Fragenkatalog hat keinen Testfall.");
	}

}
