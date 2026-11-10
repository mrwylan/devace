package info.wylan.devace.ui;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import info.wylan.devace.persistence.Handlungsfeld;
import info.wylan.devace.persistence.KatalogFrage;
import info.wylan.devace.persistence.RolleListItem;
import info.wylan.devace.service.Ausgangspunkt;
import info.wylan.devace.service.EntwurfUngueltigException;
import info.wylan.devace.service.FragenkatalogService;
import info.wylan.devace.service.KeinKiEntwurfException;
import info.wylan.devace.service.KiAngaben;
import info.wylan.devace.service.KiErgebnis;
import info.wylan.devace.service.NotFoundException;
import jakarta.annotation.security.RolesAllowed;
import org.jspecify.annotations.Nullable;

/**
 * UC-006 Fragenkatalog und Entscheidungsmodell erstellen.
 */
@Route(value = "fragenkatalog-erstellen", layout = MainLayout.class)
@PageTitle("Fragenkatalog und Entscheidungsmodell erstellen")
// UC-006 GR-009: Nur der angemeldete Administrator erstellt Fragenkataloge und Entscheidungsmodelle.
@RolesAllowed("ADMINISTRATOR")
public class FragenkatalogUndEntscheidungsmodellErstellenView extends VerticalLayout {

	private static final int VORSCHLAG_ANZAHL = 3;

	private final FragenkatalogService service;

	private final List<Handlungsfeld> handlungsfelder;

	private final Grid<RolleListItem> rolleGrid = new Grid<>();

	private final RadioButtonGroup<Ausgangspunkt> ausgangspunkt = new RadioButtonGroup<>("Ausgangspunkt");

	private final Button uebernehmen = new Button("Bearbeitung beginnen");

	private final VerticalLayout ausgangspunktBereich = new VerticalLayout();

	private final VerticalLayout editor = new VerticalLayout();

	private final Map<Long, VerticalLayout> fragenJeHandlungsfeld = new LinkedHashMap<>();

	private final IntegerField kiAnzahl = new IntegerField("Fragen je Handlungsfeld");

	private final TextArea kiSchwerpunkte = new TextArea("Schwerpunkte (optional)");

	private @Nullable RolleListItem gewaehlteRolle;

	public FragenkatalogUndEntscheidungsmodellErstellenView(FragenkatalogService service) {
		this.service = service;
		this.handlungsfelder = service.handlungsfelder();
		setWidthFull();

		this.rolleGrid.addColumn(RolleListItem::name).setHeader("Rolle").setAutoWidth(true);
		this.rolleGrid.addColumn(FragenkatalogUndEntscheidungsmodellErstellenView::katalogeText)
			.setHeader("Fragenkataloge")
			.setAutoWidth(true);
		this.rolleGrid.addThemeVariants(GridVariant.NO_BORDER);
		this.rolleGrid.setId("rolle-grid");
		this.rolleGrid.setEmptyStateText("Noch keine Rolle angelegt.");
		this.rolleGrid.setAllRowsVisible(true);
		this.rolleGrid.addSelectionListener((event) -> event.getFirstSelectedItem().ifPresentOrElse(this::rolleGewaehlt,
				this::zuruecksetzen));

		this.ausgangspunkt.setId("ausgangspunkt");
		this.ausgangspunkt.setItemLabelGenerator(FragenkatalogUndEntscheidungsmodellErstellenView::ausgangspunktText);
		this.uebernehmen.setId("ausgangspunkt-uebernehmen");
		this.uebernehmen.addClickListener((event) -> beginnen());
		this.ausgangspunktBereich.add(this.ausgangspunkt, this.uebernehmen);
		this.ausgangspunktBereich.setPadding(false);
		this.ausgangspunktBereich.setVisible(false);

		this.editor.setPadding(false);
		this.editor.setVisible(false);
		add(new H2("Fragenkataloge"), this.rolleGrid, this.ausgangspunktBereich, this.editor);
		aktualisierenRollen();
	}

	private void aktualisierenRollen() {
		this.rolleGrid.setItems(this.service.rollen());
	}

	private static String katalogeText(RolleListItem rolle) {
		if (rolle.fragenkataloge().isEmpty()) {
			return "–";
		}
		return String.join(", ", rolle.fragenkataloge().stream().map((k) -> "Version " + k.version() + " (" + k.status() + ")").toList());
	}

