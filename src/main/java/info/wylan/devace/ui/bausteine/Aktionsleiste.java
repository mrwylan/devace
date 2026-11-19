package info.wylan.devace.ui.bausteine;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * ADR-006: Knopfleiste mit festem Abstand zum Inhalt. Links stehen Nebenaktionen, rechts die Hauptaktionen. In langen
 * Formularen bleibt sie am unteren Rand sichtbar.
 */
public class Aktionsleiste extends HorizontalLayout {

	private final HorizontalLayout rechts = new HorizontalLayout();

	/**
	 * @param rechts die Hauptaktionen, rechtsbündig
	 */
	public Aktionsleiste(Component... rechts) {
		addClassName("devace-aktionsleiste");
		setWidthFull();
		setAlignItems(FlexComponent.Alignment.CENTER);
		this.rechts.add(rechts);
		this.rechts.addClassName("devace-aktionsleiste-rechts");
		add(this.rechts);
		setFlexGrow(1, this.rechts);
		this.rechts.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
	}

	/**
	 * Setzt Nebenaktionen an den linken Rand.
	 * @param links die Nebenaktionen
	 * @return diese Leiste
	 */
	public Aktionsleiste links(Component... links) {
		addComponentAsFirst(new HorizontalLayout(links));
		return this;
	}

	/**
	 * Hält die Leiste beim Bildlauf am unteren Rand des sichtbaren Bereichs.
	 * @return diese Leiste
	 */
	public Aktionsleiste haftend() {
		addClassName("devace-aktionsleiste-haftend");
		return this;
	}

}
