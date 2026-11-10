package info.wylan.devace.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import info.wylan.devace.service.EmailSystemClient.Einladungsmail;

/**
 * Fake des E-Mail-Systems für die Tests der Schichten darüber: kein Versand an einen echten Server.
 *
 * <p>
 * {@link #lehneAb} lässt den Versand an eine Adresse scheitern (A4), {@link #gesendet()} zeigt, was versendet wurde.
 */
public class EmailSystemClientFake implements EmailSystemClient {

	private final List<Einladungsmail> gesendet = new ArrayList<>();

	private final Set<String> abgelehnt = new HashSet<>();

	@Override
	public void einladungVersenden(Einladungsmail mail) {
		if (this.abgelehnt.contains(mail.adresse())) {
			throw new EmailVersandFehlgeschlagenException(new IllegalStateException("stubbed"));
		}
		this.gesendet.add(mail);
	}

	/** A4: das E-Mail-System lehnt den Versand an diese Adresse ab. */
	public void lehneAb(String adresse) {
		this.abgelehnt.add(adresse);
	}

	public List<Einladungsmail> gesendet() {
		return List.copyOf(this.gesendet);
	}

	public void reset() {
		this.gesendet.clear();
		this.abgelehnt.clear();
	}

}
