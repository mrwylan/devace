package info.wylan.devace.service;

/**
 * Das Objekt, auf das sich der Akteur bezieht, gibt es nicht (mehr).
 */
public abstract class NotFoundException extends RuntimeException {

	protected NotFoundException(String message) {
		super(message);
	}

}
