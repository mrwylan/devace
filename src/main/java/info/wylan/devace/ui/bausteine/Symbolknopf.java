package info.wylan.devace.ui.bausteine;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * ADR-006: Schaltfläche nur mit Symbol, mit Tooltip und zugänglichem Namen, für Zeilenaktionen.
 */
public final class Symbolknopf {

	private Symbolknopf() {
	}

	/**
	 * @param symbol das Symbol
	 * @param tooltip der kurze Text des Tooltips
	 * @param name der zugängliche Name, unter dem Bildschirmleser und Tests die Schaltfläche finden
	 * @return die Schaltfläche
	 */
	public static Button erstellen(VaadinIcon symbol, String tooltip, String name) {
		Button knopf = new Button(symbol.create());
		knopf.addThemeVariants(ButtonVariant.TERTIARY);
		knopf.setTooltipText(tooltip);
		knopf.setAriaLabel(name);
		knopf.addClassName("devace-symbolknopf");
		return knopf;
	}

}
