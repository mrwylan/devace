package info.wylan.devace.ui;

import java.time.LocalDateTime;
import java.util.List;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.sidenav.SideNavItem;
import info.wylan.devace.TestcontainersConfiguration;
import info.wylan.devace.persistence.FreigabeKandidat;
import info.wylan.devace.service.FragenkatalogService;
import info.wylan.devace.service.KeinEntwurfZurFreigabeException;
import info.wylan.devace.usecase.UseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * UC-007 Fragenkatalog freigeben, getestet über die Ansicht mit echter Datenbank.
 *
 * <p>
 * Testdaten (R__uc007_freigabe.sql): Rolle 9501 mit vollständigem Entwurf Version 1 (Katalog 9501) ohne freigegebene
 * Version; Rolle 9502 mit freigegebener Version 1 (Katalog 9502) und vollständigem Entwurf Version 2 (Katalog 9503);
 * Rolle 9503 mit unvollständigem Entwurf Version 1 (Katalog 9504: Handlungsfelder 5 und 6 ohne Frage, Frage 2 ohne
 * Vorgabe für Stufe 5); Rolle 9504 nur mit freigegebener Version 1 (Katalog 9505). Vollständige Kataloge haben je
 * Handlungsfeld eine Frage. Das Konto des Administrators legt der Test an (die Datei darf keinen Administrator
 * enthalten, weil sonst das Startkonto der Browser-Tests fehlt).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@WithMockUser(username = UC007FragenkatalogFreigebenTest.ADMIN, roles = "ADMINISTRATOR")
class UC007FragenkatalogFreigebenTest extends SpringBrowserlessTest {

	static final String ADMIN = "uc007-administrator";

	private static final long ADMIN_ID = 9501;

	private static final String ERSTFREIGABE = "UC007 Erstfreigabe";

	private static final String NACHFOLGER = "UC007 Nachfolger";

	private static final String UNVOLLSTAENDIG = "UC007 Unvollständig";

	private static final String OHNE_ENTWURF = "UC007 Ohne Entwurf";

	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private FragenkatalogService service;

	@BeforeEach
	void administrator() {
		this.jdbc.sql("""
				insert into benutzer (id, benutzername, passwort_hash, berechtigung)
				values (?, ?, 'unused', 'Administrator') on conflict do nothing""").param(ADMIN_ID).param(ADMIN).update();
	}

	@AfterEach
	void aufraeumen() {
		// nur, was die Tests geändert oder angelegt haben: Freigaben der Testdaten zurücksetzen (Reihenfolge wegen
		// der einen freigegebenen Version je Rolle), das laufende Assessment von BR-006 entfernen
		this.jdbc.sql("delete from assessment where id = 9501").update();
		this.jdbc.sql("delete from einladung where id = 9501").update();
		this.jdbc.sql("""
				update fragenkatalog set status = 'Entwurf', freigegeben_am = null, freigegeben_von_id = null
				where id in (9501, 9503, 9504)""").update();
		this.jdbc.sql("""
				update fragenkatalog set status = 'Freigegeben', freigegeben_am = timestamp '2026-01-02 10:00:00',
				freigegeben_von_id = 9301 where id in (9502, 9505)""").update();
	}

	// ---------------------------------------------------------------- Schritte 1 bis 4

	@Test
	@UseCase(id = "UC-007", scenario = "Steps 1-2: Rollen mit Entwurf", businessRules = { "BR-003" })
	void zeigt_die_rollen_mit_entwurf_zur_auswahl() {
		navigate(FragenkatalogFreigebenView.class);

		List<String> rollen = kandidaten().stream().map(FreigabeKandidat::rolle).toList();
		assertThat(rollen).contains(ERSTFREIGABE, NACHFOLGER, UNVOLLSTAENDIG).doesNotContain(OHNE_ENTWURF);
		assertThat(zelle(NACHFOLGER, 1)).isEqualTo("Version 2");
		assertThat(zelle(NACHFOLGER, 2)).isEqualTo("Version 1");
		assertThat(zelle(ERSTFREIGABE, 1)).isEqualTo("Version 1");
		assertThat(zelle(ERSTFREIGABE, 2)).isEqualTo("–");
	}