	private static String ausgangspunktText(Ausgangspunkt punkt) {
		return switch (punkt) {
			case LEER -> "Leerer Entwurf";
			case FREIGEGEBENE_VERSION -> "Freigegebene Version als neue Version";
			case VORHANDENER_ENTWURF -> "Vorhandener Entwurf";
		};
	}

	/** UC-006 step 3: die Rolle ist gewählt, der Administrator legt den Ausgangspunkt fest. */
	private void rolleGewaehlt(RolleListItem rolle) {
		this.gewaehlteRolle = rolle;
		this.editor.setVisible(false);
		Set<Ausgangspunkt> verfuegbar;
		try {
			verfuegbar = this.service.verfuegbareAusgangspunkte(rolle.id());
		}
		catch (NotFoundException ex) {
			fehler(ex.getMessage());
			aktualisierenRollen();
			return;
		}
		List<Ausgangspunkt> items = new ArrayList<>(EnumSet.copyOf(verfuegbar));
		this.ausgangspunkt.setItems(items);
		this.ausgangspunkt.setValue(items.get(0));
		this.ausgangspunktBereich.setVisible(true);
	}

	private void zuruecksetzen() {
		this.gewaehlteRolle = null;
		this.ausgangspunktBereich.setVisible(false);
		this.editor.setVisible(false);
	}

	/** UC-006 step 4: zeigt den gewählten Ausgangspunkt je Handlungsfeld an. */
	private void beginnen() {
		RolleListItem rolle = this.gewaehlteRolle;
		Ausgangspunkt punkt = this.ausgangspunkt.getValue();
		if (rolle == null || punkt == null) {
			return;
		}
		try {
			aufbauen(this.service.ausgangspunkt(rolle.id(), punkt));
		}
		catch (NotFoundException | info.wylan.devace.service.AusgangspunktNichtVerfuegbarException ex) {
			fehler(ex.getMessage());
			aktualisierenRollen();
		}
	}

	/** Baut den Editor mit dem Entwurf je Handlungsfeld auf (steps 4 and 7). */
	private void aufbauen(List<KatalogFrage> fragen) {
		this.editor.removeAll();
		this.fragenJeHandlungsfeld.clear();
		RolleListItem rolle = this.gewaehlteRolle;
		this.editor.add(new H2("Entwurf für " + ((rolle != null) ? rolle.name() : "")));
		for (Handlungsfeld feld : this.handlungsfelder) {
			VerticalLayout liste = new VerticalLayout();
			liste.setPadding(false);
			this.fragenJeHandlungsfeld.put(feld.id(), liste);
			for (KatalogFrage frage : fragen) {
				if (frage.handlungsfeldId() == feld.id()) {
					liste.add(new FrageEditor(frage, liste));
				}
			}
			Button ergaenzen = new Button("Frage ergänzen");
			ergaenzen.setId("frage-ergaenzen-" + feld.nummer());
			ergaenzen.addClickListener((event) -> liste.add(new FrageEditor(
					new KatalogFrage(feld.id(), "", List.of("", "", "", "", "", "")), liste)));
			this.editor.add(new H3(feld.nummer() + " " + feld.name()), liste, ergaenzen);
		}
		this.editor.add(kiBereich(), aktionen());
		this.editor.setVisible(true);
	}

	private VerticalLayout kiBereich() {
		this.kiAnzahl.setId("ki-anzahl");
		this.kiAnzahl.setValue(VORSCHLAG_ANZAHL);
		this.kiAnzahl.setStepButtonsVisible(true);
		this.kiAnzahl.setMin(1);
		this.kiAnzahl.setMax(10);
		this.kiSchwerpunkte.setId("ki-schwerpunkte");
		this.kiSchwerpunkte.setWidthFull();
		this.kiSchwerpunkte.setValue("");
		Button anfordern = new Button("KI-Entwurf anfordern", (event) -> kiAnfordern());
		anfordern.setId("ki-anfordern");
		VerticalLayout bereich = new VerticalLayout(new H3("KI-Entwurf"), this.kiAnzahl, this.kiSchwerpunkte, anfordern);
		bereich.setPadding(false);
		return bereich;
	}

	private HorizontalLayout aktionen() {
		Button speichern = new Button("Entwurf speichern", (event) -> speichern());
		speichern.addThemeVariants(ButtonVariant.PRIMARY);
		speichern.setId("entwurf-speichern");
		Button abbrechen = new Button("Abbrechen", (event) -> abbrechen());
		abbrechen.setId("entwurf-abbrechen");
		return new HorizontalLayout(abbrechen, speichern);
	}

