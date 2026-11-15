package info.wylan.devace.ui;

import java.util.List;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.sidenav.SideNavItem;
import info.wylan.devace.TestcontainersConfiguration;
import info.wylan.devace.persistence.RolleOption;
import info.wylan.devace.usecase.UseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UC-005 Radar-Charts für die Schulungsplanung auswerten, getestet über die Ansicht mit echter Datenbank.
 *
 * <p>
 * Testdaten (R__uc005_schulungsplanung.sql): Rolle 9701 mit Scores der MAGs 9701 und 9702 ohne Teamleitung,
 * Rolle 9702 mit Scores der MAG 9703, Rolle 9703 ohne Score. Die erwarteten Mittelwerte stehen im Kommentar der
 * Testdaten. Andere Use Cases haben eigene Rollen mit Scores; die Tests prüfen nur die Rollen mit dem Präfix UC005.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@WithMockUser(username = "uc005-schulung", roles = "SCHULUNGSVERANTWORTLICHE")
class UC005RadarChartsFuerDieSchulungsplanungAuswertenTest extends SpringBrowserlessTest {

	private static final String FELD_1 = "Domänenwissen & Business Alignment";

	private static final String FELD_2 = "Architektur & Engineering";

	private static final String FELD_3 = "Qualität & Testing";

	@Autowired
	private JdbcClient jdbc;

	// ---------------------------------------------------------------- Hauptablauf

	@Test
	@UseCase(id = "UC-005", scenario = "Main Success Scenario", businessRules = { "BR-001", "BR-002", "BR-004" },
			requirements = { "C-005" })
	void schulungsverantwortliche_sieht_die_radar_charts_aller_mags_der_rolle() {
		long scoresVorher = scores();
		navigate(RadarChartsFuerDieSchulungsplanungAuswertenView.class);

		// Schritt 2, BR-004: nur Rollen mit Scores (nicht 9703)
		assertThat(eigeneRollen()).containsExactly("UC005 Entwicklung", "UC005 Test");
		// Schritt 3: die Schulungsverantwortliche wählt selbst; vor der Wahl kein Radar-Chart
		assertThat(rolleAuswahl().getValue()).isNull();
		assertThat(find(Html.class).all()).isEmpty();

		waehleRolle("UC005 Entwicklung");

		// Schritt 4, BR-002, BR-004: alle MAGs mit Scores der Rolle, ohne Bezug auf eine Teamleitung; nicht 9703
		assertThat(karten()).containsExactly("schulung-mag-9701", "schulung-mag-9702");
		assertThat(titel(9701)).isEqualTo("UC005 Alpha · UC005 Nord");
		assertThat(titel(9702)).isEqualTo("UC005 Beta · UC005 Süd");
		// BR-001: Mittelwert je Handlungsfeld der MAG und Rolle, kaufmännisch auf eine Nachkommastelle gerundet
		assertThat(werte(9701)).contains(FELD_1 + "3.5", FELD_2 + "2.3").hasSize(6);
		assertThat(find(Html.class).all()).hasSize(2);

		// Nachbedingung: Es sind keine Daten geändert.
		assertThat(scores()).isEqualTo(scoresVorher);
	}

	@Test
	@UseCase(id = "UC-005", scenario = "Main Success Scenario", businessRules = { "BR-003" })
	void kleine_mag_mit_einem_einzigen_score_hat_ein_radar_chart() {
		navigate(RadarChartsFuerDieSchulungsplanungAuswertenView.class);
		waehleRolle("UC005 Entwicklung");

		// BR-003: MAG 9702 hat nur einen Score und erscheint trotzdem
		assertThat(werte(9702)).contains(FELD_1 + "1.0");
		assertThat(anzahl(svg(9702), "devace-radar-punkt")).isEqualTo(1);
	}

