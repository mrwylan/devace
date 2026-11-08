package info.wylan.devace.service;

/**
 * UC-008 step 3: Die zu ändernde Rolle gibt es nicht (mehr).
 */
public class RolleNichtGefundenException extends NotFoundException {

	public RolleNichtGefundenException() {
		super("Die Rolle ist nicht vorhanden.");
	}

}
