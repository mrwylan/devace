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
@Route(value = "ki-hinweise", layout = MainLayout.class)
@PageTitle("Hinweise zur KI-Nutzung")
@PermitAll
public class KiHinweiseView extends VerticalLayout {

	public KiHinweiseView() {
		Karte karte = new Karte("Hinweise zur KI-Nutzung", "Wie devace KI-Modelle einsetzt");
		karte.add(new Paragraph("Inhalt folgt."));
		add(karte);
	}

}
