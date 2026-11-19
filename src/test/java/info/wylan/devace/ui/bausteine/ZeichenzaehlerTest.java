package info.wylan.devace.ui.bausteine;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ADR-006: Der Zeichenzähler zählt wie die Services (ohne führende und folgende Leerzeichen), zeigt die Zahl im
 * Hilfetext, markiert das Feld über der Grenze mit dem Hinweis der Spezifikation und sperrt die zugehörige Aktion.
 */
class ZeichenzaehlerTest {

	@Test
	void zaehlt_ohne_fuehrende_und_folgende_leerzeichen_und_behaelt_den_hilfetext() {
		TextField feld = new TextField();
		feld.setHelperText("Pflichtangabe");
		Zeichenzaehler zaehler = Zeichenzaehler.nurZaehlen(feld, 10);

		feld.setValue("  abc  ");

		assertThat(zaehler.zeichen()).isEqualTo(3);
		assertThat(feld.getHelperText()).isEqualTo("Pflichtangabe · 3 / 10 Zeichen");
	}

	@Test
	void weiche_grenze_meldet_und_sperrt_erst_ueber_der_grenze() {
		TextArea feld = new TextArea();
		Button senden = new Button("Senden");
		Zeichenzaehler zaehler = Zeichenzaehler.mitMeldung(feld, 5, (zahl) -> "Zu lang: " + zahl + " von höchstens 5.");
		Zeichenzaehler.sperrt(senden, zaehler);

		feld.setValue("12345");
		assertThat(zaehler.eingehalten()).isTrue();
		assertThat(feld.isInvalid()).isFalse();
		assertThat(senden.isEnabled()).isTrue();

		// das Feld nimmt mehr Zeichen an, als die Grenze erlaubt
		feld.setValue("123456");
		assertThat(feld.getValue()).hasSize(6);
		assertThat(zaehler.eingehalten()).isFalse();
		assertThat(feld.isInvalid()).isTrue();
		assertThat(feld.getErrorMessage()).isEqualTo("Zu lang: 6 von höchstens 5.");
		assertThat(feld.getHelperText()).isEqualTo("6 / 5 Zeichen");
		assertThat(senden.isEnabled()).isFalse();

		// zurück unter der Grenze verschwindet der eigene Hinweis
		feld.setValue("1234");
		assertThat(feld.isInvalid()).isFalse();
		assertThat(senden.isEnabled()).isTrue();
	}

	@Test
	void fremder_hinweis_bleibt_stehen() {
		TextArea feld = new TextArea();
		// die eingebaute Prüfung des Feldes aus, damit nur der Zähler auf den Zustand wirkt (wie unter einem Binder)
		feld.setManualValidation(true);
		Zeichenzaehler.mitMeldung(feld, 5, (zahl) -> "Zu lang.");
		feld.setInvalid(true);
		feld.setErrorMessage("Pflichtangabe fehlt.");

		feld.setValue("abc");

		assertThat(feld.isInvalid()).isTrue();
		assertThat(feld.getErrorMessage()).isEqualTo("Pflichtangabe fehlt.");
	}

	@Test
	void eine_aktion_bleibt_gesperrt_solange_einer_von_mehreren_zaehlern_ueber_der_grenze_liegt() {
		TextField erstes = new TextField();
		TextField zweites = new TextField();
		Button speichern = new Button("Speichern");
		Zeichenzaehler.sperrt(speichern, Zeichenzaehler.nurZaehlen(erstes, 3), Zeichenzaehler.nurZaehlen(zweites, 3));

		erstes.setValue("abcd");
		zweites.setValue("ab");
		assertThat(speichern.isEnabled()).isFalse();

		erstes.setValue("abc");
		assertThat(speichern.isEnabled()).isTrue();
	}

}