	@Test
	@UseCase(id = "UC-005", scenario = "Main Success Scenario", businessRules = { "BR-006" })
	void handlungsfeld_ohne_score_zeigt_keinen_wert_und_behaelt_seine_achse() {
		navigate(RadarChartsFuerDieSchulungsplanungAuswertenView.class);
		waehleRolle("UC005 Entwicklung");

		// BR-006: Handlungsfeld 3 ohne Score: kein Wert, auch nicht 0; sechs Achsen, zwei Punkte
		assertThat(werte(9701)).contains(FELD_3 + "–").doesNotContain(FELD_3 + "0.0");
		assertThat(anzahl(svg(9701), "devace-radar-achse")).isEqualTo(6);
		assertThat(anzahl(svg(9701), "devace-radar-punkt")).isEqualTo(2);
	}

	@Test
	@UseCase(id = "UC-005", scenario = "Main Success Scenario", businessRules = { "BR-001" })
	void skala_zeigt_nur_ganzzahlen() {
		navigate(RadarChartsFuerDieSchulungsplanungAuswertenView.class);
		waehleRolle("UC005 Entwicklung");

		// BR-001: fünf Ringe für die Ganzzahlen 1 bis 5, keine Zwischenstufen
		assertThat(anzahl(svg(9701), "devace-radar-ring")).isEqualTo(5);
	}

	@Test
	@UseCase(id = "UC-005", scenario = "Main Success Scenario", businessRules = { "BR-002", "BR-005" },
			requirements = { "NFR-002" })
	void zeigt_nur_mittelwerte_und_keine_einladungen_oder_einzelnen_scores() {
		navigate(RadarChartsFuerDieSchulungsplanungAuswertenView.class);
		waehleRolle("UC005 Entwicklung");

		// BR-005: nur Mittelwerte, keine einzelnen Scores (3.0, 4.0 und 2.0, 2.5), keine Zahl von Teilnehmern
		String text = find(RadarChartsFuerDieSchulungsplanungAuswertenView.class).single().getElement().getTextRecursively();
		assertThat(text).doesNotContain(FELD_1 + "3.0", FELD_1 + "4.0", FELD_2 + "2.0", FELD_2 + "2.5")
			.doesNotContainIgnoringCase("Teilnehmer")
			.doesNotContainIgnoringCase("Assessment")
			// BR-002: nur Radar-Charts, keine Einladungen
			.doesNotContainIgnoringCase("Einladung")
			.doesNotContain("@");
	}

	// ---------------------------------------------------------------- A1

	@Test
	@Transactional
	@UseCase(id = "UC-005", scenario = "A1: Keine Scores vorhanden", businessRules = { "BR-004" })
	void ohne_scores_ist_noch_keine_auswertung_moeglich() {
		// Alle Scores entfernen; das Zurückrollen des Tests stellt die Testdaten wieder her.
		this.jdbc.sql("delete from score").update();

		navigate(RadarChartsFuerDieSchulungsplanungAuswertenView.class);

		assertThat(find(Div.class).withId("schulung-keine-scores").single().getElement().getTextRecursively())
			.isEqualTo("Es liegen noch keine Scores vor.");
		// der Use Case endet: keine Auswahl und kein Radar-Chart
		assertThat(find(Select.class).all()).isEmpty();
		assertThat(find(Html.class).all()).isEmpty();
	}

	// ---------------------------------------------------------------- A2

	@Test
	@UseCase(id = "UC-005", scenario = "A2: Schulungsverantwortliche wechselt die Rolle")
	void wechsel_der_rolle_zeigt_die_radar_charts_zur_neuen_rolle() {
		navigate(RadarChartsFuerDieSchulungsplanungAuswertenView.class);
		waehleRolle("UC005 Entwicklung");
		assertThat(karten()).containsExactly("schulung-mag-9701", "schulung-mag-9702");

		waehleRolle("UC005 Test");

		// A2: nur die MAGs mit Scores der neuen Rolle
		assertThat(karten()).containsExactly("schulung-mag-9703");
		assertThat(werte(9703)).contains(FELD_1 + "5.0");
	}

	// ---------------------------------------------------------------- BR-007

