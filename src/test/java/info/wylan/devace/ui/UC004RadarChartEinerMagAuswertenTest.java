package info.wylan.devace.ui;

import java.util.List;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.sidenav.SideNavItem;
import info.wylan.devace.TestcontainersConfiguration;
import info.wylan.devace.persistence.MagListItem;
import info.wylan.devace.persistence.RolleOption;
import info.wylan.devace.service.AuswertungService;
import info.wylan.devace.service.MagNichtZugeordnetException;
import info.wylan.devace.usecase.UseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * UC-004 Radar-Chart einer MAG auswerten, getestet über die Ansicht mit echter Datenbank.
 *
 * <p>
 * Testdaten (R__uc004_auswertung.sql): uc004-teamleitung hat die MAG 9601 mit Scores und die MAG 9602 ohne Scores;
 * die MAG 9604 mit Scores gehört einer anderen Teamleitung. Die erwarteten Mittelwerte stehen im Kommentar der
 * Testdaten.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@WithMockUser(username = "uc004-teamleitung", roles = "TEAMLEITUNG")
class UC004RadarChartEinerMagAuswertenTest extends SpringBrowserlessTest {

	private static final String FELD_1 = "Domänenwissen & Business Alignment";

	private static final String FELD_2 = "Architektur & Engineering";

	private static final String FELD_3 = "Qualität & Testing";

	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private AuswertungService service;

	// ---------------------------------------------------------------- Hauptablauf

	@Test
	@UseCase(id = "UC-004", scenario = "Main Success Scenario", businessRules = { "BR-001", "BR-002", "BR-004" },
			requirements = { "C-005" })
	void teamleitung_sieht_das_radar_chart_ihrer_mag_und_rolle() {
		long scoresVorher = scores();
		navigate(RadarChartEinerMagAuswertenView.class);

		// Schritt 2, BR-002, BR-004: nur eigene MAGs mit Scores (nicht 9602 ohne Scores, nicht die fremde 9604)
		assertThat(mags()).containsExactly("UC004 Alpha · UC004 Nord", "UC004 Delta · UC004 Nord");
		// Schritt 3: die Teamleitung wählt selbst; vor der Wahl ist nichts gewählt und kein Radar-Chart zu sehen
		assertThat(magAuswahl().getValue()).isNull();
		assertThat(rolleAuswahl().getValue()).isNull();
		assertThat(find(Html.class).all()).isEmpty();

		// Schritt 2: bei der gewählten MAG nur Rollen mit Scores (nicht 9603)
		waehleMag("UC004 Alpha · UC004 Nord");
		assertThat(rollen()).containsExactly("UC004 Entwicklung", "UC004 Test");
		assertThat(rolleAuswahl().getValue()).isNull();
		assertThat(find(Html.class).all()).isEmpty();

		// Schritt 3 und 4: MAG und Rolle gewählt, das Radar-Chart mit dem Wert je Handlungsfeld
		waehleRolle("UC004 Entwicklung");
		// BR-001: Mittelwert aller Scores, auch aus einer anderen Version des Fragenkatalogs, kaufmännisch gerundet
		assertThat(werte()).contains(FELD_1 + "3.2", FELD_2 + "2.3", FELD_3 + "4.5");
		assertThat(werte()).contains(handlungsfeldName(5) + "1.0", handlungsfeldName(6) + "5.0").hasSize(6);
		assertThat(find(Html.class).single().getElement().getOuterHTML())
			.contains("Radar-Chart der Scores je Handlungsfeld");

		// Nachbedingung: Es sind keine Daten geändert.
		assertThat(scores()).isEqualTo(scoresVorher);
	}

	@Test
	@UseCase(id = "UC-004", scenario = "Main Success Scenario", businessRules = { "BR-006" })
	void handlungsfeld_ohne_score_zeigt_keinen_wert_und_behaelt_seine_achse() {
		navigate(RadarChartEinerMagAuswertenView.class);
		waehle("UC004 Alpha · UC004 Nord", "UC004 Entwicklung");

		// BR-006: Handlungsfeld 4 hat keinen Score: kein Wert, auch nicht 0
		String feld4 = handlungsfeldName(4);
		assertThat(werte()).contains(feld4 + "–").doesNotContain(feld4 + "0.0");
		// die Achse bleibt, aber kein Punkt: sechs Achsen, fünf Punkte
		String svg = find(Html.class).single().getElement().getOuterHTML();
		assertThat(anzahl(svg, "devace-radar-achse")).isEqualTo(6);
		assertThat(anzahl(svg, "devace-radar-punkt")).isEqualTo(5);
		// die Beschriftung der Achse steht im SVG mit maskiertem &
		assertThat(svg).contains(feld4.replace("&", "&amp;"));
	}