	@Test
	@UseCase(id = "UC-007", scenario = "Steps 3-4: Entwurf prüfen")
	void zeigt_den_entwurf_je_handlungsfeld_mit_fragen_und_entscheidungsmodell() {
		navigate(FragenkatalogFreigebenView.class);

		test(pruefen(NACHFOLGER)).click();

		assertThat(find(Span.class).withClassName("devace-untertitel").all().get(0).getText()).contains("Version 2");
		List<String> felder = handlungsfeldNamen();
		List<AccordionPanel> panels = find(AccordionPanel.class).all();
		assertThat(panels).hasSize(6);
		for (int nummer = 1; nummer <= 6; nummer++) {
			assertThat(panels.get(nummer - 1).getSummary().getElement().getTextRecursively()).contains(felder.get(nummer - 1)).contains("1 Fragen");
			assertThat(panels.get(nummer - 1).getElement().getTextRecursively())
				.contains("UC007 Frage " + nummer + " in Katalog 9503");
		}
		Div frage = find(Div.class).withId("frage-1").single();
		List<String> modell = find(ListItem.class, frage).all().stream().map(ListItem::getText).toList();
		assertThat(modell).containsExactly("Stufe 0 – Nie gehört: UC007 Vorgabe 0 zu Frage 95031",
				"Stufe 1 – Schon mal gehört: UC007 Vorgabe 1 zu Frage 95031",
				"Stufe 2 – Grundverständnis: UC007 Vorgabe 2 zu Frage 95031",
				"Stufe 3 – Sicherer Umgang: UC007 Vorgabe 3 zu Frage 95031",
				"Stufe 4 – Fortgeschrittene Expertise: UC007 Vorgabe 4 zu Frage 95031",
				"Stufe 5 – Expert:in / Lehrniveau: UC007 Vorgabe 5 zu Frage 95031");
	}

	// ---------------------------------------------------------------- Schritte 5 bis 9

	@Test
	@UseCase(id = "UC-007", scenario = "Steps 5-6: Zusammenfassung", businessRules = { "BR-001" })
	void vollstaendiger_entwurf_fuehrt_zur_zusammenfassung() {
		navigate(FragenkatalogFreigebenView.class);
		test(pruefen(NACHFOLGER)).click();

		test(find(Button.class).withId("freigabe-anfordern").single()).click();

		Dialog dialog = find(Dialog.class).withId("freigabe-dialog").single();
		assertThat(dialog.isOpened()).isTrue();
		assertThat(find(Paragraph.class, dialog).withId("freigabe-zusammenfassung").single().getText())
			.isEqualTo("Rolle " + NACHFOLGER + " · Version 2");
		List<String> jeFeld = find(ListItem.class, dialog).all().stream().map(ListItem::getText).toList();
		assertThat(jeFeld).containsExactlyElementsOf(handlungsfeldNamen().stream().map(n -> n + ": 1 Fragen").toList());
		assertThat(find(Div.class).withId("entwurf-unvollstaendig").exists()).isFalse();
	}

	@Test
	@UseCase(id = "UC-007", businessRules = { "BR-002", "BR-004", "BR-005" })
	void freigabe_gibt_den_entwurf_frei_und_archiviert_die_bisherige_version() {
		List<String> inhaltVorher = inhalt(9503);
		LocalDateTime vorher = LocalDateTime.now().withNano(0);
		navigate(FragenkatalogFreigebenView.class);
		test(pruefen(NACHFOLGER)).click();
		test(find(Button.class).withId("freigabe-anfordern").single()).click();

		test(find(Button.class).withId("freigabe-bestaetigen").single()).click();

		// Schritt 9: Bestätigung
		assertThat(test(find(Notification.class).last()).getText()).isEqualTo("Fragenkatalog freigegeben.");
		// Post-S-1, BR-005: freigegeben mit Zeitpunkt und Administrator
		assertThat(status(9503)).isEqualTo("Freigegeben");
		assertThat(this.jdbc.sql("select freigegeben_von_id from fragenkatalog where id = 9503")
			.query(Long.class)
			.single()).isEqualTo(ADMIN_ID);
		assertThat(this.jdbc.sql("select freigegeben_am from fragenkatalog where id = 9503")
			.query(LocalDateTime.class)
			.single()).isBetween(vorher, LocalDateTime.now().plusSeconds(1));
		// Post-S-2, BR-002: die bisherige Version ist archiviert und behält ihre Freigabe
		assertThat(status(9502)).isEqualTo("Archiviert");
		assertThat(this.jdbc.sql("select freigegeben_am from fragenkatalog where id = 9502")
			.query(LocalDateTime.class)
			.single()).isEqualTo(LocalDateTime.of(2026, 1, 2, 10, 0));
		// Post-S-3: genau eine freigegebene Version
		assertThat(freigegebeneVersionen(9502)).isEqualTo(1);
		// Post-S-4, BR-004: Fragen und Entscheidungsmodell unverändert
		assertThat(inhalt(9503)).isEqualTo(inhaltVorher);
		// die Rolle hat keinen Entwurf mehr und erscheint nicht mehr zur Auswahl
		assertThat(kandidaten().stream().map(FreigabeKandidat::rolle)).doesNotContain(NACHFOLGER);
	}

