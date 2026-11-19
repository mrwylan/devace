package info.wylan.devace.ui;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import info.wylan.devace.persistence.TestfallRepository.PruefbarerKatalog;
import info.wylan.devace.persistence.TestfallRepository.TestFrage;
import info.wylan.devace.persistence.TestfallRepository.Testfall;
import info.wylan.devace.persistence.TestlaufRepository.Testergebnis;
import info.wylan.devace.persistence.TestlaufRepository.Testlauf;
import info.wylan.devace.service.KeinTestfallException;
import info.wylan.devace.service.TestfallService;
import info.wylan.devace.service.TestfallService.TestlaufErgebnis;
import info.wylan.devace.service.TestfallUngueltigException;
import info.wylan.devace.service.TestlaufNichtAbgeschlossenException;
import info.wylan.devace.ui.bausteine.Aktionsleiste;
import info.wylan.devace.ui.bausteine.DevaceListe;
import info.wylan.devace.ui.bausteine.Symbolknopf;
import info.wylan.devace.ui.bausteine.Zeichenzaehler;
import jakarta.annotation.security.RolesAllowed;
import org.jspecify.annotations.Nullable;

/** UC-009 KI-Bewertung mit Testfällen absichern. */
@Route(value = "ki-absicherung", layout = MainLayout.class)
@PageTitle("KI-Bewertung mit Testfällen absichern")
// UC-009 BR-011: Nur der angemeldete Administrator pflegt und führt Testfälle aus.
@RolesAllowed("ADMINISTRATOR")
public class KiBewertungMitTestfaellenAbsichernView extends VerticalLayout implements BeforeEnterObserver {

	/** UC-009 A1: der Hinweis aus OQ-085. */
	static final String KEIN_KATALOG = "Noch kein Fragenkatalog zur Prüfung vorhanden.";

	/** UC-009 step 7, NFR-012: der Wartehinweis aus OQ-090. */
	static final String WARTEN = "Die Testfälle werden ausgeführt.";

