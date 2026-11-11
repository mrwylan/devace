package info.wylan.devace.ui;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.streams.DownloadEvent;
import info.wylan.devace.persistence.Starthinweis;
import info.wylan.devace.persistence.Verlauf;
import info.wylan.devace.service.AntwortUngueltigException;
import info.wylan.devace.service.AssessmentService;
import info.wylan.devace.service.AssessmentService.Auswertung;
import info.wylan.devace.service.AssessmentService.Dialog;
import info.wylan.devace.service.AssessmentService.Einstieg;
import info.wylan.devace.service.AssessmentService.FeldScore;
import info.wylan.devace.service.EinladungNichtNutzbarException;
import info.wylan.devace.service.KiModellClient.Abschnitt;
import info.wylan.devace.service.TextreportNichtErstelltException;

/**
 * UC-002 Assessment durchführen und UC-003 Assessment abschließen und Textreport herunterladen. Der Teilnehmer meldet
 * sich nicht an; die Referenz seiner Einladung steht in der
 * Adresse.
 */
@Route("assessment")
@PageTitle("Kompetenz-Assessment")
// UC-002: Der Teilnehmer nutzt nur seine Einladung, keine Anmeldung (OQ-003).
@AnonymousAllowed
public class AssessmentView extends Div implements HasUrlParameter<String> {

	private final AssessmentService service;

	private final Clock clock;

	/** UC-003 BR-006: Der Textreport steht einmal zum Download bereit. */
	private boolean textreportHeruntergeladen;

	private final Div inhalt = new Div();

	private String referenz = "";

	/** Die Rolle der Einladung, für die Kopfzeilen von UC-003. */
	private String rolle = "";

	public AssessmentView(AssessmentService service, Clock clock) {
		this.service = service;
		this.clock = clock;
		Div kopf = new Div(new Span("devace · Kompetenz-Assessment"));
		kopf.addClassName("devace-kopf");
		this.inhalt.addClassName("devace-assessment");
		add(kopf, this.inhalt);
	}

	@Override
	public void setParameter(BeforeEvent event, String parameter) {
		this.referenz = (parameter != null) ? parameter : "";
		// UC-002 step 1–2: A1, A2 oder der Starthinweis
		Einstieg einstieg = this.service.einstieg(this.referenz);
		switch (einstieg) {
			case Einstieg.NichtNutzbar nichtNutzbar -> zeigeNichtNutzbar();
			case Einstieg.StarthinweisNoetig noetig -> zeigeStarthinweis(noetig.hinweis(), noetig.rolle());
			case Einstieg.Fortsetzen fortsetzen -> zeigeDialog(fortsetzen.dialog());
		}
	}

	/** UC-002 A1: Die Einladung ist unbekannt, eingelöst oder abgelaufen. */
	private void zeigeNichtNutzbar() {
		setzeInhalt(meldung(EinladungNichtNutzbarException.TITEL,
				new EinladungNichtNutzbarException().getMessage(), "warnung", "assessment-nicht-nutzbar"));
	}

