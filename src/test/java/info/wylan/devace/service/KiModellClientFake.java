package info.wylan.devace.service;

import java.util.ArrayList;
import java.util.List;

import info.wylan.devace.service.KiModellClient.EntwurfAnfrage;
import info.wylan.devace.service.KiModellClient.HandlungsfeldAnfrage;
import info.wylan.devace.service.KiModellClient.VorgeschlageneFrage;

/**
 * Fake des KI-Modells für die Tests der Schichten darüber: kein Aufruf eines echten Modells.
 *
 * <p>
 * Ohne Vorgabe liefert es je angefragtem Handlungsfeld die verlangte Zahl Fragen mit sechs Vorgaben.
 * {@link #failNext()} löst A2 aus, {@link #antwortet} legt die nächste Antwort fest, {@link #anfragen()} zeigt, was
 * gesendet wurde.
 */
public class KiModellClientFake implements KiModellClient {

	private final List<EntwurfAnfrage> anfragen = new ArrayList<>();

	private RuntimeException naechsterFehler;

	private List<VorgeschlageneFrage> naechsteAntwort;

	@Override
	public List<VorgeschlageneFrage> fragenEntwerfen(EntwurfAnfrage anfrage) {
		this.anfragen.add(anfrage);
		if (this.naechsterFehler != null) {
			RuntimeException fehler = this.naechsterFehler;
			this.naechsterFehler = null;
			throw fehler;
		}
		if (this.naechsteAntwort != null) {
			List<VorgeschlageneFrage> antwort = this.naechsteAntwort;
			this.naechsteAntwort = null;
			return antwort;
		}
		List<VorgeschlageneFrage> fragen = new ArrayList<>();
		for (HandlungsfeldAnfrage feld : anfrage.handlungsfelder()) {
			for (int i = 1; i <= feld.anzahl(); i++) {
				fragen.add(new VorgeschlageneFrage(feld.nummer(), "KI-Frage " + feld.nummer() + "." + i + "?",
						List.of("Vorgabe 0", "Vorgabe 1", "Vorgabe 2", "Vorgabe 3", "Vorgabe 4", "Vorgabe 5")));
			}
		}
		return fragen;
	}

	/** A2: das KI-Modell ist nicht erreichbar. */
	public void failNext() {
		this.naechsterFehler = new KiModellNichtErreichbarException(new IllegalStateException("stubbed"));
	}

	/** Legt die nächste Antwort fest, zum Beispiel leer (kein Entwurf) oder mit unvollständigen Vorgaben (BR-011). */
	public void antwortet(List<VorgeschlageneFrage> antwort) {
		this.naechsteAntwort = antwort;
	}

	public List<EntwurfAnfrage> anfragen() {
		return List.copyOf(this.anfragen);
	}

	public void reset() {
		this.anfragen.clear();
		this.naechsterFehler = null;
		this.naechsteAntwort = null;
	}

}
