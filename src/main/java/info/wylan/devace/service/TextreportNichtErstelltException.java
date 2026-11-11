package info.wylan.devace.service;

/**
 * UC-003 A1: Das KI-Modell ist nicht erreichbar oder liefert keinen Textreport; es wurde nichts gespeichert.
 */
public class TextreportNichtErstelltException extends BusinessRuleException {

	public static final String TITEL = "Textreport nicht erstellt";

	public TextreportNichtErstelltException() {
		super("Das KI-Modell hat keinen Textreport geliefert. Es wurde nichts gespeichert, Ihre Antworten bleiben erhalten. "
				+ "Sie können den Abschluss erneut anfordern.");
	}

}