	private static final DateTimeFormatter ZEITPUNKT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Sprache.LOCALE);

	private final TestfallService service;

	/** ADR-005: Aufrufe des KI-Modells im Hintergrund, mit Wartehinweis. */
	private final KiWarten kiWarten;

	/** ADR-006: Liste mit Suche und Sortierung; die Auswahl einer Zeile öffnet das Detail darunter. */
	private final DevaceListe<PruefbarerKatalog> katalogListe = new DevaceListe<>("pruefbare-kataloge",
			"Kein prüfbarer Fragenkatalog.");

	private final Grid<PruefbarerKatalog> kataloge = this.katalogListe.getGrid();

	private final Div detail = new Div();

	private @Nullable PruefbarerKatalog gewaehlt;

	/** Während die Übersicht ihre Zeilen erneuert, baut die erneute Auswahl das Detail nicht neu auf. */
	private boolean uebersichtWirdErneuert;

	public KiBewertungMitTestfaellenAbsichernView(TestfallService service, KiWarten kiWarten) {
		this.service = service;
		this.kiWarten = kiWarten;
		setWidthFull();
		this.katalogListe.spalte("Rolle", PruefbarerKatalog::rolle).setAutoWidth(true);
		this.katalogListe.spalte("Version", (k) -> String.valueOf(k.version()))
			.setComparator(Comparator.comparingInt(PruefbarerKatalog::version))
			.setAutoWidth(true);
		this.katalogListe.spalte("Status", PruefbarerKatalog::status).setAutoWidth(true);
		this.katalogListe.spalte("Testfälle", (k) -> String.valueOf(k.testfaelle()))
			.setComparator(Comparator.comparingInt(PruefbarerKatalog::testfaelle))
			.setAutoWidth(true);
		this.katalogListe.spalte("Jüngster Testlauf", KiBewertungMitTestfaellenAbsichernView::letzterLauf)
			.setAutoWidth(true);
		this.kataloge.setHeight("18rem");
		// UC-009 step 3: Der Administrator wählt einen Fragenkatalog.
		this.kataloge.asSingleSelect().addValueChangeListener((event) -> {
			if (this.uebersichtWirdErneuert) {
				return;
			}
			this.gewaehlt = event.getValue();
			zeigeDetail();
		});
		this.detail.setWidthFull();
		this.detail.addClassName("devace-detail");
	}

	/** UC-009 steps 1–2: Bei jedem Öffnen zeigt die Ansicht die prüfbaren Fragenkataloge; A1 ohne Katalog. */
	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		removeAll();
		List<PruefbarerKatalog> pruefbar = this.service.pruefbareKataloge();
		if (pruefbar.isEmpty()) {
			// UC-009 A1: Es gibt keinen prüfbaren Fragenkatalog; der Use Case endet.
			Div hinweis = new Div(new Span(KEIN_KATALOG));
			hinweis.addClassName("devace-hinweis");
			hinweis.setId("ki-absicherung-kein-katalog");
			add(hinweis);
			return;
		}
		this.gewaehlt = null;
		this.katalogListe.setItems(pruefbar);
		this.detail.removeAll();
		Span auswahlHinweis = new Span("Wählen Sie einen Fragenkatalog, um seine Testfälle und Testläufe zu sehen.");
		auswahlHinweis.addClassName("devace-untertitel");
		this.detail.add(auswahlHinweis);
		add(this.katalogListe, this.detail);
	}

	/** UC-009 step 4: die Testfälle je Frage und die bisherigen Testläufe des gewählten Fragenkatalogs. */
	private void zeigeDetail() {
		this.detail.removeAll();
		PruefbarerKatalog katalog = this.gewaehlt;
		if (katalog == null) {
			return;
		}
		H3 titel = new H3(katalog.rolle() + " · Version " + katalog.version() + " (" + katalog.status() + ")");
		Button ausfuehren = new Button("Testfälle ausführen");
		ausfuehren.addThemeVariants(ButtonVariant.PRIMARY);
		ausfuehren.setId("ki-ausfuehren");
		Div ergebnis = new Div();
		ergebnis.setId("ki-ergebnis");
		ergebnis.setWidthFull();
		ausfuehren.addClickListener((event) -> ausfuehren(katalog, ausfuehren, ergebnis));

		VerticalLayout fragen = new VerticalLayout();
		fragen.setPadding(false);
		for (TestFrage frage : this.service.testfaelle(katalog.id())) {
			fragen.add(frageKarte(frage));
		}
		this.detail.add(titel, fragen, ausfuehren, ergebnis, testlaeufe(katalog));
	}

	private Div frageKarte(TestFrage frage) {
		Span kopf = new Span("Frage " + frage.position() + ": " + frage.text());
		kopf.getStyle().set("font-weight", "600");
		Div karte = new Div(kopf);
		karte.addClassName("devace-karte");
		karte.setWidthFull();
		karte.setId("ki-frage-" + frage.frageId());
		for (Testfall testfall : frage.testfaelle()) {
			Span antwort = new Span(testfall.antworttext());
			Span stufe = new Span("Erwartete Stufe: " + testfall.erwarteteStufe());
			stufe.addClassName("devace-untertitel");
			Button bearbeiten = Symbolknopf.erstellen(VaadinIcon.EDIT, "Bearbeiten", "Testfall bearbeiten");
			bearbeiten.addClickListener((event) -> testfallDialog(frage, testfall));
			bearbeiten.setId("testfall-bearbeiten-" + testfall.id());
			Button entfernen = Symbolknopf.erstellen(VaadinIcon.TRASH, "Entfernen", "Testfall entfernen");
			entfernen.addThemeVariants(ButtonVariant.ERROR);
			entfernen.addClickListener((event) -> {
				this.service.testfallEntfernen(testfall.id());
				aktualisiere();
			});
			entfernen.setId("testfall-entfernen-" + testfall.id());
			Div text = new Div(antwort, stufe);
			text.addClassName("devace-testfall-text");
			HorizontalLayout knoepfe = new HorizontalLayout(bearbeiten, entfernen);
			knoepfe.setSpacing(false);
			Div zeile = new Div(text, knoepfe);
			zeile.addClassName("devace-testfall");
			zeile.setId("testfall-" + testfall.id());
			karte.add(zeile);
		}
		Button hinzufuegen = new Button("Testfall hinzufügen", (event) -> testfallDialog(frage, null));
		hinzufuegen.setId("testfall-hinzufuegen-" + frage.frageId());
		karte.add(new Aktionsleiste().links(hinzufuegen));
		return karte;
	}

	/** UC-009 step 5 und A2: Testfall anlegen oder ändern; ungültige Angaben werden am Feld gemeldet. */
	private void testfallDialog(TestFrage frage, @Nullable Testfall testfall) {
		Dialog dialog = new Dialog();
		dialog.setId("testfall-dialog");
		dialog.setHeaderTitle((testfall == null) ? "Testfall hinzufügen" : "Testfall bearbeiten");
		dialog.setWidth("36rem");
		Span frageText = new Span("Frage " + frage.position() + ": " + frage.text());
		frageText.addClassName("devace-untertitel");
		TextArea antwort = new TextArea("Vorgegebene Antwort");
		antwort.setId("testfall-antwort");
		antwort.setWidthFull();
		antwort.setHelperText("Pflichtangabe");
		Select<Integer> stufe = new Select<>();
		stufe.setLabel("Erwartete Stufe");
		stufe.setId("testfall-stufe");
		stufe.setItems(0, 1, 2, 3, 4, 5);
		stufe.setItemLabelGenerator(
				(wert) -> wert + " – " + FragenkatalogUndEntscheidungsmodellErstellenView.STUFEN.get(wert));
		stufe.setWidthFull();
		if (testfall != null) {
			antwort.setValue(testfall.antworttext());
			stufe.setValue(testfall.erwarteteStufe());
		}
		dialog.add(new VerticalLayout(frageText, antwort, stufe));
		Button abbrechen = new Button("Abbrechen", (event) -> dialog.close());
		abbrechen.setId("testfall-abbrechen");
		Button speichern = new Button("Speichern", (event) -> {
			antwort.setInvalid(false);
			stufe.setInvalid(false);
			try {
				if (testfall == null) {
					this.service.testfallAnlegen(frage.frageId(), antwort.getValue(), stufe.getValue());
				}
				else {
					this.service.testfallAendern(testfall.id(), antwort.getValue(), stufe.getValue());
				}
				dialog.close();
				aktualisiere();
			}
			catch (TestfallUngueltigException ex) {
				// UC-009 A2: Hinweis am Feld, nichts gespeichert, weiter bei Schritt 5
				String fehlerAntwort = ex.fehler().get(TestfallUngueltigException.ANTWORT);
				if (fehlerAntwort != null) {
					antwort.setInvalid(true);
					antwort.setErrorMessage(fehlerAntwort);
				}
				String fehlerStufe = ex.fehler().get(TestfallUngueltigException.STUFE);
				if (fehlerStufe != null) {
					stufe.setInvalid(true);
					stufe.setErrorMessage(fehlerStufe);
				}
			}
		});
		speichern.addThemeVariants(ButtonVariant.PRIMARY);
		speichern.setId("testfall-speichern");
		dialog.getFooter().add(abbrechen, speichern);
		// UC-009 BR-001, A2, ADR-006: Zähler an der Antwort; über 500 Zeichen der Hinweis am Feld und Speichern gesperrt
		Zeichenzaehler.sperrt(speichern,
				Zeichenzaehler.mitMeldung(antwort, 500, (zahl) -> "Höchstens 500 Zeichen."));
		dialog.open();
	}

	/**
	 * UC-009 steps 6–9: führt die Testfälle aus und zeigt Trefferquote und Abweichungen; A3 ohne Testfall, A4 ohne
	 * Stufe. Bis das Ergebnis vorliegt, zeigt die Ansicht den Wartehinweis (NFR-012, ADR-005).
	 */
	private void ausfuehren(PruefbarerKatalog katalog, Button ausloeser, Div ergebnis) {
		ausloeser.setEnabled(false);
		ergebnis.removeAll();
		ergebnis.add(KiWarten.wartehinweis(WARTEN, "ki-wartehinweis"));
		this.kiWarten.ausfuehren(() -> this.service.ausfuehren(katalog.id()), (lauf) -> {
			ausloeser.setEnabled(true);
			aktualisiereUebersicht();
			ergebnis.removeAll();
			ergebnis.add(laufErgebnis(lauf));
			ersetzeTestlaeufe(katalog);
		}, (fehler) -> {
			ausloeser.setEnabled(true);
			ergebnis.removeAll();
			if (fehler instanceof KeinTestfallException || fehler instanceof TestlaufNichtAbgeschlossenException) {
				// UC-009 A3 (weiter bei Schritt 5) und A4 (kein Testlauf gespeichert, weiter bei Schritt 6)
				Div hinweis = new Div(new Span(fehler.getMessage()));
				hinweis.addClassName("devace-hinweis");
				hinweis.getElement().setAttribute("theme", "fehler");
				hinweis.setId("ki-ausfuehren-hinweis");
				ergebnis.add(hinweis);
			}
			else {
				throw fehler;
			}
		});
	}

	/** UC-009 step 9: Trefferquote, Bestanden oder nicht und die Abweichungen. */
	private static Div laufErgebnis(TestlaufErgebnis lauf) {
		// UC-009 BR-006: Unter 90 Prozent ist ein Testlauf nicht bestanden; die Abweichungen werden aufgeführt.
		Div box = new Div(new Span("Trefferquote " + prozent(lauf.trefferquote()) + " · " + urteil(lauf.trefferquote())));
		box.addClassName("devace-hinweis");
		box.getElement().setAttribute("theme", lauf.bestanden() ? "erfolg" : "fehler");
		box.setId("ki-trefferquote");
		Div wrapper = new Div(box, abweichungen(lauf.abweichungen(), "ki-abweichungen"));
		wrapper.setWidthFull();
		return wrapper;
	}

	private static Grid<Testergebnis> abweichungen(List<Testergebnis> abweichungen, String id) {
		Grid<Testergebnis> grid = new Grid<>();
		grid.setId(id);
		grid.addColumn(Testergebnis::frage).setHeader("Frage").setFlexGrow(2);
		grid.addColumn(Testergebnis::antworttext).setHeader("Vorgegebene Antwort").setFlexGrow(2);
		grid.addColumn(Testergebnis::erwarteteStufe).setHeader("Erwartet").setAutoWidth(true);
		grid.addColumn(Testergebnis::ermittelteStufe).setHeader("Ermittelt").setAutoWidth(true);
		grid.setItems(abweichungen);
		grid.setAllRowsVisible(true);
		return grid;
	}

	/** UC-009 step 4 und BR-007: die bisherigen Testläufe; ein gewählter Testlauf zeigt seine Abweichungen. */
	private Div testlaeufe(PruefbarerKatalog katalog) {
		H3 titel = new H3("Testläufe");
		Grid<Testlauf> grid = new Grid<>();
		grid.setId("testlaeufe");
		grid.addColumn((lauf) -> ZEITPUNKT.format(lauf.ausgefuehrtAm())).setHeader("Zeitpunkt").setAutoWidth(true);
		grid.addColumn(Testlauf::administrator).setHeader("Administrator").setAutoWidth(true);
		grid.addColumn(Testlauf::kiModell).setHeader("KI-Modell").setAutoWidth(true);
		grid.addColumn((lauf) -> prozent(lauf.trefferquote())).setHeader("Trefferquote").setAutoWidth(true);
		grid.addColumn((lauf) -> urteil(lauf.trefferquote())).setHeader("Ergebnis").setAutoWidth(true);
		grid.setItems(this.service.testlaeufe(katalog.id()));
		grid.setAllRowsVisible(true);
		Div frueher = new Div();
		frueher.setId("testlauf-abweichungen");
		grid.asSingleSelect().addValueChangeListener((event) -> {
			frueher.removeAll();
			if (event.getValue() != null) {
				List<Testergebnis> abweichungen = this.service.ergebnisse(event.getValue().id())
					.stream()
					.filter(Testergebnis::abweichung)
					.toList();
				frueher.add(abweichungen(abweichungen, "testlauf-abweichungen-grid"));
			}
		});
		Div bereich = new Div(titel, grid, frueher);
		bereich.setId("testlaeufe-bereich");
		bereich.setWidthFull();
		return bereich;
	}

	private void ersetzeTestlaeufe(PruefbarerKatalog katalog) {
		this.detail.getChildren()
			.filter((teil) -> teil.getId().filter("testlaeufe-bereich"::equals).isPresent())
			.findFirst()
			.ifPresent((alt) -> this.detail.replace(alt, testlaeufe(katalog)));
	}

	/** Nach einer Änderung der Testfälle: Übersicht und Detail mit dem aktuellen Stand. */
	private void aktualisiere() {
		aktualisiereUebersicht();
		zeigeDetail();
	}

	/**
	 * UC-009 step 2: die Übersicht mit der aktuellen Zahl der Testfälle und dem jüngsten Testlauf. Die Auswahl bleibt
	 * stehen, ohne das Detail neu aufzubauen, damit das Ergebnis eines Testlaufs sichtbar bleibt (Schritt 9).
	 */
	private void aktualisiereUebersicht() {
		PruefbarerKatalog vorher = this.gewaehlt;
		List<PruefbarerKatalog> pruefbar = this.service.pruefbareKataloge();
		this.uebersichtWirdErneuert = true;
		try {
			this.katalogListe.setItems(pruefbar);
			if (vorher != null) {
				pruefbar.stream().filter((k) -> k.id() == vorher.id()).findFirst().ifPresent((k) -> {
					this.gewaehlt = k;
					this.kataloge.asSingleSelect().setValue(k);
				});
			}
		}
		finally {
			this.uebersichtWirdErneuert = false;
		}
	}

	private static String letzterLauf(PruefbarerKatalog katalog) {
		BigDecimal quote = katalog.letzteTrefferquote();
		return (quote == null) ? "–" : prozent(quote) + " · " + urteil(quote);
	}

	/** UC-009 BR-006, OQ-089: Bestanden ab 90 Prozent, sonst Nicht bestanden. */
	static String urteil(BigDecimal trefferquote) {
		return TestfallService.bestanden(trefferquote) ? "Bestanden" : "Nicht bestanden";
	}

	/** UC-009 BR-005: die Trefferquote mit zwei Nachkommastellen. */
	static String prozent(BigDecimal trefferquote) {
		return String.format(Sprache.LOCALE, "%.2f %%", trefferquote);
	}

}