	private List<FrageEditor> editoren() {
		List<FrageEditor> alle = new ArrayList<>();
		for (VerticalLayout liste : this.fragenJeHandlungsfeld.values()) {
			liste.getChildren().filter(FrageEditor.class::isInstance).map(FrageEditor.class::cast).forEach(alle::add);
		}
		return alle;
	}

	private List<KatalogFrage> aktuellerEntwurf() {
		return editoren().stream().map(FrageEditor::toFrage).toList();
	}

	/** UC-006 steps 5–7, A2, A3, A6: fordert den KI-Entwurf an. */
	private void kiAnfordern() {
		RolleListItem rolle = this.gewaehlteRolle;
		KiAngaben angaben = kiAngaben();
		if (rolle == null || angaben == null) {
			return;
		}
		try {
			KiErgebnis ergebnis = this.service.kiEntwurf(rolle.id(), aktuellerEntwurf(), angaben);
			uebernehmen(rolle, ergebnis, angaben);
		}
		catch (KeinKiEntwurfException ex) {
			// UC-006 A2: Es ist kein Entwurf entstanden; Ausgangspunkt und Angaben bleiben, weiter bei Schritt 5.
			fehler(ex.getMessage());
		}
	}

	private void uebernehmen(RolleListItem rolle, KiErgebnis ergebnis, KiAngaben angaben) {
		switch (ergebnis) {
			case KiErgebnis.Ergaenzt ergaenzt -> aufbauenMitErhalt(ergaenzt.entwurf());
			case KiErgebnis.GenuegendFragen genug -> genugFragenDialog(rolle, genug.anzahl(), angaben);
		}
	}

	/** Zeigt den ergänzten Entwurf an und erhält die Angaben des KI-Bereichs. */
	private void aufbauenMitErhalt(List<KatalogFrage> entwurf) {
		Integer anzahl = this.kiAnzahl.getValue();
		String schwerpunkte = this.kiSchwerpunkte.getValue();
		aufbauen(entwurf);
		this.kiAnzahl.setValue(anzahl);
		this.kiSchwerpunkte.setValue(schwerpunkte);
	}

	/** UC-006 A6 und A7: Hinweis auf genügend Fragen, weitere ergänzen oder abbrechen. */
	private void genugFragenDialog(RolleListItem rolle, int anzahl, KiAngaben bisher) {
		Dialog dialog = new Dialog();
		dialog.setId("genug-fragen-dialog");
		dialog.setHeaderTitle("Genügend Fragen vorhanden");
		IntegerField weitere = new IntegerField("Weitere Fragen je Handlungsfeld");
		weitere.setId("weitere-anzahl");
		weitere.setValue(VORSCHLAG_ANZAHL);
		weitere.setStepButtonsVisible(true);
		dialog.add(new VerticalLayout(new Paragraph("Jedes Handlungsfeld hat bereits mindestens " + anzahl + " Fragen."),
				weitere));
		Button abbrechen = new Button("Abbrechen", (event) -> dialog.close());
		abbrechen.setId("genug-abbrechen");
		Button ergaenzen = new Button("Weitere Fragen ergänzen", (event) -> {
			// UC-006 GR-003: Dieselbe Grenze 1 bis 10 gilt für die Zahl weiterer Fragen je Handlungsfeld.
			Integer wert = weitere.getValue();
			if (wert == null || wert < 1 || wert > 10) {
				weitere.setInvalid(true);
				weitere.setErrorMessage("Die Zahl muss zwischen 1 und 10 liegen.");
				return;
			}
			try {
				KiErgebnis ergebnis = this.service.weitereFragen(rolle.id(), aktuellerEntwurf(),
						new KiAngaben(wert, bisher.schwerpunkte()));
				dialog.close();
				if (ergebnis instanceof KiErgebnis.Ergaenzt ergaenzt) {
					aufbauenMitErhalt(ergaenzt.entwurf());
				}
			}
			catch (KeinKiEntwurfException ex) {
				dialog.close();
				fehler(ex.getMessage());
			}
		});
		ergaenzen.addThemeVariants(ButtonVariant.PRIMARY);
		ergaenzen.setId("genug-ergaenzen");
		dialog.getFooter().add(abbrechen, ergaenzen);
		dialog.open();
	}

