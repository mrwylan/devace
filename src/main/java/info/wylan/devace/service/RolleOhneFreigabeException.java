package info.wylan.devace.service;

/**
 * UC-001 BR-003: Für die Rolle besteht kein freigegebener Fragenkatalog oder es gibt sie nicht. Die Oberfläche bietet
 * nur Rollen mit Freigabe an; der Text ist technisch.
 */
public class RolleOhneFreigabeException extends BusinessRuleException {

	public RolleOhneFreigabeException() {
		super("Diese Rolle steht nicht zur Verfügung.");
	}

}
