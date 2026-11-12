package info.wylan.devace.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import info.wylan.devace.persistence.Fragenkatalog;
import info.wylan.devace.persistence.FreigabeKandidat;
import info.wylan.devace.persistence.Handlungsfeld;
import info.wylan.devace.persistence.KatalogFrage;
import info.wylan.devace.service.EntwurfUnvollstaendigException;
import info.wylan.devace.service.FragenkatalogService;
import info.wylan.devace.service.FreigabeZusammenfassung;
import info.wylan.devace.service.KeinEntwurfZurFreigabeException;
import info.wylan.devace.service.NotFoundException;
import jakarta.annotation.security.RolesAllowed;
import org.jspecify.annotations.Nullable;

/**
 * UC-007 Fragenkatalog freigeben.
 */
@Route(value = "fragenkatalog-freigeben", layout = MainLayout.class)
@PageTitle("Fragenkatalog freigeben")
// UC-007 BR-007: Nur der angemeldete Administrator gibt Fragenkataloge frei.
@RolesAllowed("ADMINISTRATOR")
public class FragenkatalogFreigebenView extends VerticalLayout implements BeforeEnterObserver {

	static final String UNVOLLSTAENDIG_TITEL = "Der Entwurf ist unvollständig und wird nicht freigegeben.";

	static final String FREIGEGEBEN = "Fragenkatalog freigegeben.";

	private final FragenkatalogService service;

	private final Div inhalt = new Div();

	private @Nullable FreigabeKandidat gewaehlt;

	private @Nullable Div meldung;

	public FragenkatalogFreigebenView(FragenkatalogService service) {
		this.service = service;
		setWidthFull();
		this.inhalt.setWidthFull();
		add(this.inhalt);
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		zeigeUebersicht();
	}

	// ---------------------------------------------------------------- Auswahl (Schritte 1–3, A1)

	/** UC-007 steps 1–2: die Rollen mit einem Entwurf zur Auswahl; A1, wenn es keine gibt. */
	private void zeigeUebersicht() {
		this.gewaehlt = null;
		this.meldung = null;
		List<FreigabeKandidat> kandidaten;
		try {
			kandidaten = this.service.rollenMitEntwurf();
		}
		catch (KeinEntwurfZurFreigabeException ex) {
			// UC-007 A1: Hinweis, dass kein Entwurf zur Freigabe vorliegt; der Use Case endet.
			Div hinweis = meldungsBox("Kein Entwurf", ex.getMessage(), "warnung");
			hinweis.setId("kein-entwurf");
			setzeInhalt(seitentitel("Fragenkatalog freigeben"), hinweis);
			return;
		}

		Grid<FreigabeKandidat> grid = new Grid<>();
		grid.setId("freigabe-grid");
		grid.addColumn(FreigabeKandidat::rolle).setHeader("Rolle").setAutoWidth(true);
		grid.addComponentColumn((k) -> badge("Version " + k.entwurfVersion(), "entwurf"))
			.setHeader("Entwurf")
			.setAutoWidth(true);
		grid.addComponentColumn((k) -> (k.freigegebeneVersion() != null)
				? badge("Version " + k.freigegebeneVersion(), "freigegeben") : new Span("–"))
			.setHeader("Freigegebene Version")
			.setAutoWidth(true);
		grid.addComponentColumn((k) -> {
			Button pruefen = new Button("Prüfen", (event) -> rolleGewaehlt(k));
			pruefen.addThemeVariants(ButtonVariant.TERTIARY);
			pruefen.setAriaLabel("Entwurf der Rolle " + k.rolle() + " prüfen");
			return pruefen;
		}).setAutoWidth(true).setFlexGrow(0);
		grid.addThemeVariants(GridVariant.NO_BORDER);
		grid.setAllRowsVisible(true);
		grid.setItems(kandidaten);

		VerticalLayout karte = karte("Entwürfe zur Freigabe", "Rollen mit einem Entwurf, mit der bisher freigegebenen Version.");
		karte.add(grid, fussnote("Mit der Freigabe wird die bisher freigegebene Version der Rolle archiviert. "
				+ "Laufende Assessments behalten ihren Fragenkatalog."));
		setzeInhalt(seitentitel("Fragenkatalog freigeben"), karte);
	}

