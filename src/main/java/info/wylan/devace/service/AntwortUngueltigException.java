package info.wylan.devace.service;

/**
 * UC-002 A4: Die Antwort ist leer oder überschreitet die zulässige Länge.
 */
public class AntwortUngueltigException extends BusinessRuleException {

	public AntwortUngueltigException(String message) {
		super(message);
	}

}