	@Test
	@UseCase(id = "UC-004", scenario = "Main Success Scenario", businessRules = { "BR-001" })
	void skala_zeigt_nur_ganzzahlen() {
		navigate(RadarChartEinerMagAuswertenView.class);
		waehle("UC004 Alpha · UC004 Nord", "UC004 Entwicklung");

		// BR-001: Die Skala zeigt nur die Ganzzahlen: fünf Ringe für 1 bis 5, keine Zwischenstufen
		String svg = find(Html.class).single().getElement().getOuterHTML();
		assertThat(anzahl(svg, "devace-radar-ring")).isEqualTo(5);
	}

	@Test
	@UseCase(id = "UC-004", scenario = "Main Success Scenario", businessRules = { "BR-005" },
			requirements = { "NFR-002" })
	void zeigt_nur_mittelwerte_und_keine_einzelnen_scores() {
		navigate(RadarChartEinerMagAuswertenView.class);
		waehle("UC004 Alpha · UC004 Nord", "UC004 Entwicklung");

		// BR-005: nur der Mittelwert, keine einzelnen Scores (3.0, 4.0, 2.5 und 2.0, 2.5), keine Zahl von Teilnehmern
		String text = find(RadarChartEinerMagAuswertenView.class).single().getElement().getTextRecursively();
		assertThat(text).doesNotContain(FELD_1 + "3.0", FELD_1 + "4.0", FELD_1 + "2.5", FELD_2 + "2.0", FELD_2 + "2.5")
			.doesNotContainIgnoringCase("Teilnehmer")
			.doesNotContainIgnoringCase("Assessment")
			.doesNotContainIgnoringCase("Einladung");
	}

	// ---------------------------------------------------------------- A1

	@Test
	@WithMockUser(username = "uc004-ohne-scores", roles = "TEAMLEITUNG")
	@UseCase(id = "UC-004", scenario = "A1: Keine Scores vorhanden", businessRules = { "BR-004" })
	void ohne_scores_ist_noch_keine_auswertung_moeglich() {
		long scoresVorher = scores();
		navigate(RadarChartEinerMagAuswertenView.class);

		assertThat(find(Div.class).withId("auswertung-keine-scores").single().getElement().getTextRecursively())
			.isEqualTo("Zu Ihren MAGs liegen noch keine Scores vor.");
		// der Use Case endet: keine Auswahl und kein Radar-Chart
		assertThat(find(Select.class).all()).isEmpty();
		assertThat(find(Html.class).all()).isEmpty();
		// Fehlerfall: Es werden keine Daten geändert.
		assertThat(scores()).isEqualTo(scoresVorher);
	}

	// ---------------------------------------------------------------- A2

	@Test
	@UseCase(id = "UC-004", scenario = "A2: Teamleitung wechselt MAG oder Rolle", businessRules = { "BR-003" })
	void wechsel_der_rolle_zeigt_das_radar_chart_zur_neuen_auswahl() {
		navigate(RadarChartEinerMagAuswertenView.class);
		waehle("UC004 Alpha · UC004 Nord", "UC004 Entwicklung");
		assertThat(werte()).contains(FELD_1 + "3.2");

		waehle("UC004 Alpha · UC004 Nord", "UC004 Test");

		// A2, BR-003: das Radar-Chart zur neuen Auswahl, auch aus dem Score eines einzigen Assessments
		assertThat(werte()).contains(FELD_1 + "1.0", FELD_2 + "–", FELD_3 + "–");
		assertThat(anzahl(find(Html.class).single().getElement().getOuterHTML(), "devace-radar-punkt")).isEqualTo(1);
	}

	@Test
	@UseCase(id = "UC-004", scenario = "A2: Teamleitung wechselt MAG oder Rolle", businessRules = { "BR-001" })
	void wechsel_der_mag_zeigt_das_radar_chart_der_neuen_mag() {
		navigate(RadarChartEinerMagAuswertenView.class);
		waehle("UC004 Alpha · UC004 Nord", "UC004 Entwicklung");
		assertThat(werte()).contains(FELD_1 + "3.2");

		// A2: eine andere MAG; die Rolle ist neu zu wählen, bis dahin kein Radar-Chart der alten MAG
		waehleMag("UC004 Delta · UC004 Nord");
		assertThat(rolleAuswahl().getValue()).isNull();
		assertThat(find(Html.class).all()).isEmpty();
		assertThat(rollen()).containsExactly("UC004 Entwicklung");

		// dieselbe Rolle wie zuvor zeigt jetzt die Werte der neuen MAG
		waehleRolle("UC004 Entwicklung");
		assertThat(werte()).contains(FELD_1 + "2.0", FELD_2 + "–", FELD_3 + "–");
	}