	@Test
	@UseCase(id = "UC-007", businessRules = { "BR-002" })
	void erste_freigabe_einer_rolle_ohne_bisherige_version() {
		navigate(FragenkatalogFreigebenView.class);
		test(pruefen(ERSTFREIGABE)).click();
		test(find(Button.class).withId("freigabe-anfordern").single()).click();

		test(find(Button.class).withId("freigabe-bestaetigen").single()).click();

		assertThat(status(9501)).isEqualTo("Freigegeben");
		assertThat(freigegebeneVersionen(9501)).isEqualTo(1);
	}

	@Test
	@UseCase(id = "UC-007", businessRules = { "BR-006" })
	void laufendes_assessment_behaelt_seinen_fragenkatalog() {
		// ein laufendes Assessment mit der freigegebenen Version 1 der Rolle 9502 (Teamleitung, MAG und
		// Starthinweis aus R__uc002_assessment.sql)
		this.jdbc.sql("""
				insert into einladung (id, referenz, mag_id, rolle_id, erstellt_von_id, status, erstellt_am,
				gueltigkeit_tage, laeuft_ab_am)
				values (9501, 'uc007-laufend-9501', 9301, 9502, 9301, 'Eingelöst', timestamp '2026-03-01 10:00:00', 30,
				date '2026-03-30')""").update();
		this.jdbc.sql("""
				insert into assessment (id, einladung_id, mag_id, rolle_id, fragenkatalog_id, starthinweis_id, status,
				gestartet_am, letzte_eingabe_am)
				values (9501, 9501, 9301, 9502, 9502, 9301, 'Laufend', localtimestamp, localtimestamp)""").update();
		navigate(FragenkatalogFreigebenView.class);
		test(pruefen(NACHFOLGER)).click();
		test(find(Button.class).withId("freigabe-anfordern").single()).click();

		test(find(Button.class).withId("freigabe-bestaetigen").single()).click();

		assertThat(status(9502)).isEqualTo("Archiviert");
		assertThat(this.jdbc.sql("select fragenkatalog_id from assessment where id = 9501")
			.query(Long.class)
			.single()).isEqualTo(9502L);
		assertThat(this.jdbc.sql("select count(*) from frage where fragenkatalog_id = 9502")
			.query(Integer.class)
			.single()).isEqualTo(6);
	}

	// ---------------------------------------------------------------- A1

	@Test
	@Transactional
	@UseCase(id = "UC-007", scenario = "A1: Kein Entwurf vorhanden")
	void ohne_entwurf_erscheint_der_hinweis() {
		// alle Entwürfe in der Testtransaktion entfernen; das Zurückrollen stellt die gemeinsamen Testdaten wieder her
		String entwuerfe = "select id from fragenkatalog where status = 'Entwurf'";
		this.jdbc.sql("delete from frage where fragenkatalog_id in (" + entwuerfe + ")").update();
		this.jdbc.sql("delete from fragenkatalog where status = 'Entwurf'").update();

		navigate(FragenkatalogFreigebenView.class);

		assertThat(find(Div.class).withId("kein-entwurf").single().getElement().getTextRecursively())
			.contains("Es liegt kein Entwurf zur Freigabe vor.");
		assertThat(find(Grid.class).exists()).isFalse();
	}

