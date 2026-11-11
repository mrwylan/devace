package info.wylan.devace.service;

import java.util.List;

/**
 * UC-007 A2: Der Entwurf ist nicht vollständig und wird nicht freigegeben. {@link #handlungsfelderOhneFrage()}
 * nennt die Handlungsfelder ohne Frage, {@link #fragenMitUnvollstaendigemModell()} die Nummern der Fragen ohne
 * Vorgabe für jede Stufe.
 */
public class EntwurfUnvollstaendigException extends BusinessRuleException {

	private final List<String> handlungsfelderOhneFrage;

	private final List<Integer> fragenMitUnvollstaendigemModell;

	public EntwurfUnvollstaendigException(List<String> handlungsfelderOhneFrage,
			List<Integer> fragenMitUnvollstaendigemModell) {
		super("Der Entwurf ist unvollständig und wird nicht freigegeben.");
		this.handlungsfelderOhneFrage = List.copyOf(handlungsfelderOhneFrage);
		this.fragenMitUnvollstaendigemModell = List.copyOf(fragenMitUnvollstaendigemModell);
	}

	public List<String> handlungsfelderOhneFrage() {
		return this.handlungsfelderOhneFrage;
	}

	public List<Integer> fragenMitUnvollstaendigemModell() {
		return this.fragenMitUnvollstaendigemModell;
	}

}
