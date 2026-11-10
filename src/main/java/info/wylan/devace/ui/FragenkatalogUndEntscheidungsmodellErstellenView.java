package info.wylan.devace.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import info.wylan.devace.persistence.FragenkatalogRepository;
import info.wylan.devace.persistence.Handlungsfeld;
import info.wylan.devace.persistence.KatalogFrage;
import info.wylan.devace.persistence.KatalogUebersicht;
import info.wylan.devace.persistence.RolleListItem;
import info.wylan.devace.service.Ausgangspunkt;
import info.wylan.devace.service.AusgangspunktNichtVerfuegbarException;
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

	/** Bezeichnung der Stufen 0 bis 5 (Glossar: Stufe). */
	private static final List<String> STUFEN = List.of("Nie gehört", "Schon mal gehört", "Grundverständnis",
			"Sicherer Umgang", "Fortgeschrittene Expertise", "Expert:in / Lehrniveau");

	private final FragenkatalogService service;

	private final List<Handlungsfeld> handlungsfelder;

	private final Grid<RolleListItem> rolleGrid = new Grid<>();

	private final IntegerField kiAnzahl = new IntegerField("Fragen je Handlungsfeld");

	private final TextArea kiSchwerpunkte = new TextArea("Schwerpunkte (optional)");

	private final Div inhalt = new Div();

	/** Der Entwurf dieser Sitzung, nach Handlungsfeld geordnet; die Position + 1 ist die Nummer der Frage. */
	private final List<KatalogFrage> entwurf = new ArrayList<>();

	/** Meldungen der letzten Prüfung (A4): Position der Frage, dann Stufe (−1 für den Wortlaut). */
	private final Map<Integer, Map<Integer, String>> pruefmeldungen = new HashMap<>();

	private @Nullable RolleListItem gewaehlteRolle;

	private int entwurfVersion;

	private @Nullable Div meldung;

	private int geoeffnetesHandlungsfeld;

	public FragenkatalogUndEntscheidungsmodellErstellenView(FragenkatalogService service) {
		this.service = service;
		this.handlungsfelder = service.handlungsfelder();
		setWidthFull();
		add(this.inhalt);
		this.inhalt.setWidthFull();
		this.kiAnzahl.setId("ki-anzahl");
		this.kiSchwerpunkte.setId("ki-schwerpunkte");
		zeigeUebersicht();
	}

	// ---------------------------------------------------------------- Übersicht (Schritte 1–3)

	/** UC-006 steps 1–2: die Rollen mit ihren Fragenkatalogen, deren Version und Status. */
	private void zeigeUebersicht() {
		this.gewaehlteRolle = null;
		this.entwurf.clear();
		this.pruefmeldungen.clear();
		this.meldung = null;

		this.rolleGrid.removeAllColumns();
		this.rolleGrid.addColumn(RolleListItem::name).setHeader("Rolle").setAutoWidth(true);
		this.rolleGrid
			.addComponentColumn((rolle) -> versionsAnzeige(rolle, FragenkatalogRepository.FREIGEGEBEN, "freigegeben"))
			.setHeader("Freigegebene Version")
			.setAutoWidth(true);
		this.rolleGrid.addComponentColumn((rolle) -> versionsAnzeige(rolle, FragenkatalogRepository.ENTWURF, "entwurf"))
			.setHeader("Entwurf")
			.setAutoWidth(true);
		this.rolleGrid.addComponentColumn(this::aktion).setHeader("").setAutoWidth(true).setFlexGrow(0);
		this.rolleGrid.addThemeVariants(GridVariant.NO_BORDER);
		this.rolleGrid.setId("rolle-grid");
		this.rolleGrid.setEmptyStateText("Noch keine Rolle angelegt.");
		this.rolleGrid.setAllRowsVisible(true);
		this.rolleGrid.setItems(this.service.rollen());

		VerticalLayout karte = karte("Fragenkataloge je Rolle",
				"Version und Status je Rolle. Eine Rolle hat höchstens einen Entwurf.");
		// UC-006 GR-005, GR-006: Nur der Entwurf ändert sich; er gilt erst nach der Freigabe in Assessments.
		karte.add(this.rolleGrid, fussnote(
				"Freigegebene und archivierte Fragenkataloge bleiben unverändert. Ein Entwurf gilt erst nach der Freigabe in Assessments."));
		setzeInhalt(seitentitel("Fragenkatalog"), karte);
	}

	private static Component versionsAnzeige(RolleListItem rolle, String status, String thema) {
		return rolle.fragenkataloge()
			.stream()
			.filter((katalog) -> status.equals(katalog.status()))
			.findFirst()
			.<Component>map((katalog) -> badge("Version " + katalog.version(), thema))
			.orElseGet(Span::new);
	}

	private Component aktion(RolleListItem rolle) {
		boolean hatEntwurf = hatStatus(rolle, FragenkatalogRepository.ENTWURF);
		boolean hatFreigegeben = hatStatus(rolle, FragenkatalogRepository.FREIGEGEBEN);
		String text = hatEntwurf ? "Entwurf fortsetzen" : (hatFreigegeben ? "Neue Version beginnen" : "Katalog beginnen");
		Button aktion = new Button(text, (event) -> rolleGewaehlt(rolle));
		aktion.addThemeVariants(ButtonVariant.TERTIARY);
		aktion.setAriaLabel(text + ": Rolle " + rolle.name());
		return aktion;
	}

	private static boolean hatStatus(RolleListItem rolle, String status) {
		return rolle.fragenkataloge().stream().anyMatch((katalog) -> status.equals(katalog.status()));
	}

	private static int hoechsteVersion(RolleListItem rolle) {
		return rolle.fragenkataloge().stream().mapToInt(KatalogUebersicht::version).max().orElse(0);
	}

	/** UC-006 step 3: der Administrator wählt eine Rolle und legt den Ausgangspunkt fest. */
	private void rolleGewaehlt(RolleListItem rolle) {
		Set<Ausgangspunkt> verfuegbar;
		try {
			verfuegbar = this.service.verfuegbareAusgangspunkte(rolle.id());
		}
		catch (NotFoundException ex) {
			fehler(ex.getMessage());
			zeigeUebersicht();
			return;
		}
		// UC-006 GR-004: Besteht ein Entwurf, setzt der Administrator ihn fort statt einen weiteren zu beginnen.
		if (verfuegbar.size() == 1) {
			beginnen(rolle, verfuegbar.iterator().next());
			return;
		}
		ausgangspunktDialog(rolle, new ArrayList<>(EnumSet.copyOf(verfuegbar)));
	}

	private void ausgangspunktDialog(RolleListItem rolle, List<Ausgangspunkt> moeglich) {
		Dialog dialog = new Dialog();
		dialog.setId("ausgangspunkt-dialog");
		dialog.setHeaderTitle("Fragenkatalog für die Rolle „" + rolle.name() + "“ beginnen");
		dialog.setWidth("34rem");
		int neueVersion = hoechsteVersion(rolle) + 1;
		int freigegeben = rolle.fragenkataloge()
			.stream()
			.filter((katalog) -> FragenkatalogRepository.FREIGEGEBEN.equals(katalog.status()))
			.mapToInt(KatalogUebersicht::version)
			.findFirst()
			.orElse(0);

		RadioButtonGroup<Ausgangspunkt> auswahl = new RadioButtonGroup<>("Ausgangspunkt");
		auswahl.setId("ausgangspunkt");
		auswahl.setItems(moeglich);
		auswahl.setItemLabelGenerator((punkt) -> switch (punkt) {
			case LEER -> "Leerer Entwurf";
			case FREIGEGEBENE_VERSION -> "Freigegebene Version " + freigegeben + " als neue Version " + neueVersion;
			case VORHANDENER_ENTWURF -> "Vorhandener Entwurf";
		});
		auswahl.setValue(moeglich.get(0));
		dialog.add(new VerticalLayout(auswahl, fussnote(
				"Die freigegebene Version bleibt unverändert; die Änderungen gelten nur für den neuen Entwurf.")));

		Button abbrechen = new Button("Abbrechen", (event) -> dialog.close());
		abbrechen.setId("ausgangspunkt-abbrechen");
		Button weiter = new Button("Weiter", (event) -> {
			dialog.close();
			beginnen(rolle, auswahl.getValue());
		});
		weiter.addThemeVariants(ButtonVariant.PRIMARY);
		weiter.setId("ausgangspunkt-uebernehmen");
		dialog.getFooter().add(abbrechen, weiter);
		dialog.open();
	}

	/** UC-006 step 4: zeigt den gewählten Ausgangspunkt je Handlungsfeld an. */
	private void beginnen(RolleListItem rolle, Ausgangspunkt punkt) {
		try {
			List<KatalogFrage> fragen = this.service.ausgangspunkt(rolle.id(), punkt);
			this.gewaehlteRolle = rolle;
			this.entwurfVersion = (punkt == Ausgangspunkt.VORHANDENER_ENTWURF)
					? rolle.fragenkataloge()
						.stream()
						.filter((katalog) -> FragenkatalogRepository.ENTWURF.equals(katalog.status()))
						.mapToInt(KatalogUebersicht::version)
						.findFirst()
						.orElse(hoechsteVersion(rolle) + 1)
					: hoechsteVersion(rolle) + 1;
			uebernehmeEntwurf(fragen);
			this.geoeffnetesHandlungsfeld = 1;
			zeigeEntwurf();
		}
		catch (NotFoundException | AusgangspunktNichtVerfuegbarException ex) {
			fehler(ex.getMessage());
			zeigeUebersicht();
		}
	}

	// ---------------------------------------------------------------- Entwurf (Schritte 7–9)

	private void uebernehmeEntwurf(List<KatalogFrage> fragen) {
		this.entwurf.clear();
		this.entwurf.addAll(fragen);
		sortiere();
		this.pruefmeldungen.clear();
	}

	private void sortiere() {
		this.entwurf.sort(Comparator.comparingInt((frage) -> reihenfolge(frage.handlungsfeldId())));
	}

	private int reihenfolge(long handlungsfeldId) {
		for (int i = 0; i < this.handlungsfelder.size(); i++) {
			if (this.handlungsfelder.get(i).id() == handlungsfeldId) {
				return i;
			}
		}
		return this.handlungsfelder.size();
	}

	private record Zeile(int index, KatalogFrage frage) {
	}

	/** UC-006 step 7: zeigt den Entwurf je Handlungsfeld an. */
	private void zeigeEntwurf() {
		RolleListItem rolle = this.gewaehlteRolle;
		if (rolle == null) {
			zeigeUebersicht();
			return;
		}
		VerticalLayout entwurfKarte = karte("Entwurf · Rolle " + rolle.name(), "Version " + this.entwurfVersion
				+ " · Status Entwurf · jede Frage gehört zu genau einem Handlungsfeld");
		entwurfKarte.getStyle().set("flex", "3");
		entwurfKarte.add(handlungsfelderAnzeige(), aktionen());

		VerticalLayout kiKarte = kiBereich();
		kiKarte.getStyle().set("flex", "2");

		HorizontalLayout spalten = new HorizontalLayout(entwurfKarte, kiKarte);
		spalten.setWidthFull();
		spalten.setAlignItems(FlexComponent.Alignment.START);

		List<Component> teile = new ArrayList<>();
		teile.add(seitentitel("Entwurf bearbeiten"));
		if (this.meldung != null) {
			teile.add(this.meldung);
		}
		teile.add(spalten);
		setzeInhalt(teile.toArray(Component[]::new));
	}

	private Accordion handlungsfelderAnzeige() {
		Accordion accordion = new Accordion();
		accordion.setWidthFull();
		for (Handlungsfeld feld : this.handlungsfelder) {
			List<Zeile> zeilen = new ArrayList<>();
			for (int i = 0; i < this.entwurf.size(); i++) {
				if (this.entwurf.get(i).handlungsfeldId() == feld.id()) {
					zeilen.add(new Zeile(i, this.entwurf.get(i)));
				}
			}
			Span anzahl = badge(zeilen.size() + " Fragen", zeilen.isEmpty() ? "entwurf" : "neutral");
			HorizontalLayout kopf = new HorizontalLayout(new Span(feld.name()), anzahl);
			kopf.setWidthFull();
			kopf.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
			AccordionPanel panel = new AccordionPanel(kopf, fragenGrid(zeilen));
			panel.setId("handlungsfeld-" + feld.nummer());
			accordion.add(panel);
		}
		accordion.close();
		if (this.geoeffnetesHandlungsfeld > 0) {
			accordion.open(this.geoeffnetesHandlungsfeld - 1);
		}
		accordion.addOpenedChangeListener((event) -> {
			OptionalInt offen = event.getOpenedIndex();
			this.geoeffnetesHandlungsfeld = offen.isPresent() ? offen.getAsInt() + 1 : 0;
		});
		return accordion;
	}

	private Grid<Zeile> fragenGrid(List<Zeile> zeilen) {
		Grid<Zeile> grid = new Grid<>();
		grid.addColumn((zeile) -> zeile.index() + 1).setHeader("Nr.").setAutoWidth(true).setFlexGrow(0);
		grid.addColumn((zeile) -> zeile.frage().text()).setHeader("Frage").setFlexGrow(1);
		grid.addComponentColumn((zeile) -> {
			long belegt = zeile.frage().vorgaben().stream().filter((v) -> v != null && !v.isBlank()).count();
			boolean fehlerhaft = this.pruefmeldungen.containsKey(zeile.index()) || belegt < STUFEN.size();
			return badge(belegt + "/" + STUFEN.size() + " Stufen", fehlerhaft && belegt < STUFEN.size() ? "fehler" : "freigegeben");
		}).setHeader("Stufen").setAutoWidth(true).setFlexGrow(0);
		grid.addComponentColumn((zeile) -> {
			Button bearbeiten = new Button("Bearbeiten", (event) -> zeigeFrage(zeile.index(), false));
			bearbeiten.addThemeVariants(ButtonVariant.TERTIARY);
			bearbeiten.setAriaLabel("Frage " + (zeile.index() + 1) + " bearbeiten");
			return bearbeiten;
		}).setAutoWidth(true).setFlexGrow(0);
		grid.addThemeVariants(GridVariant.NO_BORDER);
		grid.setAllRowsVisible(true);
		grid.setEmptyStateText("Noch keine Frage.");
		grid.setItems(zeilen);
		return grid;
	}

	private HorizontalLayout aktionen() {
		Button hinzufuegen = new Button("Frage hinzufügen", (event) -> frageHinzufuegen());
		hinzufuegen.addThemeVariants(ButtonVariant.TERTIARY);
		hinzufuegen.setId("frage-hinzufuegen");
		Button abbrechen = new Button("Abbrechen", (event) -> abbrechen());
		abbrechen.addThemeVariants(ButtonVariant.TERTIARY);
		abbrechen.setId("entwurf-abbrechen");
		Button speichern = new Button("Entwurf speichern", (event) -> speichern());
		speichern.addThemeVariants(ButtonVariant.PRIMARY);
		speichern.setId("entwurf-speichern");
		HorizontalLayout rechts = new HorizontalLayout(abbrechen, speichern);
		HorizontalLayout leiste = new HorizontalLayout(hinzufuegen, rechts);
		leiste.setWidthFull();
		leiste.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
		return leiste;
	}

	/** UC-006 step 8: ergänzt eine Frage im geöffneten Handlungsfeld (sonst im ersten). */
	private void frageHinzufuegen() {
		Handlungsfeld feld = this.handlungsfelder.get(Math.max(this.geoeffnetesHandlungsfeld, 1) - 1);
		KatalogFrage neu = new KatalogFrage(feld.id(), "", List.of("", "", "", "", "", ""));
		int position = this.entwurf.size();
		for (int i = 0; i < this.entwurf.size(); i++) {
			if (reihenfolge(this.entwurf.get(i).handlungsfeldId()) > reihenfolge(feld.id())) {
				position = i;
				break;
			}
		}
		this.entwurf.add(position, neu);
		this.pruefmeldungen.clear();
		zeigeFrage(position, true);
	}

	private VerticalLayout kiBereich() {
		this.kiAnzahl.setValue((this.kiAnzahl.getValue() != null) ? this.kiAnzahl.getValue() : VORSCHLAG_ANZAHL);
		this.kiAnzahl.setStepButtonsVisible(true);
		this.kiAnzahl.setMin(1);
		this.kiAnzahl.setMax(10);
		this.kiAnzahl.setWidthFull();
		// UC-006 GR-003: Die Zahl der Fragen je Handlungsfeld ist eine ganze Zahl von 1 bis 10, vorgeschlagen werden 3.
		this.kiAnzahl.setHelperText("1 bis 10, vorgeschlagen sind " + VORSCHLAG_ANZAHL);
		this.kiSchwerpunkte.setWidthFull();
		this.kiSchwerpunkte.setHelperText("Höchstens 2000 Zeichen");
		Button anfordern = new Button("KI-Entwurf anfordern", (event) -> kiAnfordern());
		anfordern.addThemeVariants(ButtonVariant.PRIMARY);
		anfordern.setId("ki-anfordern");

		VerticalLayout karte = karte("KI-Entwurf anfordern", "Das KI-Modell ergänzt Fragen mit Entscheidungsmodell");
		// UC-006 GR-006, GR-007, GR-010: Vorschlag; nur Rolle, Handlungsfelder, Schwerpunkte, Zahl; Vorhandenes bleibt.
		Div hinweis = new Div(hinweisTitel("Nur ein Vorschlag"), new Span(
				"Die Anfrage enthält nur Rolle, Handlungsfelder, Schwerpunkte und Zahl der Fragen, keine Angaben zu Personen. Vorhandene Fragen bleiben unverändert."));
		hinweis.addClassName("devace-hinweis");
		karte.add(this.kiAnzahl, this.kiSchwerpunkte, anfordern, hinweis);
		return karte;
	}

	private List<KatalogFrage> aktuellerEntwurf() {
		return List.copyOf(this.entwurf);
	}

	/** UC-006 steps 5–7, A2, A3, A6: fordert den KI-Entwurf an. */
	private void kiAnfordern() {
		RolleListItem rolle = this.gewaehlteRolle;
		KiAngaben angaben = kiAngaben();
		if (rolle == null || angaben == null) {
			return;
		}
		this.meldung = null;
		try {
			KiErgebnis ergebnis = this.service.kiEntwurf(rolle.id(), aktuellerEntwurf(), angaben);
			switch (ergebnis) {
				case KiErgebnis.Ergaenzt ergaenzt -> {
					uebernehmeEntwurf(ergaenzt.entwurf());
					zeigeEntwurf();
				}
				case KiErgebnis.GenuegendFragen genug -> genugFragenDialog(rolle, genug.anzahl(), angaben);
			}
		}
		catch (KeinKiEntwurfException ex) {
			// UC-006 A2: Es ist kein Entwurf entstanden; Ausgangspunkt und Angaben bleiben, weiter bei Schritt 5.
			this.meldung = meldungsBox("Kein Entwurf entstanden", ex.getMessage(), "fehler");
			zeigeEntwurf();
		}
	}

	/** UC-006 A6 und A7: Hinweis auf genügend Fragen, weitere ergänzen oder abbrechen. */
	private void genugFragenDialog(RolleListItem rolle, int anzahl, KiAngaben bisher) {
		Dialog dialog = new Dialog();
		dialog.setId("genug-fragen-dialog");
		dialog.setHeaderTitle("Genügend Fragen vorhanden");
		dialog.setWidth("34rem");
		IntegerField weitere = new IntegerField("Weitere Fragen je Handlungsfeld");
		weitere.setId("weitere-anzahl");
		weitere.setValue(VORSCHLAG_ANZAHL);
		weitere.setStepButtonsVisible(true);
		weitere.setWidthFull();
		weitere.setHelperText("1 bis 10");
		Paragraph text = new Paragraph("Jedes Handlungsfeld hat bereits mindestens " + anzahl
				+ " Fragen. Sie können weitere Fragen vom KI-Modell entwerfen lassen oder die Anforderung abbrechen.");
		dialog.add(new VerticalLayout(text, weitere));
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
					uebernehmeEntwurf(ergaenzt.entwurf());
				}
				zeigeEntwurf();
			}
			catch (KeinKiEntwurfException ex) {
				dialog.close();
				this.meldung = meldungsBox("Kein Entwurf entstanden", ex.getMessage(), "fehler");
				zeigeEntwurf();
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
		this.meldung = null;
		this.pruefmeldungen.clear();
		try {
			this.service.speichern(rolle.id(), aktuellerEntwurf());
		}
		catch (EntwurfUngueltigException ex) {
			// UC-006 A4: Hinweis an den betroffenen Fragen und Angaben, weiter bei Schritt 8.
			List<String> zeilen = new ArrayList<>();
			for (EntwurfUngueltigException.Fehler f : ex.fehler()) {
				this.pruefmeldungen.computeIfAbsent(f.frageIndex(), (k) -> new HashMap<>()).put(f.stufe(), f.meldung());
				zeilen.add("Frage " + (f.frageIndex() + 1) + " (" + handlungsfeldName(f.frageIndex()) + "): "
						+ ((f.stufe() < 0) ? "" : "Stufe " + f.stufe() + " – ") + f.meldung());
			}
			this.meldung = meldungsBox("Der Entwurf ist unvollständig", String.join(" ", zeilen), "fehler");
			zeigeEntwurf();
			return;
		}
		catch (NotFoundException ex) {
			fehler(ex.getMessage());
			zeigeUebersicht();
			return;
		}
		Notification notification = Notification.show("Entwurf gespeichert.");
		notification.addThemeVariants(NotificationVariant.SUCCESS);
		zeigeUebersicht();
	}

	private String handlungsfeldName(int frageIndex) {
		long id = this.entwurf.get(frageIndex).handlungsfeldId();
		return this.handlungsfelder.stream().filter((f) -> f.id() == id).map(Handlungsfeld::name).findFirst().orElse("");
	}

	/** UC-006 A5: verwirft die Änderungen dieser Sitzung. */
	private void abbrechen() {
		zeigeUebersicht();
	}

	// ---------------------------------------------------------------- Frage und Entscheidungsmodell (Schritt 8)

	private void zeigeFrage(int index, boolean neu) {
		KatalogFrage frage = this.entwurf.get(index);
		RolleListItem rolle = this.gewaehlteRolle;
		Map<Integer, String> meldungen = this.pruefmeldungen.getOrDefault(index, Map.of());

		TextArea wortlaut = new TextArea("Fragetext");
		wortlaut.setId("frage-text");
		wortlaut.setWidthFull();
		wortlaut.setMinHeight("3rem");
		wortlaut.setValue(frage.text());
		wortlaut.setHelperText("Pflichtangabe, höchstens 500 Zeichen");
		markiere(wortlaut, meldungen.get(EntwurfUngueltigException.Fehler.WORTLAUT));

		FormLayout stufen = new FormLayout();
		stufen.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("40rem", 3));
		List<TextArea> vorgaben = new ArrayList<>();
		for (int stufe = 0; stufe < STUFEN.size(); stufe++) {
			TextArea vorgabe = new TextArea("Stufe " + stufe + " – " + STUFEN.get(stufe));
			vorgabe.setId("vorgabe-" + stufe);
			vorgabe.setMinHeight("5rem");
			vorgabe.setValue((stufe < frage.vorgaben().size()) ? frage.vorgaben().get(stufe) : "");
			markiere(vorgabe, meldungen.get(stufe));
			vorgaben.add(vorgabe);
			stufen.add(vorgabe);
		}

		Button entfernen = new Button("Frage entfernen", (event) -> {
			this.entwurf.remove(index);
			this.pruefmeldungen.clear();
			zeigeEntwurf();
		});
		entfernen.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.ERROR);
		entfernen.setId("frage-entfernen");
		Button abbrechen = new Button("Abbrechen", (event) -> {
			if (neu) {
				this.entwurf.remove(index);
			}
			zeigeEntwurf();
		});
		abbrechen.addThemeVariants(ButtonVariant.TERTIARY);
		abbrechen.setId("frage-abbrechen");
		Button uebernehmen = new Button("Übernehmen", (event) -> {
			this.entwurf.set(index, new KatalogFrage(frage.handlungsfeldId(), wortlaut.getValue(),
					vorgaben.stream().map(TextArea::getValue).toList()));
			this.pruefmeldungen.remove(index);
			zeigeEntwurf();
		});
		uebernehmen.addThemeVariants(ButtonVariant.PRIMARY);
		uebernehmen.setId("frage-uebernehmen");
		HorizontalLayout rechts = new HorizontalLayout(abbrechen, uebernehmen);
		HorizontalLayout leiste = new HorizontalLayout(entfernen, rechts);
		leiste.setWidthFull();
		leiste.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

		VerticalLayout karte = karte("Frage " + (index + 1) + " · " + handlungsfeldName(index),
				"Jede Frage hat genau eine Vorgabe je Stufe von 0 bis 5. Die KI ordnet Antworten diesen Vorgaben zu.");
		karte.add(wortlaut, new H3("Entscheidungsmodell: Vorgabe je Stufe"), stufen, leiste);
		Span pfad = fussnote("Fragenkatalog › " + ((rolle != null) ? rolle.name() : "") + " › Version "
				+ this.entwurfVersion + " › Frage " + (index + 1));
		setzeInhalt(seitentitel("Frage bearbeiten"), pfad, karte);
	}

	private static void markiere(TextArea feld, @Nullable String meldung) {
		if (meldung != null) {
			feld.setInvalid(true);
			feld.setErrorMessage(meldung);
		}
	}

	// ---------------------------------------------------------------- Bausteine

	private void setzeInhalt(Component... teile) {
		this.inhalt.removeAll();
		VerticalLayout spalte = new VerticalLayout(teile);
		spalte.setPadding(false);
		this.inhalt.add(spalte);
	}

	private static H2 seitentitel(String text) {
		H2 titel = new H2(text);
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

	private static Span fussnote(String text) {
		Span note = new Span(text);
		note.addClassName("devace-fussnote");
		return note;
	}

	private static Span badge(String text, String thema) {
		Span badge = new Span(text);
		badge.addClassName("devace-badge");
		badge.getElement().setAttribute("theme", thema);
		return badge;
	}

	private static Span hinweisTitel(String text) {
		Span titel = new Span(text);
		titel.addClassName("devace-hinweis-titel");
		return titel;
	}

	private static Div meldungsBox(String titel, String text, String thema) {
		Div box = new Div(hinweisTitel(titel), new Span(text));
		box.addClassName("devace-hinweis");
		box.getElement().setAttribute("theme", thema);
		return box;
	}

	private static void fehler(String text) {
		Notification notification = Notification.show(text);
		notification.addThemeVariants(NotificationVariant.ERROR);
	}

}
