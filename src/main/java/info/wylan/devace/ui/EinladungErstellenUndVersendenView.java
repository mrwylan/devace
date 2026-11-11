package info.wylan.devace.ui;

import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import info.wylan.devace.persistence.MagListItem;
import info.wylan.devace.persistence.RolleOption;
import info.wylan.devace.service.EinladungService;
import info.wylan.devace.service.EinladungService.Auswahl;
import info.wylan.devace.service.EinladungService.EinladungsAngaben;
import info.wylan.devace.service.EinladungService.Ergebnis;
import info.wylan.devace.service.EinladungService.Zusammenfassung;
import info.wylan.devace.service.EinladungsangabenUngueltigException;
import info.wylan.devace.service.KeineRolleMitFreigabeException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import jakarta.annotation.security.RolesAllowed;

/**
 * UC-001 Einladung erstellen und versenden.
 */
@Route(value = "einladungen", layout = MainLayout.class)
@PageTitle("Einladungen erstellen und versenden")
// UC-001 BR-009: Nur die angemeldete Teamleitung erstellt Einladungen.
@RolesAllowed("TEAMLEITUNG")
public class EinladungErstellenUndVersendenView extends VerticalLayout implements BeforeEnterObserver {

	private static final int VORSCHLAG_GUELTIGKEIT = 30;

	private final EinladungService service;

	private final Div inhalt = new Div();

	private final Select<MagListItem> mag = new Select<>();

	private final Select<RolleOption> rolle = new Select<>();

	private final TextArea adressen = new TextArea("E-Mail-Adressen der Teilnehmer");

	private final IntegerField gueltigkeit = new IntegerField("Gültigkeit in Tagen");

	public EinladungErstellenUndVersendenView(EinladungService service) {
		this.service = service;
		setWidthFull();
		add(this.inhalt);
		this.inhalt.setWidthFull();
	}

