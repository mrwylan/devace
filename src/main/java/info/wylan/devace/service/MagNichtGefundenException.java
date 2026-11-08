package info.wylan.devace.service;

/**
 * UC-008 step 3: Die zu ändernde MAG gibt es nicht (mehr).
 */
public class MagNichtGefundenException extends NotFoundException {

	public MagNichtGefundenException() {
		super("Die MAG ist nicht vorhanden.");
	}

}