	/** UC-002 steps 2–3: der Starthinweis mit der Bestätigung; A3 ohne Bestätigung. */
	private void zeigeStarthinweis(Starthinweis hinweis, String rolle) {
		H3 titel = new H3("Starthinweis");
		Paragraph untertitel = new Paragraph("Bitte lesen Sie den Hinweis, bevor Sie beginnen.");
		untertitel.addClassName("devace-untertitel");
		Div karte = new Div(titel, untertitel, abschnitt("Eingesetzte KI-Modelle", hinweis.kiModelle()),
				abschnitt("Bitte nicht erfassen und Firmenweisungen", hinweis.text()));
		karte.addClassName("devace-karte");
		karte.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "0.75rem");
		karte.add(meldung("Ihre Antworten bleiben anonym",
				"Das KI-Modell erhält weder Ihre Einladung noch Ihre E-Mail-Adresse, nur die Frage und den Text Ihrer Antwort.",
				null, null));
		// UC-002 BR-002: Das Assessment beginnt erst nach der Bestätigung des Starthinweises.
		Checkbox bestaetigung = new Checkbox("Ich habe den Starthinweis gelesen und bestätige ihn.");
		bestaetigung.setId("assessment-bestaetigung");
		Button nichtStarten = new Button("Nicht starten", (event) -> this.inhalt.removeAll());
		nichtStarten.addThemeVariants(ButtonVariant.TERTIARY);
		nichtStarten.setId("assessment-nicht-starten");
		Button starten = new Button("Assessment starten", (event) -> starten(bestaetigung.getValue()));
		starten.addThemeVariants(ButtonVariant.PRIMARY);
		starten.setId("assessment-starten");
		starten.setEnabled(false);
		bestaetigung.addValueChangeListener((event) -> starten.setEnabled(event.getValue()));
		HorizontalLayout aktionen = new HorizontalLayout(nichtStarten, starten);
		aktionen.setWidthFull();
		aktionen.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.END);
		karte.add(bestaetigung, aktionen);
		setzeInhalt(kopfzeile("Willkommen zu Ihrem Assessment", rolle), karte);
	}

	/** UC-002 steps 3–4: startet das Assessment und stellt die erste Frage. */
	private void starten(boolean bestaetigt) {
		try {
			zeigeDialog(this.service.starten(this.referenz, bestaetigt));
		}
		catch (EinladungNichtNutzbarException ex) {
			zeigeNichtNutzbar();
		}
	}

	/** UC-002 steps 4–8: der Dialog mit der nächsten Frage oder die Meldung, dass alle beantwortet sind. */
	private void zeigeDialog(Dialog dialog) {
		this.rolle = dialog.rolle();
		int beantwortet = dialog.verlauf().size();
		ProgressBar fortschritt = new ProgressBar(0, Math.max(dialog.anzahl(), 1), beantwortet);
		fortschritt.setId("assessment-fortschritt");
		String stand = dialog.alleBeantwortet() ? "Frage " + dialog.anzahl() + " von " + dialog.anzahl()
				: "Frage " + dialog.nummer() + " von " + dialog.anzahl();
		if (dialog.alleBeantwortet()) {
			Button abschliessen = new Button("Assessment abschließen");
			abschliessen.addThemeVariants(ButtonVariant.PRIMARY);
			abschliessen.setId("assessment-abschliessen");
			// UC-003 step 1: Der Teilnehmer schließt sein Assessment ab.
			abschliessen.setDisableOnClick(true);
			abschliessen.addClickListener((event) -> abschliessen());
			setzeInhalt(kopfzeile(stand, dialog.rolle()), fortschritt,
					meldung("Alle Fragen beantwortet", "Sie haben alle " + dialog.anzahl()
							+ " Fragen beantwortet. Schließen Sie das Assessment ab, um Ihren Textreport zu erhalten.",
							"erfolg", "assessment-fertig"),
					abschliessen);
			return;
		}
		H3 titel = new H3("Dialog");
		Paragraph untertitel = new Paragraph(
				"Beantworten Sie jede Frage in eigenen Worten. Eine abgegebene Antwort lässt sich nicht ändern.");
		untertitel.addClassName("devace-untertitel");
		Div karte = new Div(titel, untertitel);
		karte.addClassName("devace-karte");
		// UC-002 BR-008: Der Verlauf zeigt die zugeordnete Stufe nicht.
		for (Verlauf eintrag : dialog.verlauf()) {
			karte.add(eintrag("Q", "Frage " + eintrag.position(), eintrag.frage()),
					eintrag("S", "Ihre Antwort", eintrag.antwort()));
		}
		karte.add(eintrag("Q", "Frage " + dialog.nummer(), dialog.naechsteFrage()));
		TextArea antwort = new TextArea();
		antwort.setId("assessment-antwort");
		antwort.setPlaceholder("Ihre Antwort in eigenen Worten …");
		// NFR-010: zugänglicher Name des Antwortfelds (WCAG 2.1 AA), das Layout bleibt wie im Mockup ohne sichtbares Label
		antwort.setAriaLabel("Ihre Antwort");
		antwort.setWidthFull();
		antwort.setMinHeight("6rem");
		antwort.getStyle().set("margin-top", "0.75rem");
		Button senden = new Button("Senden");
		senden.addThemeVariants(ButtonVariant.PRIMARY);
		senden.setId("assessment-senden");
		senden.setDisableOnClick(true);
		senden.addClickListener((event) -> {
			try {
				// UC-002 steps 5–7
				zeigeDialog(this.service.antworten(this.referenz, antwort.getValue()));
			}
			catch (AntwortUngueltigException ex) {
				// UC-002 A4: Hinweis an der Antwort, weiter bei Schritt 5
				antwort.setInvalid(true);
				antwort.setErrorMessage(ex.getMessage());
				senden.setEnabled(true);
			}
			catch (EinladungNichtNutzbarException ex) {
				zeigeNichtNutzbar();
			}
		});
		Paragraph fussnote = new Paragraph("Höchstens 500 Zeichen. Sie können das Assessment jederzeit verlassen; "
				+ "bereits gegebene Antworten bleiben 7 Tage erhalten.");
		fussnote.addClassName("devace-fussnote");
		HorizontalLayout aktion = new HorizontalLayout(senden);
		aktion.setWidthFull();
		aktion.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.END);
		karte.add(antwort, aktion, fussnote);
		setzeInhalt(kopfzeile(stand, dialog.rolle()), fortschritt, karte);
	}

	/** UC-003 steps 1–5; A1 wenn kein Textreport entsteht. */
	private void abschliessen() {
		try {
			zeigeAuswertung(this.service.abschliessen(this.referenz));
		}
		catch (TextreportNichtErstelltException ex) {
			zeigeTextreportNichtErstellt(ex);
		}
		catch (EinladungNichtNutzbarException ex) {
			zeigeNichtNutzbar();
		}
	}

	/** UC-003 A1: Hinweis, nichts gespeichert, der Abschluss lässt sich erneut anfordern. */
	private void zeigeTextreportNichtErstellt(TextreportNichtErstelltException ex) {
		Button erneut = new Button("Abschluss erneut anfordern", (event) -> abschliessen());
		erneut.addThemeVariants(ButtonVariant.PRIMARY);
		erneut.setId("assessment-abschluss-erneut");
		erneut.setDisableOnClick(true);
		setzeInhalt(titelzeile("Assessment abschließen", rolle()),
				meldung(TextreportNichtErstelltException.TITEL, ex.getMessage(), "fehler", "textreport-nicht-erstellt"),
				erneut);
	}

	/** UC-003 A3: Der Textreport wurde bereits heruntergeladen oder ist verfallen. */
	private void zeigeTextreportNichtVerfuegbar(String rolle) {
		setzeInhalt(titelzeile("Assessment abschließen", rolle),
				meldung("Textreport nicht mehr verfügbar",
						"Der Textreport wurde bereits heruntergeladen oder ist verfallen. Ihre Scores bleiben ohne Personenbezug gespeichert.",
						"warnung", "textreport-nicht-verfuegbar"));
	}

	/**
	 * UC-003 steps 5–7: die Auswertung mit Radar-Chart, Scores und Textreport; sie wird nicht gespeichert (BR-005) und
	 * steht nur in dieser Ansicht zur Verfügung.
	 */
	private void zeigeAuswertung(Auswertung auswertung) {
		this.textreportHeruntergeladen = false;
		Div fertig = meldung("Assessment abgeschlossen",
				"Ihre Auswertung ist fertig. Radar-Chart, Scores und Textreport sehen nur Sie, und nur jetzt.", "erfolg",
				"assessment-abgeschlossen");

		H3 chartTitel = new H3("Radar-Chart und Scores");
		Paragraph chartText = new Paragraph(
				"Ihre Scores je Handlungsfeld auf der Skala 0 bis 5: Summe der Stufen geteilt durch die Zahl der Fragen.");
		chartText.addClassName("devace-untertitel");
		Div scores = new Div();
		scores.addClassName("devace-score-liste");
		scores.setId("auswertung-scores");
		for (FeldScore score : auswertung.scores()) {
			Span name = new Span(score.name());
			Span wert = new Span((score.score() != null) ? format(score.score()) : "–");
			wert.addClassName("devace-score-wert");
			scores.add(new Div(name, wert));
		}
		Paragraph ohneFrage = new Paragraph("Handlungsfelder ohne Frage haben keinen Score.");
		ohneFrage.addClassName("devace-fussnote");
		Div diagramm = new Div(new Html(RadarChart.svg(auswertung.scores())), new Div(scores, ohneFrage));
		diagramm.addClassName("devace-radar");
		Div chartKarte = new Div(chartTitel, chartText, diagramm);
		chartKarte.addClassName("devace-karte");

		H3 reportTitel = new H3("Textreport");
		Paragraph reportText = new Paragraph(
				"Vom KI-Modell erstellt: Er würdigt Ihre Leistung, begründet Ihre Einstufung und zeigt, wo Sie als Nächstes investieren.");
		reportText.addClassName("devace-untertitel");
		Paragraph ueberblick = new Paragraph(auswertung.textreport().ueberblick());
		ueberblick.setId("textreport-ueberblick");
		Div abschnitte = new Div();
		abschnitte.addClassName("devace-auswertung-felder");
		for (Abschnitt abschnitt : auswertung.textreport().abschnitte()) {
			abschnitte.add(abschnitt(abschnitt, auswertung.scores()));
		}
		// UC-003 BR-003: Scores und Textreport sind kein Bestandteil der Leistungsbeurteilung.
		Paragraph keineBeurteilung = new Paragraph(
				"Das sind Vorschläge zur Weiterentwicklung. Scores und Textreport sind kein Bestandteil der Leistungsbeurteilung.");
		keineBeurteilung.addClassName("devace-fussnote");
		Div reportKarte = new Div(reportTitel, reportText, ueberblick, abschnitte, keineBeurteilung);
		reportKarte.addClassName("devace-karte");

		// UC-003 steps 6–7, BR-006: einmaliger Download; danach oder nach dem Verfall gilt A3
		Button herunterladen = new Button("Textreport herunterladen");
		herunterladen.addThemeVariants(ButtonVariant.PRIMARY);
		Anchor download = new Anchor((DownloadEvent event) -> herunterladen(event, auswertung), "");
		download.setId("textreport-herunterladen");
		download.add(herunterladen);
		Paragraph hinweis = new Paragraph("Der Textreport steht einmal zum Download bereit. Ihre Antworten sind bereits "
				+ "gelöscht, Ihre Scores bleiben ohne Personenbezug erhalten.");
		hinweis.addClassName("devace-fussnote");

		setzeInhalt(titelzeile("Ihre Auswertung", auswertung.rolle()), fertig, chartKarte, reportKarte, download,
				hinweis);
	}

	private void herunterladen(DownloadEvent event, Auswertung auswertung) {
		boolean verfuegbar;
		synchronized (this) {
			// UC-003 BR-006: nach dem Download und nach dem Verfall steht der Textreport nicht mehr zur Verfügung
			verfuegbar = !this.textreportHeruntergeladen && !auswertung.verfallen(LocalDateTime.now(this.clock));
			this.textreportHeruntergeladen = true;
		}
		if (!verfuegbar) {
			event.getResponse().setStatus(410);
			event.getUI().access(() -> zeigeTextreportNichtVerfuegbar(auswertung.rolle()));
			return;
		}
		byte[] pdf = TextreportPdf.erstellen(auswertung);
		event.setFileName("devace-textreport.pdf");
		event.setContentType("application/pdf");
		event.setContentLength(pdf.length);
		try (OutputStream ausgabe = event.getOutputStream()) {
			ausgabe.write(pdf);
		}
		catch (java.io.IOException ex) {
			throw new java.io.UncheckedIOException(ex);
		}
	}

	private static Div abschnitt(Abschnitt abschnitt, List<FeldScore> scores) {
		String wert = scores.stream()
			.filter((s) -> s.name().equals(abschnitt.handlungsfeld()) && s.score() != null)
			.map((s) -> format(s.score()))
			.findFirst()
			.orElse("");
		Span titel = new Span(abschnitt.handlungsfeld());
		titel.getStyle().set("font-weight", "600");
		Span score = new Span(wert);
		score.addClassName("devace-score-wert");
		Div kopf = new Div(titel, score);
		kopf.addClassName("devace-assessment-zeile");
		Paragraph text = new Paragraph(abschnitt.wuerdigung());
		text.addClassName("devace-untertitel");
		Div karte = new Div(kopf, text);
		if (!abschnitt.naechsterSchritt().isBlank()) {
			Paragraph schritt = new Paragraph("Nächster Schritt: " + abschnitt.naechsterSchritt());
			schritt.addClassName("devace-naechster-schritt");
			karte.add(schritt);
		}
		karte.addClassName("devace-auswertung-feld");
		return karte;
	}

	/** UC-003 BR-001: Der Score wird immer mit einer Nachkommastelle ausgegeben. */
	static String format(BigDecimal score) {
		return String.format(Sprache.LOCALE, "%.1f", score);
	}

	private String rolle() {
		return this.rolle;
	}

	private static Div titelzeile(String titel, String rolle) {
		Span text = new Span(titel);
		text.addClassName("devace-seitentitel");
		Span badge = new Span("Rolle: " + rolle);
		badge.addClassName("devace-badge");
		badge.getElement().setAttribute("theme", "neutral");
		Div zeile = new Div(text, badge);
		zeile.addClassName("devace-assessment-titel");
		return zeile;
	}

	private static Div kopfzeile(String text, String rolle) {
		Span badge = new Span("Rolle: " + rolle);
		badge.addClassName("devace-badge");
		badge.getElement().setAttribute("theme", "neutral");
		Div zeile = new Div(new Span(text), badge);
		zeile.addClassName("devace-assessment-zeile");
		return zeile;
	}

	private static Div abschnitt(String titel, String text) {
		Span kopf = new Span(titel);
		kopf.getStyle().set("font-weight", "600");
		Span inhalt = new Span(text);
		inhalt.addClassName("devace-untertitel");
		Div abschnitt = new Div(kopf, inhalt);
		abschnitt.getStyle().set("display", "flex").set("flex-direction", "column");
		return abschnitt;
	}

	private static Div eintrag(String marke, String kopf, String text) {
		Span kreis = new Span(marke);
		kreis.addClassName("devace-dialog-marke");
		Div eintrag = new Div(kreis, new Div(new Span(kopf), new Span(text)));
		eintrag.addClassName("devace-dialog-eintrag");
		return eintrag;
	}

	private static Div meldung(String titel, String text, String thema, String id) {
		Span kopf = new Span(titel);
		kopf.addClassName("devace-hinweis-titel");
		Div box = new Div(kopf, new Span(text));
		box.addClassName("devace-hinweis");
		if (thema != null) {
			box.getElement().setAttribute("theme", thema);
		}
		if (id != null) {
			box.setId(id);
		}
		return box;
	}

	private void setzeInhalt(Component... teile) {
		this.inhalt.removeAll();
		this.inhalt.add(teile);
	}

}
