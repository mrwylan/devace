package info.wylan.devace.ui;

import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import info.wylan.devace.ui.bausteine.Karte;
import jakarta.annotation.security.PermitAll;

/**
 * Platzhalter der Fußzeile (ADR-006). Der Inhalt ist noch nicht spezifiziert.
 */
@Route(value = "datenschutz", layout = MainLayout.class)
@PageTitle("Datenschutz")
@PermitAll
public class DatenschutzView extends VerticalLayout {

	public DatenschutzView() {
		Karte karte = new Karte("Datenschutz", "Hinweise zum Datenschutz in devace");
		karte.add(new Paragraph("Inhalt folgt."));
		add(karte);
	}

}
