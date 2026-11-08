package info.wylan.devace.service;

import java.util.List;

/**
 * UC-006 A4: Der Entwurf ist unvollständig oder ungültig; {@link #fehler()} nennt die betroffenen Angaben.
 */
public class EntwurfUngueltigException extends BusinessRuleException {

	private final List<Fehler> fehler;

	public EntwurfUngueltigException(List<Fehler> fehler) {
		super("Der Entwurf ist unvollständig oder ungültig.");
		this.fehler = List.copyOf(fehler);
	}

	public List<Fehler> fehler() {
		return this.fehler;
	}

	/**
	 * Eine betroffene Angabe: die Frage (Index im Entwurf) und die Stufe ihrer Vorgabe, oder {@code -1} für den
	 * Wortlaut.
	 */
	public record Fehler(int frageIndex, int stufe, String meldung) {

		public static final int WORTLAUT = -1;

	}

}
