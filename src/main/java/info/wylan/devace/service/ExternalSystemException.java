package info.wylan.devace.service;

/**
 * Ein externes System, das devace aufruft, antwortet nicht oder nicht verwertbar.
 */
public abstract class ExternalSystemException extends RuntimeException {

	protected ExternalSystemException(String message, Throwable cause) {
		super(message, cause);
	}

}
