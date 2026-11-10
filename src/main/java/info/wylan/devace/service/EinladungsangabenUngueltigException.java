package info.wylan.devace.service;

import java.util.List;

/**
 * UC-001 A1: Die Angaben fehlen oder sind ungültig. Jeder Fehler nennt das Feld und den Hinweis dazu.
 */
public class EinladungsangabenUngueltigException extends BusinessRuleException {

	public static final String ADRESSEN = "adressen";

	public static final String GUELTIGKEIT = "gueltigkeit";

	private final List<Fehler> fehler;

	public EinladungsangabenUngueltigException(List<Fehler> fehler) {
		super("Die Angaben sind fehlend oder ungültig.");
		this.fehler = List.copyOf(fehler);
	}

	public List<Fehler> fehler() {
		return this.fehler;
	}

	public record Fehler(String feld, String meldung) {
	}

}
