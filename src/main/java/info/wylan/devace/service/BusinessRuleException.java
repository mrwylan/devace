package info.wylan.devace.service;

/**
 * Eine Geschäftsregel ist verletzt: Die Anfrage ist wohlgeformt, aber nicht erlaubt.
 */
public abstract class BusinessRuleException extends RuntimeException {

	protected BusinessRuleException(String message) {
		super(message);
	}

}