	// ---------------------------------------------------------------- BR-002, BR-007

	@Test
	@UseCase(id = "UC-004", businessRules = { "BR-002" })
	void fremde_mag_wird_nicht_ausgewertet() {
		long scoresVorher = scores();
		// die Ansicht bietet die fremde MAG 9604 nicht an (Hauptablauf); auch ein direkter Aufruf zeigt keine Scores
		assertThatThrownBy(() -> this.service.radarChart(9604, 9601)).isInstanceOf(MagNichtZugeordnetException.class);
		assertThatThrownBy(() -> this.service.rollen(9604)).isInstanceOf(MagNichtZugeordnetException.class);
		// Fehlerfall: Es werden keine Daten geändert.
		assertThat(scores()).isEqualTo(scoresVorher);
	}

	@Test
	@WithAnonymousUser
	@UseCase(id = "UC-004", businessRules = { "BR-007" }, requirements = { "NFR-005" })
	void aufruf_ohne_anmeldung_wird_abgewiesen() {
		UI.getCurrent().navigate("auswertung");

		assertThat(find(RadarChartEinerMagAuswertenView.class).exists()).isFalse();
	}

	@Test
	@WithMockUser(roles = "ADMINISTRATOR")
	@UseCase(id = "UC-004", businessRules = { "BR-007" }, requirements = { "NFR-005" })
	void aufruf_ohne_passende_berechtigung_wird_abgewiesen() {
		UI.getCurrent().navigate("auswertung");

		assertThat(find(RadarChartEinerMagAuswertenView.class).exists()).isFalse();
	}

	@Test
	@UseCase(id = "UC-004", businessRules = { "BR-007" }, requirements = { "NFR-005" })
	void teamleitung_erreicht_die_auswertung_und_der_eintrag_ist_aktiv() {
		UI.getCurrent().navigate("auswertung");

		assertThat(find(RadarChartEinerMagAuswertenView.class).exists()).isTrue();
		assertThat(navigationseintrag("Radar-Chart").isEnabled()).isTrue();
	}

	@Test
	@WithMockUser(roles = "ADMINISTRATOR")
	@UseCase(id = "UC-004", businessRules = { "BR-007" }, requirements = { "NFR-005" })
	void navigation_zeigt_den_eintrag_ohne_berechtigung_deaktiviert() {
		navigate(MagsUndRollenView.class);

		assertThat(navigationseintrag("Radar-Chart").isEnabled()).isFalse();
	}

	// ---------------------------------------------------------------- Hilfen

	@SuppressWarnings("unchecked")
	private Select<MagListItem> magAuswahl() {
		return find(Select.class).withId("auswertung-mag").single();
	}

	@SuppressWarnings("unchecked")
	private Select<RolleOption> rolleAuswahl() {
		return find(Select.class).withId("auswertung-rolle").single();
	}

	private List<String> mags() {
		Select<MagListItem> auswahl = magAuswahl();
		return auswahl.getListDataView().getItems().map(auswahl.getItemLabelGenerator()::apply).toList();
	}

	private List<String> rollen() {
		Select<RolleOption> auswahl = rolleAuswahl();
		return auswahl.getListDataView().getItems().map(auswahl.getItemLabelGenerator()::apply).toList();
	}

	private void waehle(String mag, String rolle) {
		waehleMag(mag);
		waehleRolle(rolle);
	}

	private void waehleMag(String mag) {
		Select<MagListItem> magFeld = magAuswahl();
		magFeld.setValue(magFeld.getListDataView()
			.getItems()
			.filter((eintrag) -> magFeld.getItemLabelGenerator().apply(eintrag).equals(mag))
			.findFirst()
			.orElseThrow());
	}

	private void waehleRolle(String rolle) {
		Select<RolleOption> rolleFeld = rolleAuswahl();
		rolleFeld.setValue(
				rolleFeld.getListDataView().getItems().filter((r) -> r.name().equals(rolle)).findFirst().orElseThrow());
	}

	/** Je Handlungsfeld Name und Wert ohne Trenner, z. B. "Qualität & Testing4.5". */
	private List<String> werte() {
		return find(Div.class).withId("auswertung-werte")
			.single()
			.getChildren()
			.map((zeile) -> zeile.getElement().getTextRecursively())
			.toList();
	}

	private String handlungsfeldName(int nummer) {
		return this.jdbc.sql("select name from handlungsfeld where nummer = ?").param(nummer).query(String.class).single();
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
