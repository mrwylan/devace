package info.wylan.devace.service;

/**
 * Das E-Mail-System, wie UC-001 es aufruft.
 */
public interface EmailSystemClient {

	/**
	 * UC-001 step 9: versendet die Einladung an die Adresse; A4 wenn das E-Mail-System den Versand ablehnt oder nicht
	 * erreichbar ist.
	 *
	 * @throws EmailVersandFehlgeschlagenException wenn der Versand nicht gelingt (A4)
	 */
	void einladungVersenden(Einladungsmail mail);

	/**
	 * UC-001 BR-007: die E-Mail enthält nur Betreff, einen kurzen Text und den Link. Die Adresse dient nur dem Versand.
	 */
	record Einladungsmail(String adresse, String betreff, String text) {
	}

}