	@Test
	@WithAnonymousUser
	@UseCase(id = "UC-005", businessRules = { "BR-007" }, requirements = { "NFR-005" })
	void aufruf_ohne_anmeldung_wird_abgewiesen() {
		UI.getCurrent().navigate("schulungsplanung");

		assertThat(find(RadarChartsFuerDieSchulungsplanungAuswertenView.class).exists()).isFalse();
	}

	@Test
	@WithMockUser(roles = "TEAMLEITUNG")
	@UseCase(id = "UC-005", businessRules = { "BR-007" }, requirements = { "NFR-005" })
	void aufruf_ohne_passende_berechtigung_wird_abgewiesen_und_aendert_nichts() {
		long scoresVorher = scores();
		UI.getCurrent().navigate("schulungsplanung");

		assertThat(find(RadarChartsFuerDieSchulungsplanungAuswertenView.class).exists()).isFalse();
		// Fehlerfall: Es werden keine Daten geändert.
		assertThat(scores()).isEqualTo(scoresVorher);
	}

	@Test
	@UseCase(id = "UC-005", businessRules = { "BR-007" }, requirements = { "NFR-005" })
	void schulungsverantwortliche_erreicht_die_auswertung_und_der_eintrag_ist_aktiv() {
		UI.getCurrent().navigate("schulungsplanung");

		assertThat(find(RadarChartsFuerDieSchulungsplanungAuswertenView.class).exists()).isTrue();
		assertThat(navigationseintrag("Schulungsplanung").isEnabled()).isTrue();
	}

	@Test
	@WithMockUser(roles = "ADMINISTRATOR")
	@UseCase(id = "UC-005", businessRules = { "BR-007" }, requirements = { "NFR-005" })
	void navigation_zeigt_den_eintrag_ohne_berechtigung_deaktiviert() {
		navigate(MagsUndRollenView.class);

		assertThat(navigationseintrag("Schulungsplanung").isEnabled()).isFalse();
	}

	// ---------------------------------------------------------------- Hilfen

	@SuppressWarnings("unchecked")
	private Select<RolleOption> rolleAuswahl() {
		return find(Select.class).withId("schulung-rolle").single();
	}

	private List<String> eigeneRollen() {
		return rolleAuswahl().getListDataView().getItems().map(RolleOption::name).filter((n) -> n.startsWith("UC005")).toList();
	}

	private void waehleRolle(String rolle) {
		Select<RolleOption> feld = rolleAuswahl();
		feld.setValue(feld.getListDataView().getItems().filter((r) -> r.name().equals(rolle)).findFirst().orElseThrow());
	}

	private List<String> karten() {
		return find(Div.class).withId("schulung-radar-charts")
			.single()
			.getChildren()
			.map((karte) -> karte.getId().orElse(""))
			.toList();
	}

	private Div karte(long magId) {
		return find(Div.class).withId("schulung-mag-" + magId).single();
	}

	private String titel(long magId) {
		return karte(magId).getChildren().findFirst().orElseThrow().getElement().getText();
	}

	/** Je Handlungsfeld Name und Wert ohne Trenner, z. B. "Qualität & Testing–". */
	private List<String> werte(long magId) {
		return karte(magId).getChildren()
			.filter((teil) -> teil.getElement().getClassList().contains("devace-score-liste"))
			.findFirst()
			.orElseThrow()
			.getChildren()
			.map((zeile) -> zeile.getElement().getTextRecursively())
			.toList();
	}

	private String svg(long magId) {
		return karte(magId).getChildren()
			.filter(Html.class::isInstance)
			.findFirst()
			.orElseThrow()
			.getElement()
			.getOuterHTML();
	}

	private long scores() {
		return this.jdbc.sql("select count(*) from score").query(Long.class).single();
	}

	private static int anzahl(String text, String teil) {
		return text.split(java.util.regex.Pattern.quote(teil), -1).length - 1;
	}

	private SideNavItem navigationseintrag(String beschriftung) {
		return find(SideNavItem.class).all()
			.stream()
			.filter((eintrag) -> beschriftung.equals(eintrag.getLabel()))
			.findFirst()
			.orElseThrow();
	}

}
