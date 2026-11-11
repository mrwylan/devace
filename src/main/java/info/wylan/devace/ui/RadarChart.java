package info.wylan.devace.ui;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import info.wylan.devace.service.AssessmentService.FeldScore;

/**
 * UC-003 step 5: das Radar-Chart der Scores als SVG, eine Achse je Handlungsfeld, Skala 0 bis 5 nur in Ganzzahlen
 * (UC-003 BR-001). Ein Handlungsfeld ohne Score hat keinen Punkt.
 */
final class RadarChart {

	private static final double MITTE_X = 200;

	private static final double MITTE_Y = 170;

	private static final double RADIUS = 110;

	private RadarChart() {
	}

	static String svg(List<FeldScore> scores) {
		int achsen = scores.size();
		StringBuilder svg = new StringBuilder(
				"<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 400 340' role='img' aria-label='Radar-Chart der Scores je Handlungsfeld' class='devace-radar-svg'>");
		// UC-003 BR-001: Die Skala zeigt nur die Ganzzahlen 1 bis 5.
		for (int stufe = 1; stufe <= 5; stufe++) {
			svg.append("<polygon class='devace-radar-ring' points='").append(punkte(achsen, stufe / 5.0)).append("'/>");
		}
		for (int i = 0; i < achsen; i++) {
			double[] ende = punkt(i, achsen, 1.0);
			svg.append(String.format(Locale.ROOT, "<line class='devace-radar-achse' x1='%.1f' y1='%.1f' x2='%.1f' y2='%.1f'/>",
					MITTE_X, MITTE_Y, ende[0], ende[1]));
			double[] beschriftung = punkt(i, achsen, 1.22);
			String anker = (Math.abs(beschriftung[0] - MITTE_X) < 5) ? "middle" : (beschriftung[0] > MITTE_X) ? "start" : "end";
			svg.append(String.format(Locale.ROOT, "<text class='devace-radar-text' x='%.1f' y='%.1f' text-anchor='%s'>%s</text>",
					beschriftung[0], beschriftung[1], anker, escape(scores.get(i).name())));
		}
		List<double[]> werte = new ArrayList<>();
		for (int i = 0; i < achsen; i++) {
			BigDecimal score = scores.get(i).score();
			if (score != null) {
				werte.add(punkt(i, achsen, score.doubleValue() / 5.0));
			}
		}
		if (werte.size() > 1) {
			StringBuilder linie = new StringBuilder();
			for (double[] p : werte) {
				linie.append(String.format(Locale.ROOT, "%.1f,%.1f ", p[0], p[1]));
			}
			svg.append("<polygon class='devace-radar-flaeche' points='").append(linie.toString().strip()).append("'/>");
		}
		for (double[] p : werte) {
			svg.append(String.format(Locale.ROOT, "<circle class='devace-radar-punkt' cx='%.1f' cy='%.1f' r='3.5'/>", p[0], p[1]));
		}
		return svg.append("</svg>").toString();
	}

	private static String punkte(int achsen, double anteil) {
		StringBuilder punkte = new StringBuilder();
		for (int i = 0; i < achsen; i++) {
			double[] p = punkt(i, achsen, anteil);
			punkte.append(String.format(Locale.ROOT, "%.1f,%.1f ", p[0], p[1]));
		}
		return punkte.toString().strip();
	}

	private static double[] punkt(int index, int achsen, double anteil) {
		double winkel = -Math.PI / 2 + 2 * Math.PI * index / Math.max(achsen, 1);
		return new double[] { MITTE_X + RADIUS * anteil * Math.cos(winkel), MITTE_Y + RADIUS * anteil * Math.sin(winkel) };
	}

	private static String escape(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "&apos;");
	}

}
