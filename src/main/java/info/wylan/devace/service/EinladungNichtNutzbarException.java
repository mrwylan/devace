package info.wylan.devace.service;

/**
 * UC-002 A1: Die Einladung ist unbekannt, bereits eingelöst oder abgelaufen, ohne dass ein laufendes Assessment
 * besteht.
 */
public class EinladungNichtNutzbarException extends BusinessRuleException {

	public static final String TITEL = "Einladung nicht nutzbar";

	public EinladungNichtNutzbarException() {
		super("Diese Einladung wurde bereits eingelöst oder ist abgelaufen.");
	}

}
