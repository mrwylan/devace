package info.wylan.devace.ui;

import java.util.List;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import info.wylan.devace.persistence.RolleOption;
import info.wylan.devace.service.AssessmentService.FeldScore;
import info.wylan.devace.service.SchulungsplanungService;
import info.wylan.devace.service.SchulungsplanungService.MagRadarChart;
import jakarta.annotation.security.RolesAllowed;

/** UC-005 Radar-Charts für die Schulungsplanung auswerten. */
@Route(value = "schulungsplanung", layout = MainLayout.class)
@PageTitle("Radar-Charts für die Schulungsplanung auswerten")
// UC-005 BR-007: Nur die angemeldete Schulungsverantwortliche wertet die Radar-Charts aller MAGs aus.
@RolesAllowed("SCHULUNGSVERANTWORTLICHE")
public class RadarChartsFuerDieSchulungsplanungAuswertenView extends VerticalLayout implements BeforeEnterObserver {

	/** UC-005 A1: der Hinweis aus OQ-080. */
	static final String KEINE_SCORES = "Es liegen noch keine Scores vor.";

	private final SchulungsplanungService service;

	private final Select<RolleOption> rolle = new Select<>();

	private final Div charts = new Div();

	public RadarChartsFuerDieSchulungsplanungAuswertenView(SchulungsplanungService service) {
		this.service = service;
		setWidthFull();
		this.rolle.setLabel("Rolle");
		this.rolle.setId("schulung-rolle");
		this.rolle.setWidth("24rem");
		this.rolle.setItemLabelGenerator(RolleOption::name);
		this.rolle.setEmptySelectionAllowed(false);
		this.rolle.setPlaceholder("Rolle wählen");
		this.charts.addClassName("devace-radar-raster");
		this.charts.setId("schulung-radar-charts");
		this.charts.setWidthFull();
		// UC-005 step 3 und A2: die Wahl einer Rolle zeigt ihre Radar-Charts
		this.rolle.addValueChangeListener((event) -> zeigeRadarCharts());
	}

	/** UC-005 steps 1–2: Bei jedem Öffnen zeigt die Ansicht die aktuellen Rollen mit Scores; A1 ohne Scores. */
	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		removeAll();
		List<RolleOption> rollen = this.service.rollen();
		if (rollen.isEmpty()) {
			// UC-005 A1: Es liegt noch kein Score vor; der Use Case endet.
			Div hinweis = new Div(new Span(KEINE_SCORES));
			hinweis.addClassName("devace-hinweis");
			hinweis.setId("schulung-keine-scores");
			add(hinweis);
			return;
		}
		add(this.rolle, this.charts);
		// UC-005 step 3: Die Schulungsverantwortliche wählt die Rolle selbst; die Ansicht wählt nichts vor.
		this.rolle.setItems(rollen);
		this.rolle.clear();
		this.charts.removeAll();
	}

	/** UC-005 step 4 und A2: die Radar-Charts aller MAGs der gewählten Rolle nebeneinander. */
	private void zeigeRadarCharts() {
		this.charts.removeAll();
		RolleOption gewaehlt = this.rolle.getValue();
		if (gewaehlt == null) {
			return;
		}
		for (MagRadarChart chart : this.service.radarCharts(gewaehlt.id())) {
			this.charts.add(karte(chart));
		}
	}

	private static Div karte(MagRadarChart chart) {
		H3 titel = new H3(chart.mag().scrumTeam() + " · " + chart.mag().linienteam());
		Div liste = new Div();
		liste.addClassName("devace-score-liste");
		for (FeldScore wert : chart.werte()) {
			// UC-005 BR-005: Je Handlungsfeld und MAG nur der Mittelwert, kein einzelner Score.
			// UC-005 BR-006: Ein Handlungsfeld ohne Score zeigt keinen Wert.
			Span name = new Span(wert.name());
			Span zahl = new Span((wert.score() != null) ? AssessmentView.format(wert.score()) : "–");
			zahl.addClassName("devace-score-wert");
			liste.add(new Div(name, zahl));
		}
		Div karte = new Div(titel, new Html(RadarChart.svg(chart.werte())), liste);
		karte.addClassName("devace-karte");
		karte.setId("schulung-mag-" + chart.mag().id());
		return karte;
	}

}