	/** UC-007 step 3: der Administrator wählt eine Rolle. */
	private void rolleGewaehlt(FreigabeKandidat kandidat) {
		this.gewaehlt = kandidat;
		this.meldung = null;
		zeigeEntwurf();
	}

	// ---------------------------------------------------------------- Prüfung (Schritte 4–5, A2, A3)

	/** UC-007 step 4: der Entwurf je Handlungsfeld mit Fragen und Entscheidungsmodell zur Prüfung. */
	private void zeigeEntwurf() {
		FreigabeKandidat kandidat = this.gewaehlt;
		if (kandidat == null) {
			zeigeUebersicht();
			return;
		}
		Fragenkatalog entwurf;
		try {
			entwurf = this.service.entwurfZurPruefung(kandidat.rolleId());
		}
		catch (KeinEntwurfZurFreigabeException | NotFoundException ex) {
			fehler(ex.getMessage());
			zeigeUebersicht();
			return;
		}

		VerticalLayout karte = karte("Entwurf · Rolle " + kandidat.rolle(),
				"Version " + entwurf.version() + " · Status Entwurf · Fragen und Entscheidungsmodell je Handlungsfeld");
		karte.add(handlungsfelder(entwurf.fragen()), aktionen());

		List<Component> teile = new ArrayList<>();
		teile.add(seitentitel("Entwurf prüfen"));
		if (this.meldung != null) {
			teile.add(this.meldung);
		}
		teile.add(karte);
		setzeInhalt(teile.toArray(Component[]::new));
	}

	private Accordion handlungsfelder(List<KatalogFrage> fragen) {
		Accordion accordion = new Accordion();
		accordion.setWidthFull();
		for (Handlungsfeld feld : this.service.handlungsfelder()) {
			VerticalLayout liste = new VerticalLayout();
			liste.setPadding(false);
			int anzahl = 0;
			for (int i = 0; i < fragen.size(); i++) {
				if (fragen.get(i).handlungsfeldId() == feld.id()) {
					liste.add(frage(i + 1, fragen.get(i)));
					anzahl++;
				}
			}
			if (anzahl == 0) {
				liste.add(new Paragraph("Noch keine Frage."));
			}
			Span zahl = badge(anzahl + " Fragen", (anzahl == 0) ? "fehler" : "neutral");
			HorizontalLayout kopf = new HorizontalLayout(new Span(feld.name()), zahl);
			kopf.setWidthFull();
			kopf.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
			AccordionPanel panel = new AccordionPanel(kopf, liste);
			panel.setId("handlungsfeld-" + feld.nummer());
			accordion.add(panel);
		}
		accordion.open(0);
		return accordion;
	}

	private static Component frage(int nummer, KatalogFrage frage) {
		UnorderedList modell = new UnorderedList();
		List<String> stufen = FragenkatalogUndEntscheidungsmodellErstellenView.STUFEN;
		for (int stufe = 0; stufe < stufen.size(); stufe++) {
			String vorgabe = (stufe < frage.vorgaben().size()) ? frage.vorgaben().get(stufe) : null;
			boolean fehlt = vorgabe == null || vorgabe.isBlank();
			ListItem eintrag = new ListItem("Stufe " + stufe + " – " + stufen.get(stufe) + ": "
					+ (fehlt ? "Vorgabe fehlt" : vorgabe));
			modell.add(eintrag);
		}
		Div box = new Div(new H4("Frage " + nummer), new Paragraph(frage.text()), modell);
		box.setId("frage-" + nummer);
		box.setWidthFull();
		return box;
	}

	private HorizontalLayout aktionen() {
		// UC-007 A3: Abbruch vor der Anforderung; der Entwurf wird nicht freigegeben.
		Button abbrechen = new Button("Abbrechen", (event) -> zeigeUebersicht());
		abbrechen.addThemeVariants(ButtonVariant.TERTIARY);
		abbrechen.setId("pruefung-abbrechen");
		Button anfordern = new Button("Freigabe anfordern", (event) -> freigabeAnfordern());
		anfordern.addThemeVariants(ButtonVariant.PRIMARY);
		anfordern.setId("freigabe-anfordern");
		HorizontalLayout leiste = new HorizontalLayout(abbrechen, anfordern);
		leiste.setWidthFull();
		leiste.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		return leiste;
	}

