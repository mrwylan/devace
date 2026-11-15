package info.wylan.devace.service;

import java.util.Map;

/**
 * UC-009 A2: Die Angaben zu einem Testfall sind unvollständig oder ungültig; je Feld der Hinweis aus OQ-086.
 */
public class TestfallUngueltigException extends BusinessRuleException {

	/** Feld der vorgegebenen Antwort. */
	public static final String ANTWORT = "antwort";

	/** Feld der erwarteten Stufe. */
	public static final String STUFE = "stufe";

	private final Map<String, String> fehler;

	public TestfallUngueltigException(Map<String, String> fehler) {
		super("Die Angaben zum Testfall sind unvollständig oder ungültig.");
		this.fehler = Map.copyOf(fehler);
	}

	/** Je Feld ({@link #ANTWORT}, {@link #STUFE}) der Hinweis. */
	public Map<String, String> fehler() {
		return this.fehler;
	}

}
