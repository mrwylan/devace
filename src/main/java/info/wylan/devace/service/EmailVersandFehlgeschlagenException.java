package info.wylan.devace.service;

/**
 * UC-001 A4: Das E-Mail-System lehnt den Versand ab oder ist nicht erreichbar. Die Ausnahme trägt weder die Adresse
 * noch den Text des Systems.
 */
public class EmailVersandFehlgeschlagenException extends ExternalSystemException {

	public EmailVersandFehlgeschlagenException(Throwable cause) {
		super("Der Versand der Einladung ist gescheitert.", cause);
	}

}