	@Test
	@UseCase(id = "UC-007", scenario = "A1: Kein Entwurf vorhanden", businessRules = { "BR-003" })
	void freigegebene_version_ohne_entwurf_wird_nicht_freigegeben() {
		assertThatThrownBy(() -> this.service.freigeben(9504, 1)).isInstanceOf(KeinEntwurfZurFreigabeException.class)
			.hasMessage("Es liegt kein Entwurf zur Freigabe vor.");

		assertThat(status(9505)).isEqualTo("Freigegeben");
		assertThat(this.jdbc.sql("select freigegeben_von_id from fragenkatalog where id = 9505")
			.query(Long.class)
			.single()).isEqualTo(9301L);
	}

	@Test
	@UseCase(id = "UC-007", scenario = "A1: Kein Entwurf vorhanden", businessRules = { "BR-003" })
	void geaenderte_version_seit_der_zusammenfassung_wird_nicht_freigegeben() {
		// bestätigt wird Version 1, der Entwurf der Rolle ist aber Version 2
		assertThatThrownBy(() -> this.service.freigeben(9502, 1)).isInstanceOf(KeinEntwurfZurFreigabeException.class);

		assertThat(status(9502)).isEqualTo("Freigegeben");
		assertThat(status(9503)).isEqualTo("Entwurf");
	}

	// ---------------------------------------------------------------- A2

	@Test
	@UseCase(id = "UC-007", scenario = "A2: Entwurf unvollständig", businessRules = { "BR-001" })
	void unvollstaendiger_entwurf_wird_nicht_freigegeben() {
		List<String> felder = handlungsfeldNamen();
		navigate(FragenkatalogFreigebenView.class);
		test(pruefen(UNVOLLSTAENDIG)).click();

		test(find(Button.class).withId("freigabe-anfordern").single()).click();

		Div hinweis = find(Div.class).withId("entwurf-unvollstaendig").single();
		assertThat(hinweis.getElement().getTextRecursively())
			.contains("Der Entwurf ist unvollständig und wird nicht freigegeben.");
		assertThat(find(Span.class, hinweis).withId("handlungsfelder-ohne-frage").single().getText())
			.isEqualTo("Handlungsfelder ohne Frage: " + felder.get(4) + ", " + felder.get(5));
		assertThat(find(Span.class, hinweis).withId("fragen-unvollstaendig").single().getText())
			.isEqualTo("Fragen mit unvollständigem Entscheidungsmodell: Frage 2");
		assertThat(find(Dialog.class).withId("freigabe-dialog").exists()).isFalse();
		// Fehlerfall: kein Fragenkatalog geändert
		assertThat(status(9504)).isEqualTo("Entwurf");
		assertThat(this.jdbc.sql("select freigegeben_am is null from fragenkatalog where id = 9504")
			.query(Boolean.class)
			.single()).isTrue();
	}

	// ---------------------------------------------------------------- A3

	@Test
	@UseCase(id = "UC-007", scenario = "A3: Administrator bricht ab")
	void abbruch_vor_der_anforderung_gibt_nicht_frei() {
		navigate(FragenkatalogFreigebenView.class);
		test(pruefen(NACHFOLGER)).click();

		test(find(Button.class).withId("pruefung-abbrechen").single()).click();

		assertThat(find(Grid.class).withId("freigabe-grid").exists()).isTrue();
		assertThat(status(9503)).isEqualTo("Entwurf");
		assertThat(status(9502)).isEqualTo("Freigegeben");
	}

	@Test
	@UseCase(id = "UC-007", scenario = "A3: Administrator bricht ab")
	void abbruch_bei_der_zusammenfassung_gibt_nicht_frei() {
		navigate(FragenkatalogFreigebenView.class);
		test(pruefen(NACHFOLGER)).click();
		test(find(Button.class).withId("freigabe-anfordern").single()).click();

		test(find(Button.class).withId("freigabe-abbrechen").single()).click();

		assertThat(find(Dialog.class).withId("freigabe-dialog").exists()).isFalse();
		assertThat(status(9503)).isEqualTo("Entwurf");
		assertThat(status(9502)).isEqualTo("Freigegeben");
		assertThat(freigegebeneVersionen(9502)).isEqualTo(1);
	}

