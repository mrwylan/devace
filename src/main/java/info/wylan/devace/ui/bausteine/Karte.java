package info.wylan.devace.ui.bausteine;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * ADR-006: Karte mit Titel und Untertitel links und der Hauptaktion rechts oben. Inhalt und Fußnote folgen darunter.
 */
public class Karte extends VerticalLayout {

	private final HorizontalLayout kopf;

	private final HorizontalLayout aktionen = new HorizontalLayout();

	/**
	 * @param titel der Titel der Karte
	 * @param untertitel der Untertitel; leer für keinen
	 */
	public Karte(String titel, String untertitel) {
		addClassName("devace-karte");
		setPadding(false);
		Div text = new Div(new H3(titel));
		if (!untertitel.isEmpty()) {
			Span unter = new Span(untertitel);
			unter.addClassName("devace-untertitel");
			text.add(unter);
		}
		text.addClassName("devace-karte-titel");
		this.aktionen.addClassName("devace-karte-aktionen");
		this.kopf = new HorizontalLayout(text, this.aktionen);
		this.kopf.addClassName("devace-karte-kopf");
		this.kopf.setWidthFull();
		this.kopf.setAlignItems(FlexComponent.Alignment.START);
		this.kopf.setFlexGrow(1, text);
		add(this.kopf);
	}

	/**
	 * Setzt die Hauptaktionen rechts oben im Kopf der Karte.
	 * @param komponenten die Aktionen
	 * @return diese Karte
	 */
	public Karte aktion(Component... komponenten) {
		this.aktionen.add(komponenten);
		return this;
	}

	/**
	 * Fügt den Inhalt an und lässt das letzte Element die freie Höhe füllen, etwa eine Liste.
	 * @param inhalt der Inhalt
	 * @return diese Karte
	 */
	public Karte fuellen(Component inhalt) {
		add(inhalt);
		setFlexGrow(1, inhalt);
		addClassName("devace-karte-fuellend");
		return this;
	}

	/**
	 * Fügt eine Fußnote in kleiner Schrift an.
	 * @param text der Text
	 * @return diese Karte
	 */
	public Karte fussnote(String text) {
		Span note = new Span(text);
		note.addClassName("devace-fussnote");
		add(note);
		return this;
	}

}
