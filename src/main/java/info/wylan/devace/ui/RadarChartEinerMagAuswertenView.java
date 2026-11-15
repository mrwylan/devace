package info.wylan.devace.ui;

import java.util.List;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import info.wylan.devace.persistence.MagListItem;
import info.wylan.devace.persistence.RolleOption;
import info.wylan.devace.service.AssessmentService.FeldScore;
import info.wylan.devace.service.AuswertungService;
import jakarta.annotation.security.RolesAllowed;

/** UC-004 Radar-Chart einer MAG auswerten. */
@Route(value = "auswertung", layout = MainLayout.class)
@PageTitle("Radar-Chart einer MAG auswerten")
// UC-004 BR-007: Nur die angemeldete Teamleitung wertet Radar-Charts aus.
@RolesAllowed("TEAMLEITUNG")
public class RadarChartEinerMagAuswertenView extends VerticalLayout implements BeforeEnterObserver {

	/** UC-004 A1: der Hinweis aus OQ-078. */
	static final String KEINE_SCORES = "Zu Ihren MAGs liegen noch keine Scores vor.";

	private final AuswertungService service;

	private final Select<MagListItem> mag = new Select<>();

	private final Select<RolleOption> rolle = new Select<>();

	private final Div chart = new Div();

	public RadarChartEinerMagAuswertenView(AuswertungService service) {
		this.service = service;
		setWidthFull();
		this.mag.setLabel("MAG (Scrum Team · Linienteam)");
		this.mag.setId("auswertung-mag");
		this.mag.setItemLabelGenerator((eintrag) -> eintrag.scrumTeam() + " · " + eintrag.linienteam());
		this.mag.setEmptySelectionAllowed(false);
		this.mag.setWidth("50%");
		this.rolle.setLabel("Rolle");
		this.rolle.setId("auswertung-rolle");
		this.rolle.setItemLabelGenerator(RolleOption::name);
		this.rolle.setEmptySelectionAllowed(false);
		this.rolle.setWidth("50%");
		this.chart.setWidthFull();
		// UC-004 step 3 und A2: die Wahl einer anderen MAG zeigt ihre Rollen, die Wahl einer Rolle das Radar-Chart
		this.mag.addValueChangeListener((event) -> {
			if (event.getValue() != null) {
				zeigeRollen(event.getValue());
			}
		});
		this.rolle.addValueChangeListener((event) -> zeigeRadarChart());
	}

	/** UC-004 steps 1–2: Bei jedem Öffnen zeigt die Ansicht die aktuellen MAGs mit Scores; A1 ohne Scores. */
	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		removeAll();
		H2 titel = new H2("Auswertung");
		titel.addClassName("devace-seitentitel");
		List<MagListItem> mags = this.service.mags();
		if (mags.isEmpty()) {
			// UC-004 A1: Zu keiner MAG der Teamleitung liegt ein Score vor; der Use Case endet.
			Div hinweis = new Div(new Span(KEINE_SCORES));
			hinweis.addClassName("devace-hinweis");
			hinweis.setId("auswertung-keine-scores");
			add(titel, hinweis);
			return;
		}
		HorizontalLayout auswahl = new HorizontalLayout(this.mag, this.rolle);
		auswahl.setWidthFull();
		add(titel, auswahl, this.chart);
		this.mag.setItems(mags);
		this.mag.setValue(mags.get(0));
	}

	/** UC-004 step 2: die Rollen der gewählten MAG, zu denen Scores vorliegen. */
	private void zeigeRollen(MagListItem gewaehlt) {
		List<RolleOption> rollen = this.service.rollen(gewaehlt.id());
		this.rolle.setItems(rollen);
		if (!rollen.isEmpty()) {
			this.rolle.setValue(rollen.get(0));
		}
	}

	/** UC-004 step 4: das Radar-Chart der MAG und Rolle mit dem Wert je Handlungsfeld. */
	private void zeigeRadarChart() {
		this.chart.removeAll();
		MagListItem gewaehlteMag = this.mag.getValue();
		RolleOption gewaehlteRolle = this.rolle.getValue();
		if (gewaehlteMag == null || gewaehlteRolle == null) {
			return;
		}
		List<FeldScore> werte = this.service.radarChart(gewaehlteMag.id(), gewaehlteRolle.id());
		H3 titel = new H3("Radar-Chart");
		Span untertitel = new Span("Mittelwert der Scores je Handlungsfeld auf der Skala 0 bis 5.");
		untertitel.addClassName("devace-untertitel");
		Div liste = new Div();
		liste.addClassName("devace-score-liste");
		liste.setId("auswertung-werte");
		for (FeldScore wert : werte) {
			// UC-004 BR-005: Je Handlungsfeld nur der Mittelwert, kein einzelner Score.
			// UC-004 BR-006: Ein Handlungsfeld ohne Score zeigt keinen Wert.
			Span name = new Span(wert.name());
			Span zahl = new Span((wert.score() != null) ? AssessmentView.format(wert.score()) : "–");
			zahl.addClassName("devace-score-wert");
			liste.add(new Div(name, zahl));
		}
		Div diagramm = new Div(new Html(RadarChart.svg(werte)), liste);
		diagramm.addClassName("devace-radar");
		Div karte = new Div(titel, untertitel, diagramm);
		karte.addClassName("devace-karte");
		this.chart.add(karte);
	}

}