	/** UC-007 steps 5–6 und A2: prüft die Vollständigkeit und zeigt die Zusammenfassung. */
	private void freigabeAnfordern() {
		FreigabeKandidat kandidat = this.gewaehlt;
		if (kandidat == null) {
			return;
		}
		this.meldung = null;
		try {
			zusammenfassung(this.service.freigabePruefen(kandidat.rolleId()));
		}
		catch (EntwurfUnvollstaendigException ex) {
			zeigeUnvollstaendig(ex);
		}
		catch (KeinEntwurfZurFreigabeException | NotFoundException ex) {
			fehler(ex.getMessage());
			zeigeUebersicht();
		}
	}

	/** UC-007 A2: Hinweis auf Handlungsfelder ohne Frage und Fragen mit unvollständigem Entscheidungsmodell. */
	private void zeigeUnvollstaendig(EntwurfUnvollstaendigException ex) {
		Div box = new Div(hinweisTitel(UNVOLLSTAENDIG_TITEL));
		if (!ex.handlungsfelderOhneFrage().isEmpty()) {
			Span felder = new Span("Handlungsfelder ohne Frage: " + String.join(", ", ex.handlungsfelderOhneFrage()));
			felder.setId("handlungsfelder-ohne-frage");
			box.add(felder);
		}
		if (!ex.fragenMitUnvollstaendigemModell().isEmpty()) {
			Span fragen = new Span("Fragen mit unvollständigem Entscheidungsmodell: " + ex.fragenMitUnvollstaendigemModell()
				.stream()
				.map((n) -> "Frage " + n)
				.collect(Collectors.joining(", ")));
			fragen.setId("fragen-unvollstaendig");
			box.add(fragen);
		}
		box.addClassName("devace-hinweis");
		box.getElement().setAttribute("theme", "fehler");
		box.setId("entwurf-unvollstaendig");
		this.meldung = box;
		zeigeEntwurf();
	}

	// ---------------------------------------------------------------- Bestätigung (Schritte 6–9, A3)

	/** UC-007 step 6: Rolle, Version und Zahl der Fragen je Handlungsfeld; step 7 bestätigt, A3 bricht ab. */
	private void zusammenfassung(FreigabeZusammenfassung zusammenfassung) {
		Dialog dialog = new Dialog();
		dialog.setId("freigabe-dialog");
		dialog.setHeaderTitle("Fragenkatalog freigeben");
		dialog.setWidth("32rem");

		UnorderedList jeFeld = new UnorderedList();
		zusammenfassung.fragen()
			.forEach((f) -> jeFeld.add(new ListItem(f.handlungsfeld() + ": " + f.anzahl() + " Fragen")));
		Paragraph kopf = new Paragraph("Rolle " + zusammenfassung.rolle() + " · Version " + zusammenfassung.version());
		kopf.setId("freigabe-zusammenfassung");
		dialog.add(new VerticalLayout(kopf, jeFeld,
				fussnote("Die bisher freigegebene Version der Rolle wird archiviert.")));

		// UC-007 A3: Abbruch bei der Zusammenfassung; der Entwurf wird nicht freigegeben.
		Button abbrechen = new Button("Abbrechen", (event) -> dialog.close());
		abbrechen.setId("freigabe-abbrechen");
		Button freigeben = new Button("Freigeben", (event) -> {
			dialog.close();
			freigeben(zusammenfassung);
		});
		freigeben.addThemeVariants(ButtonVariant.PRIMARY);
		freigeben.setId("freigabe-bestaetigen");
		dialog.getFooter().add(abbrechen, freigeben);
		dialog.open();
	}

	/** UC-007 steps 8–9: gibt den Entwurf frei und bestätigt die Freigabe. */
	private void freigeben(FreigabeZusammenfassung zusammenfassung) {
		try {
			this.service.freigeben(zusammenfassung.rolleId(), zusammenfassung.version());
		}
		catch (EntwurfUnvollstaendigException ex) {
			zeigeUnvollstaendig(ex);
			return;
		}
		catch (KeinEntwurfZurFreigabeException | NotFoundException ex) {
			fehler(ex.getMessage());
			zeigeUebersicht();
			return;
		}
		Notification notification = Notification.show(FREIGEGEBEN);
		notification.addThemeVariants(NotificationVariant.SUCCESS);
		zeigeUebersicht();
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