	// ---------------------------------------------------------------- BR-007 / NFR-005

	@Test
	@WithAnonymousUser
	@UseCase(id = "UC-007", businessRules = { "BR-007" }, requirements = { "NFR-005" })
	void aufruf_ohne_anmeldung_wird_abgewiesen() {
		UI.getCurrent().navigate("fragenkatalog-freigeben");

		assertThat(find(FragenkatalogFreigebenView.class).exists()).isFalse();
	}

	@Test
	@WithMockUser(roles = "TEAMLEITUNG")
	@UseCase(id = "UC-007", businessRules = { "BR-007" }, requirements = { "NFR-005" })
	void aufruf_ohne_passende_berechtigung_wird_abgewiesen() {
		UI.getCurrent().navigate("fragenkatalog-freigeben");

		assertThat(find(FragenkatalogFreigebenView.class).exists()).isFalse();
		assertThatThrownBy(() -> this.service.freigeben(9502, 2)).isInstanceOf(AccessDeniedException.class);
		assertThat(status(9503)).isEqualTo("Entwurf");
	}

	@Test
	@UseCase(id = "UC-007", businessRules = { "BR-007" }, requirements = { "NFR-005" })
	void administrator_erreicht_dieselbe_route() {
		UI.getCurrent().navigate("fragenkatalog-freigeben");

		assertThat(find(FragenkatalogFreigebenView.class).exists()).isTrue();
		assertThat(navigationseintrag("Freigabe").isEnabled()).isTrue();
	}

	@Test
	@WithMockUser(username = "uc008-teamleitung-a", roles = "TEAMLEITUNG")
	@UseCase(id = "UC-007", businessRules = { "BR-007" }, requirements = { "NFR-005" })
	void navigation_zeigt_den_eintrag_ohne_berechtigung_deaktiviert() {
		navigate(EinladungErstellenUndVersendenView.class);

		assertThat(navigationseintrag("Freigabe").isEnabled()).isFalse();
	}

	// ---------------------------------------------------------------- Hilfen

	@SuppressWarnings("unchecked")
	private Grid<FreigabeKandidat> grid() {
		return find(Grid.class).withId("freigabe-grid").single();
	}

	private List<FreigabeKandidat> kandidaten() {
		return grid().getListDataView().getItems().toList();
	}

	private int zeile(String rolle) {
		List<FreigabeKandidat> alle = kandidaten();
		return alle.indexOf(alle.stream().filter(k -> k.rolle().equals(rolle)).findFirst().orElseThrow());
	}

	/** Text der Komponente in Spalte 1 (Entwurf) oder 2 (freigegebene Version). */
	private String zelle(String rolle, int spalte) {
		return test(grid()).getCellComponent(zeile(rolle), spalte).getElement().getTextRecursively();
	}

	private Button pruefen(String rolle) {
		return (Button) test(grid()).getCellComponent(zeile(rolle), 3);
	}

	private List<String> handlungsfeldNamen() {
		return this.jdbc.sql("select name from handlungsfeld order by nummer").query(String.class).list();
	}

	private String status(long katalogId) {
		return this.jdbc.sql("select status from fragenkatalog where id = ?").param(katalogId).query(String.class).single();
	}

	private int freigegebeneVersionen(long rolleId) {
		return this.jdbc.sql("select count(*) from fragenkatalog where rolle_id = ? and status = 'Freigegeben'")
			.param(rolleId)
			.query(Integer.class)
			.single();
	}

	/** Fragen und Vorgaben des Katalogs in fester Reihenfolge. */
	private List<String> inhalt(long katalogId) {
		return this.jdbc.sql("""
				select f.position || '|' || f.handlungsfeld_id || '|' || f.text || '|' || s.stufe || '|' || s.beschreibung
				from frage f join stufenvorgabe s on s.frage_id = f.id
				where f.fragenkatalog_id = ? order by f.position, s.stufe""").param(katalogId).query(String.class).list();
	}

	private SideNavItem navigationseintrag(String beschriftung) {
		return find(SideNavItem.class).all()
			.stream()
			.filter(eintrag -> beschriftung.equals(eintrag.getLabel()))
			.findFirst()
			.orElseThrow();
	}

}