	/** UC-001 steps 1–2: Bei jedem Öffnen zeigt die Ansicht die aktuell vorhandenen MAGs und Rollen. */
	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		zeigeFormular();
	}

	/** UC-001 steps 1–4: die Auswahl der MAGs und Rollen und die Angaben; A2 wenn keine Rolle bereitsteht. */
	private void zeigeFormular() {
		Auswahl auswahl;
		try {
			auswahl = this.service.auswahl();
		}
		catch (KeineRolleMitFreigabeException ex) {
			// UC-001 A2: Es gibt keine Rolle mit freigegebenem Fragenkatalog; der Use Case endet.
			setzeInhalt(seitentitel(), meldung(KeineRolleMitFreigabeException.TITEL, ex.getMessage(), "warnung"));
			return;
		}
		this.mag.setLabel("MAG (Scrum Team · Linienteam)");
		this.mag.setId("einladung-mag");
		this.mag.setItems(auswahl.mags());
		this.mag.setItemLabelGenerator((eintrag) -> eintrag.scrumTeam() + " · " + eintrag.linienteam());
		this.mag.setEmptySelectionAllowed(false);
		if (!auswahl.mags().isEmpty()) {
			this.mag.setValue(auswahl.mags().get(0));
		}
		this.rolle.setLabel("Rolle");
		this.rolle.setId("einladung-rolle");
		this.rolle.setItems(auswahl.rollen());
		this.rolle.setItemLabelGenerator(RolleOption::name);
		this.rolle.setEmptySelectionAllowed(false);
		this.rolle.setValue(auswahl.rollen().get(0));
		this.adressen.setId("einladung-adressen");
		this.adressen.setWidthFull();
		this.adressen.setMinHeight("8rem");
		this.adressen.clear();
		// UC-001 BR-004: Die Adressen dienen nur dem Versand und werden nicht gespeichert.
		this.adressen.setHelperText("Eine Adresse pro Zeile. Die Adressen werden nur für den Versand verwendet und nicht gespeichert.");
		this.gueltigkeit.setId("einladung-gueltigkeit");
		this.gueltigkeit.setStepButtonsVisible(true);
		this.gueltigkeit.setMin(1);
		this.gueltigkeit.setMax(30);
		// UC-001 BR-008: Die Gültigkeitsdauer ist eine ganze Zahl von 1 bis 30 Tagen, vorgeschlagen werden 30.
		this.gueltigkeit.setValue(VORSCHLAG_GUELTIGKEIT);
		this.gueltigkeit.setHelperText("1 bis 30 Tage ab Erstellung");
		fehlerZuruecksetzen();

		HorizontalLayout auswahlZeile = new HorizontalLayout(this.mag, this.rolle);
		auswahlZeile.setWidthFull();
		this.mag.setWidth("50%");
		this.rolle.setWidth("50%");
		Button abbrechen = new Button("Abbrechen", (event) -> zeigeFormular());
		abbrechen.addThemeVariants(ButtonVariant.TERTIARY);
		abbrechen.setId("einladung-abbrechen");
		Button erstellen = new Button("Einladungen erstellen und versenden", (event) -> anfordern());
		erstellen.addThemeVariants(ButtonVariant.PRIMARY);
		erstellen.setId("einladung-erstellen");
		// UC-001 Pre-2: Ohne zugeordnete MAG gibt es nichts zu erstellen; die Aktion bleibt deaktiviert.
		erstellen.setEnabled(this.mag.getValue() != null);
		HorizontalLayout aktionen = new HorizontalLayout(abbrechen, erstellen);
		aktionen.setWidthFull();
		aktionen.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

		VerticalLayout formular = karte("Einladungen erstellen",
				"Laden Sie Teilnehmer Ihrer MAG zu einem Assessment ein. Die Einladung enthält keinen Hinweis auf die Person.");
		formular.add(auswahlZeile, this.adressen, this.gueltigkeit, aktionen);
		formular.getStyle().set("flex", "3");
		Div hinweis = meldung("Hinweis zur Einladung",
				"Die E-Mail enthält nur die Einladung. Sie gilt für genau ein Assessment und lässt keine Person erkennen.",
				null);
		hinweis.getStyle().set("flex", "1").set("align-self", "flex-start");
		HorizontalLayout spalten = new HorizontalLayout(formular, hinweis);
		spalten.setWidthFull();
		spalten.setAlignItems(FlexComponent.Alignment.START);
		setzeInhalt(seitentitel(), spalten);
	}

	/** UC-001 steps 5–6: prüft die Angaben und zeigt die Zusammenfassung; A1 bei fehlenden oder ungültigen Angaben. */
	private void anfordern() {
		fehlerZuruecksetzen();
		EinladungsAngaben angaben = angaben();
		try {
			Zusammenfassung zusammenfassung = this.service.pruefen(angaben);
			zusammenfassungDialog(angaben, zusammenfassung);
		}
		catch (EinladungsangabenUngueltigException ex) {
			// UC-001 A1: Hinweis an den fehlenden oder ungültigen Angaben, weiter bei Schritt 4.
			for (EinladungsangabenUngueltigException.Fehler fehler : ex.fehler()) {
				if (EinladungsangabenUngueltigException.ADRESSEN.equals(fehler.feld())) {
					this.adressen.setInvalid(true);
					this.adressen.setErrorMessage(fehler.meldung());
				}
				else {
					this.gueltigkeit.setInvalid(true);
					this.gueltigkeit.setErrorMessage(fehler.meldung());
				}
			}
		}
	}

	/** UC-001 step 7: die Teamleitung bestätigt die Zusammenfassung; A3 bricht ab. */
	private void zusammenfassungDialog(EinladungsAngaben angaben, Zusammenfassung zusammenfassung) {
		Dialog dialog = new Dialog();
		dialog.setId("einladung-zusammenfassung");
		dialog.setHeaderTitle("Einladungen versenden?");
		dialog.setWidth("34rem");
		String text = "Es werden " + zusammenfassung.anzahl() + (zusammenfassung.anzahl() == 1 ? " Einladung" : " Einladungen")
				+ " für " + zusammenfassung.mag().scrumTeam() + " · " + zusammenfassung.mag().linienteam() + ", Rolle "
				+ zusammenfassung.rolle().name() + " erstellt und per E-Mail versendet. Gültigkeit: "
				+ zusammenfassung.gueltigkeitTage() + (zusammenfassung.gueltigkeitTage() == 1 ? " Tag." : " Tage.");
		dialog.add(new Paragraph(text));
		// UC-001 A3: Abbruch bei Schritt 7 verwirft die Angaben wie bei Schritt 5.
		Button abbrechen = new Button("Abbrechen", (event) -> {
			dialog.close();
			zeigeFormular();
		});
		abbrechen.setId("zusammenfassung-abbrechen");
		Button versenden = new Button("Versenden", (event) -> {
			dialog.close();
			versenden(angaben);
		});
		versenden.addThemeVariants(ButtonVariant.PRIMARY);
		versenden.setId("einladung-versenden");
		dialog.getFooter().add(abbrechen, versenden);
		dialog.open();
	}

	/** UC-001 steps 8–10: erstellt und versendet die Einladungen und zeigt das Ergebnis. */
	private void versenden(EinladungsAngaben angaben) {
		try {
			zeigeErgebnis(this.service.erstellenUndVersenden(angaben));
		}
		catch (EinladungsangabenUngueltigException ex) {
			// die Angaben haben sich zwischen Zusammenfassung und Bestätigung nicht geändert; ungültig bleibt A1
			zeigeFormular();
		}
	}

	private void zeigeErgebnis(Ergebnis ergebnis) {
		List<Object> teile = new java.util.ArrayList<>();
		teile.add(seitentitel());
		int versendet = ergebnis.versendet();
		if (versendet > 0) {
			Div erfolg = meldung((versendet == 1) ? "1 Einladung versendet" : versendet + " Einladungen versendet",
					(versendet == 1) ? "Für 1 Adresse wurde eine Einladung mit eindeutiger Referenz erstellt und per E-Mail versendet."
							: "Für " + versendet + " Adressen wurde eine Einladung mit eindeutiger Referenz erstellt und per E-Mail versendet.",
					"erfolg");
			erfolg.setId("einladung-ergebnis");
			teile.add(erfolg);
		}
		int gescheitert = ergebnis.nichtVersendet().size();
		if (gescheitert > 0) {
			Div fehler = meldung((gescheitert == 1) ? "1 Einladung nicht versendet" : gescheitert + " Einladungen nicht versendet",
					"Der Versand ist gescheitert. Für " + ((gescheitert == 1) ? "diese Adresse" : "diese Adressen")
							+ " wurde keine Einladung gespeichert.",
					"fehler");
			fehler.setId("einladung-fehler");
			teile.add(fehler);
			Grid<String> grid = new Grid<>();
			grid.addColumn((adresse) -> adresse).setHeader("Adresse");
			grid.addThemeVariants(GridVariant.NO_BORDER);
			grid.setId("nicht-versendet-grid");
			grid.setAllRowsVisible(true);
			grid.setItems(ergebnis.nichtVersendet());
			VerticalLayout liste = karte("Nicht versendet", "Diese Liste wird nicht gespeichert und verschwindet, sobald Sie die Seite verlassen.");
			liste.add(grid);
			teile.add(liste);
		}
		Button weitere = new Button("Weitere Einladungen erstellen", (event) -> zeigeFormular());
		weitere.addThemeVariants(ButtonVariant.PRIMARY);
		weitere.setId("weitere-einladungen");
		teile.add(weitere);
		setzeInhalt(teile.toArray(new com.vaadin.flow.component.Component[0]));
	}

	private EinladungsAngaben angaben() {
		String eingabe = this.adressen.getValue();
		List<String> zeilen = (eingabe == null) ? List.of() : List.of(eingabe.split("\\R"));
		Integer tage = this.gueltigkeit.getValue();
		return new EinladungsAngaben(this.mag.getValue().id(), this.rolle.getValue().id(), zeilen,
				(tage != null) ? tage : 0);
	}

	private void fehlerZuruecksetzen() {
		this.adressen.setInvalid(false);
		this.gueltigkeit.setInvalid(false);
	}

	private void setzeInhalt(com.vaadin.flow.component.Component... teile) {
		this.inhalt.removeAll();
		VerticalLayout spalte = new VerticalLayout(teile);
		spalte.setPadding(false);
		this.inhalt.add(spalte);
	}

	private static H2 seitentitel() {
		H2 titel = new H2("Einladungen");
		titel.addClassName("devace-seitentitel");
		return titel;
	}

	private static VerticalLayout karte(String titel, String untertitel) {
		Span unter = new Span(untertitel);
		unter.addClassName("devace-untertitel");
		VerticalLayout karte = new VerticalLayout(new Div(new H3(titel), unter));
		karte.addClassName("devace-karte");
		karte.setPadding(false);
		karte.getStyle().set("padding", "1rem 1.25rem");
		return karte;
	}

	private static Div meldung(String titel, String text, String thema) {
		Span kopf = new Span(titel);
		kopf.addClassName("devace-hinweis-titel");
		Div box = new Div(kopf, new Span(text));
		box.addClassName("devace-hinweis");
		if (thema != null) {
			box.getElement().setAttribute("theme", thema);
		}
		return box;
	}

}
