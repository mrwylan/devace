package info.wylan.devace.ui.bausteine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasHelper;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.data.value.HasValueChangeMode;
import com.vaadin.flow.data.value.ValueChangeMode;
import org.jspecify.annotations.Nullable;

/**
 * ADR-006: Zeichenzähler für ein längenbegrenztes Textfeld. Er zeigt beim Tippen „<Zahl> / <Grenze> Zeichen“ im
 * Hilfetext. Die Grenze ist weich: Das Feld nimmt mehr Zeichen an, ist dann aber ungültig, und die Aktionen, die der
 * Zähler sperrt, bleiben gesperrt, bis die Grenze wieder eingehalten ist. Gezählt wird wie in den Services: der Text ohne
 * führende und folgende Leerzeichen.
 */
public final class Zeichenzaehler {

	/** Ab diesem Anteil der Grenze hebt der Zähler die Zahl hervor. */
	private static final double KNAPP = 0.9;

	private final Component feld;

	private final HasValue<?, String> wert;

	private final HasHelper hilfe;

	private final HasValidation pruefung;

	private final int grenze;

	private final @Nullable IntFunction<String> meldung;

	private final String basisHinweis;

	private final List<Runnable> beobachter = new ArrayList<>();

	private @Nullable String eigeneMeldung;

	private <F extends Component & HasValue<?, String> & HasHelper & HasValidation & HasValueChangeMode> Zeichenzaehler(
			F feld, int grenze, @Nullable IntFunction<String> meldung) {
		this.feld = feld;
		this.wert = feld;
		this.hilfe = feld;
		this.pruefung = feld;
		this.grenze = grenze;
		this.meldung = meldung;
		String bisher = feld.getHelperText();
		this.basisHinweis = (bisher != null) ? bisher : "";
		feld.setValueChangeMode(ValueChangeMode.EAGER);
		feld.addClassName("devace-zeichenfeld");
		feld.addValueChangeListener((event) -> aktualisieren());
		aktualisieren();
	}

	/**
	 * Bringt einen Zähler an, der das Feld selbst als ungültig markiert, sobald die Grenze überschritten ist.
	 * @param feld das Textfeld
	 * @param grenze höchste Zahl an Zeichen
	 * @param meldung der Hinweis der Spezifikation zur Zahl der Zeichen, zum Beispiel „Höchstens 500 Zeichen.“
	 * @return der Zähler
	 */
	public static <F extends Component & HasValue<?, String> & HasHelper & HasValidation & HasValueChangeMode> Zeichenzaehler mitMeldung(
			F feld, int grenze, IntFunction<String> meldung) {
		return new Zeichenzaehler(feld, grenze, meldung);
	}

	/**
	 * Bringt einen Zähler an einem Feld an, das ein {@code Binder} prüft: Der Zähler zeigt nur die Zahl und sperrt
	 * Aktionen, die Meldung setzt der Binder.
	 * @param feld das Textfeld
	 * @param grenze höchste Zahl an Zeichen
	 * @return der Zähler
	 */
	public static <F extends Component & HasValue<?, String> & HasHelper & HasValidation & HasValueChangeMode> Zeichenzaehler nurZaehlen(
			F feld, int grenze) {
		return new Zeichenzaehler(feld, grenze, null);
	}

	/**
	 * Sperrt die Aktion, solange einer der Zähler über seiner Grenze liegt.
	 * @param aktion die Schaltfläche, zum Beispiel „Speichern“ oder „Senden“
	 * @param zaehler die Zähler der Felder, die zur Aktion gehören
	 */
	public static void sperrt(Button aktion, Zeichenzaehler... zaehler) {
		List<Zeichenzaehler> alle = Arrays.asList(zaehler);
		Runnable pruefen = () -> aktion.setEnabled(alle.stream().allMatch(Zeichenzaehler::eingehalten));
		alle.forEach((z) -> z.beobachter.add(pruefen));
		pruefen.run();
	}

	/** @return ob die Grenze eingehalten ist */
	public boolean eingehalten() {
		return zeichen() <= this.grenze;
	}

	/** @return die Zahl der Zeichen ohne führende und folgende Leerzeichen */
	public int zeichen() {
		String text = this.wert.getValue();
		return (text != null) ? text.strip().length() : 0;
	}

	private void aktualisieren() {
		int zahl = zeichen();
		String zaehler = zahl + " / " + this.grenze + " Zeichen";
		this.hilfe.setHelperText(this.basisHinweis.isBlank() ? zaehler : this.basisHinweis + " · " + zaehler);
		this.feld.getElement().getClassList().set("devace-zaehler-knapp", zahl >= KNAPP * this.grenze && zahl <= this.grenze);
		this.feld.getElement().getClassList().set("devace-zaehler-ueber", zahl > this.grenze);
		if (this.meldung != null) {
			if (zahl > this.grenze) {
				this.eigeneMeldung = this.meldung.apply(zahl);
				this.pruefung.setErrorMessage(this.eigeneMeldung);
				this.pruefung.setInvalid(true);
			}
			else if (this.eigeneMeldung != null && this.eigeneMeldung.equals(this.pruefung.getErrorMessage())) {
				this.pruefung.setInvalid(false);
				this.eigeneMeldung = null;
			}
		}
		this.beobachter.forEach(Runnable::run);
	}

}