	/** UC-006 A3: prüft die Angaben für den KI-Entwurf. */
	private @Nullable KiAngaben kiAngaben() {
		boolean gueltig = true;
		this.kiAnzahl.setInvalid(false);
		this.kiSchwerpunkte.setInvalid(false);
		Integer anzahl = this.kiAnzahl.getValue();
		// UC-006 GR-003: Die Zahl der Fragen je Handlungsfeld ist eine ganze Zahl von 1 bis 10.
		if (anzahl == null || anzahl < 1 || anzahl > 10) {
			this.kiAnzahl.setInvalid(true);
			this.kiAnzahl.setErrorMessage("Die Zahl muss zwischen 1 und 10 liegen.");
			gueltig = false;
		}
		// UC-006 GR-003: Die Schwerpunkte haben höchstens 2000 Zeichen.
		if (this.kiSchwerpunkte.getValue().strip().length() > 2000) {
			this.kiSchwerpunkte.setInvalid(true);
			this.kiSchwerpunkte.setErrorMessage("Höchstens 2000 Zeichen.");
			gueltig = false;
		}
		return gueltig ? new KiAngaben(anzahl, this.kiSchwerpunkte.getValue()) : null;
	}

	/** UC-006 steps 9–12 und A4: speichert den Entwurf. */
	private void speichern() {
		RolleListItem rolle = this.gewaehlteRolle;
		if (rolle == null) {
			return;
		}
		List<FrageEditor> editoren = editoren();
		editoren.forEach(FrageEditor::zuruecksetzen);
		try {
			this.service.speichern(rolle.id(), editoren.stream().map(FrageEditor::toFrage).toList());
		}
		catch (EntwurfUngueltigException ex) {
			// UC-006 A4: Hinweis an den betroffenen Fragen und Angaben, weiter bei Schritt 8.
			ex.fehler().forEach((f) -> editoren.get(f.frageIndex()).markiere(f.stufe(), f.meldung()));
			return;
		}
		catch (NotFoundException ex) {
			fehler(ex.getMessage());
			aktualisierenRollen();
			return;
		}
		Notification notification = Notification.show("Entwurf gespeichert.");
		notification.addThemeVariants(NotificationVariant.SUCCESS);
		aktualisierenRollen();
		zuruecksetzen();
		this.rolleGrid.deselectAll();
	}

	/** UC-006 A5: verwirft die Änderungen dieser Sitzung. */
	private void abbrechen() {
		zuruecksetzen();
		this.rolleGrid.deselectAll();
	}

	private static void fehler(String text) {
		Notification notification = Notification.show(text);
		notification.addThemeVariants(NotificationVariant.ERROR);
	}

	/** Eine Frage mit ihrem Entscheidungsmodell (sechs Vorgaben). */
	private static class FrageEditor extends VerticalLayout {

		private final long handlungsfeldId;

		private final TextArea wortlaut = new TextArea("Frage");

		private final List<TextArea> stufen = new ArrayList<>();

		FrageEditor(KatalogFrage frage, VerticalLayout liste) {
			this.handlungsfeldId = frage.handlungsfeldId();
			setPadding(false);
			this.wortlaut.setWidthFull();
			this.wortlaut.setValue(frage.text());
			add(this.wortlaut);
			for (int stufe = 0; stufe < 6; stufe++) {
				TextArea vorgabe = new TextArea("Vorgabe Stufe " + stufe);
				vorgabe.setWidthFull();
				vorgabe.setValue((stufe < frage.vorgaben().size()) ? frage.vorgaben().get(stufe) : "");
				this.stufen.add(vorgabe);
				add(vorgabe);
			}
			Button entfernen = new Button("Frage entfernen", (event) -> liste.remove(this));
			add(entfernen);
		}

		KatalogFrage toFrage() {
			return new KatalogFrage(this.handlungsfeldId, this.wortlaut.getValue(),
					this.stufen.stream().map(TextArea::getValue).toList());
		}

		void zuruecksetzen() {
			this.wortlaut.setInvalid(false);
			this.stufen.forEach((s) -> s.setInvalid(false));
		}

		void markiere(int stufe, String meldung) {
			TextArea feld = (stufe < 0) ? this.wortlaut : this.stufen.get(stufe);
			feld.setInvalid(true);
			feld.setErrorMessage(meldung);
		}

	}

}
