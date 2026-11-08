package info.wylan.devace.service;

/**
 * UC-006 A2: Das KI-Modell ist nicht erreichbar oder nicht eingerichtet.
 */
public class KiModellNichtErreichbarException extends ExternalSystemException {

	public KiModellNichtErreichbarException(Throwable cause) {
		super("Das KI-Modell ist nicht erreichbar.", cause);
	}

}
