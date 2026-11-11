package info.wylan.devace.service;

/**
 * UC-002 A3 und BR-002: Ohne bestätigten Starthinweis beginnt das Assessment nicht.
 */
public class StarthinweisNichtBestaetigtException extends BusinessRuleException {

	public StarthinweisNichtBestaetigtException() {
		super("Das Assessment beginnt erst, nachdem der Starthinweis bestätigt ist.");
	}

}
