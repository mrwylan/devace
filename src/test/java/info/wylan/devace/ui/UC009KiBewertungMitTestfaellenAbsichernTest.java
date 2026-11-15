package info.wylan.devace.ui;

import java.math.BigDecimal;
import java.util.List;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.textfield.TextArea;
import info.wylan.devace.TestcontainersConfiguration;
import info.wylan.devace.persistence.KatalogFrage;
import info.wylan.devace.persistence.TestfallRepository.PruefbarerKatalog;
import info.wylan.devace.persistence.TestlaufRepository.Testergebnis;
import info.wylan.devace.service.Ausgangspunkt;
import info.wylan.devace.service.FragenkatalogService;
import info.wylan.devace.service.KiModellClient;
import info.wylan.devace.service.KiModellClient.BewertungAnfrage;
import info.wylan.devace.service.KiModellClientFake;
import info.wylan.devace.usecase.UseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UC-009 KI-Bewertung mit Testfällen absichern, getestet über die Ansicht mit echter Datenbank. Das KI-Modell ist
 * durch {@link KiModellClientFake} ersetzt; ohne Vorgabe ordnet es jeder Antwort Stufe 3 zu.
 *
 * <p>
 * Testdaten (R__uc009_absicherung.sql): Rolle 9801 mit freigegebener Version 9801 (ein Testfall) und Entwurf 9802 (drei
 * Testfälle), Rolle 9802 nur archiviert, Rolle 9803 mit Entwurf ohne Testfall,
 * Rolle 9804 mit freigegebener Version und einem Testfall. Tests, die Daten ändern oder einen früheren Testlauf
 * brauchen, legen ihn in ihrer Transaktion an und rollen zurück.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@WithMockUser(username = "uc009-admin", roles = "ADMINISTRATOR")
class UC009KiBewertungMitTestfaellenAbsichernTest extends SpringBrowserlessTest {

	private static final KiModellClientFake KI = new KiModellClientFake();

	private static final HintergrundExecutorFake HINTERGRUND = new HintergrundExecutorFake();

	private static final long ENTWURF = 9802;

	@TestBean
	KiModellClient kiModellClient;

	/** ADR-005: Die KI-Aufrufe laufen im Test sofort; ihr Ergebnis erreicht die Ansicht mit roundTrip(). */
	@TestBean(name = "applicationTaskExecutor", methodName = "hintergrund")
	TaskExecutor applicationTaskExecutor;

	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private FragenkatalogService fragenkatalogService;

	static KiModellClient kiModellClient() {
		return KI;
	}

	static TaskExecutor hintergrund() {
		return HINTERGRUND;
	}

	@BeforeEach
	void zuruecksetzen() {
		KI.reset();
		HINTERGRUND.reset();
	}

	// ---------------------------------------------------------------- Hauptablauf

	@Test
	@Transactional
	@UseCase(id = "UC-009", scenario = "Main Success Scenario", businessRules = { "BR-002" })
	void uebersicht_zeigt_die_pruefbaren_fragenkataloge_mit_testfaellen_und_juengstem_testlauf() {
		frueherenTestlaufAnlegen();
		navigate(KiBewertungMitTestfaellenAbsichernView.class);

		// Schritt 2, BR-002: Entwurf und freigegebene Version, nicht die archivierte Version
		assertThat(zeile("UC009 Entwicklung", 1)).containsExactly("UC009 Entwicklung", "1", "Freigegeben", "1", "–");
		assertThat(zeile("UC009 Entwicklung", 2)).containsExactly("UC009 Entwicklung", "2", "Entwurf", "3",
				"66.67 % · Nicht bestanden");
		assertThat(zeile("UC009 Ohne Testfall", 1)).containsExactly("UC009 Ohne Testfall", "1", "Entwurf", "0", "–");
		assertThat(katalogGrid().getListDataView().getItems().map(PruefbarerKatalog::rolle))
			.doesNotContain("UC009 Archiv");
	}

	@Test
	@Transactional
	@UseCase(id = "UC-009", scenario = "Main Success Scenario", businessRules = { "BR-007" })
	void gewaehlter_fragenkatalog_zeigt_testfaelle_je_frage_und_fruehere_testlaeufe() {
		frueherenTestlaufAnlegen();
		navigate(KiBewertungMitTestfaellenAbsichernView.class);
		waehleKatalog(ENTWURF);

		// Schritt 4: die Testfälle je Frage
		assertThat(text("ki-frage-9803")).contains("Frage 1: UC009 Frage A?")
			.contains("UC009 Antwort eins")
			.contains("Erwartete Stufe: 3")
			.contains("UC009 Antwort zwei")
			.contains("Erwartete Stufe: 1");
		assertThat(text("ki-frage-9804")).contains("Frage 2: UC009 Frage B?").contains("UC009 Antwort drei");
		// Schritt 4, BR-007: die bisherigen Testläufe mit Zeitpunkt und Trefferquote
		Grid<?> laeufe = find(Grid.class).withId("testlaeufe").single();
		assertThat(test(laeufe).size()).isEqualTo(1);
		assertThat(test(laeufe).getCellText(0, 0)).isEqualTo("01.03.2026 09:30");
		assertThat(test(laeufe).getCellText(0, 1)).isEqualTo("uc009-admin");
		assertThat(test(laeufe).getCellText(0, 2)).isEqualTo("test/frueher");
		assertThat(test(laeufe).getCellText(0, 3)).isEqualTo("66.67 %");
		assertThat(test(laeufe).getCellText(0, 4)).isEqualTo("Nicht bestanden");

		// BR-007: ein früherer Testlauf zeigt seine Abweichungen
		test(laeufe).select(0);
		Grid<?> abweichungen = find(Grid.class).withId("testlauf-abweichungen-grid").single();
		assertThat(test(abweichungen).size()).isEqualTo(1);
		assertThat(test(abweichungen).getCellText(0, 1)).isEqualTo("UC009 Antwort zwei");
		assertThat(test(abweichungen).getCellText(0, 2)).isEqualTo("1");
		assertThat(test(abweichungen).getCellText(0, 3)).isEqualTo("2");
	}

	@Test
	@Transactional
	@UseCase(id = "UC-009", scenario = "Main Success Scenario", businessRules = { "BR-001" })
	void testfall_anlegen_aendern_und_entfernen() {
		navigate(KiBewertungMitTestfaellenAbsichernView.class);
		waehleKatalog(ENTWURF);

		// Schritt 5: anlegen
		test(button("testfall-hinzufuegen-9804")).click();
		fuelleTestfall("UC009 Neue Antwort", 5);
		test(button("testfall-speichern")).click();
		assertThat(testfaelleDerFrage(9804)).containsExactlyInAnyOrder("UC009 Antwort drei:4", "UC009 Neue Antwort:5");

		// Schritt 5: ändern
		test(button("testfall-bearbeiten-9803")).click();
		fuelleTestfall("UC009 Antwort drei, geändert", 2);
		test(button("testfall-speichern")).click();
		assertThat(testfaelleDerFrage(9804)).contains("UC009 Antwort drei, geändert:2");

		// Schritt 5: entfernen
		test(button("testfall-entfernen-9803")).click();
		assertThat(testfaelleDerFrage(9804)).containsExactly("UC009 Neue Antwort:5");
		// Erfolgsfall: Fragen und Entscheidungsmodell bleiben unverändert
		assertThat(this.jdbc.sql("select count(*) from frage where id = 9804").query(Long.class).single()).isEqualTo(1L);
	}

	@Test
	@Transactional
	@UseCase(id = "UC-009", scenario = "Main Success Scenario", businessRules = { "BR-007" })
	void entfernter_testfall_bleibt_im_frueheren_testlauf_erhalten() {
		frueherenTestlaufAnlegen();
		navigate(KiBewertungMitTestfaellenAbsichernView.class);
		waehleKatalog(ENTWURF);

		test(button("testfall-entfernen-9802")).click();

		// BR-007: Der Testlauf behält das Ergebnis mit der Kopie des Testfalls; nur der Verweis entfällt.
		assertThat(this.jdbc.sql("""
				select coalesce(testfall_id, 0) || ':' || antworttext || ':' || erwartete_stufe || ':' || ermittelte_stufe
				from testergebnis where id = 9802
				""").query(String.class).single()).isEqualTo("0:UC009 Antwort zwei:1:2");
	}

	@Test
	@Transactional
	@UseCase(id = "UC-009", scenario = "Main Success Scenario", businessRules = { "BR-003", "BR-004", "BR-005",
			"BR-006", "BR-008", "BR-009" }, requirements = { "NFR-001", "NFR-009", "C-005" })
	void testlauf_fuehrt_alle_testfaelle_aus_und_zeigt_trefferquote_und_abweichungen() {
		String fragenVorher = fragenUndVorgaben();
		long laeufeVorher = laeufe();
		java.time.LocalDateTime vorher = java.time.LocalDateTime.now().minusSeconds(1);
		navigate(KiBewertungMitTestfaellenAbsichernView.class);
		waehleKatalog(ENTWURF);
		// erwartet 3, 1, 4; das KI-Modell ordnet 3, 2, 0 zu: ein Treffer von drei
		KI.bewertetDerReiheNachMit(3, 2, 0);

		test(button("ki-ausfuehren")).click();
		roundTrip();

		// BR-003: alle drei Testfälle des Fragenkatalogs; BR-004, BR-008: nur Frage, Entscheidungsmodell, Antwort
		assertThat(KI.bewertungen()).hasSize(3);
		BewertungAnfrage erste = KI.bewertungen().get(0);
		assertThat(erste.frage()).isEqualTo("UC009 Frage A?");
		assertThat(erste.antwort()).isEqualTo("UC009 Antwort eins");
		assertThat(erste.vorgaben()).containsExactly("UC009 Vorgabe 0", "UC009 Vorgabe 1", "UC009 Vorgabe 2",
				"UC009 Vorgabe 3", "UC009 Vorgabe 4", "UC009 Vorgabe 5");
		assertThat(KI.bewertungen()).allSatisfy((anfrage) -> assertThat(anfrage.toString()).doesNotContain("uc009-admin")
			.doesNotContain("@"));
		// Schritt 9, BR-005, BR-006: 1 von 3 ist 33.33 %, unter 90 % nicht bestanden
		assertThat(text("ki-trefferquote")).isEqualTo("Trefferquote 33.33 % · Nicht bestanden");
		Grid<?> abweichungen = find(Grid.class).withId("ki-abweichungen").single();
		assertThat(test(abweichungen).size()).isEqualTo(2);
		assertThat(test(abweichungen).getCellText(0, 1)).isEqualTo("UC009 Antwort zwei");
		assertThat(test(abweichungen).getCellText(1, 1)).isEqualTo("UC009 Antwort drei");

		// Schritt 8: der Testlauf mit Modell, Administrator, Trefferquote und ermittelter Stufe je Testfall
		assertThat(laeufe()).isEqualTo(laeufeVorher + 1);
		assertThat(this.jdbc.sql("""
				select l.ki_modell || ':' || b.benutzername || ':' || l.trefferquote
				from testlauf l join benutzer b on b.id = l.ausgefuehrt_von_id
				where l.fragenkatalog_id = 9802 order by l.id desc limit 1
				""").query(String.class).single()).isEqualTo(KiModellClientFake.MODELL + ":uc009-admin:33.33");
		// Erfolgsfall: der Testlauf mit dem Zeitpunkt seiner Ausführung
		java.time.LocalDateTime zeitpunkt = this.jdbc
			.sql("select ausgefuehrt_am from testlauf where fragenkatalog_id = 9802 order by id desc limit 1")
			.query(java.time.LocalDateTime.class)
			.single();
		assertThat(zeitpunkt).isBetween(vorher, java.time.LocalDateTime.now().plusSeconds(1));
		assertThat(this.jdbc.sql("""
				select e.ermittelte_stufe from testergebnis e
				where e.testlauf_id = (select max(id) from testlauf where fragenkatalog_id = 9802) order by e.id
				""").query(Integer.class).list()).containsExactly(3, 2, 0);
		// BR-009: Fragen und Entscheidungsmodell bleiben unverändert
		assertThat(fragenUndVorgaben()).isEqualTo(fragenVorher);
		// der neue Testlauf erscheint in der Liste
		assertThat(test(find(Grid.class).withId("testlaeufe").single()).getCellText(0, 3)).isEqualTo("33.33 %");
	}

	@Test
	@Transactional
	@UseCase(id = "UC-009", scenario = "Main Success Scenario", businessRules = { "BR-005", "BR-006" },
			requirements = { "NFR-009" })
	void zwei_von_drei_treffern_sind_66_67_prozent_und_alle_treffer_sind_bestanden() {
		navigate(KiBewertungMitTestfaellenAbsichernView.class);
		waehleKatalog(ENTWURF);

		// BR-005: kaufmännisch auf zwei Nachkommastellen (OQ-091)
		KI.bewertetDerReiheNachMit(3, 1, 0);
		test(button("ki-ausfuehren")).click();
		roundTrip();
		assertThat(text("ki-trefferquote")).isEqualTo("Trefferquote 66.67 % · Nicht bestanden");

		// BR-006: ab 90 % bestanden
		KI.bewertetDerReiheNachMit(3, 1, 4);
		test(button("ki-ausfuehren")).click();
		roundTrip();
		assertThat(text("ki-trefferquote")).isEqualTo("Trefferquote 100.00 % · Bestanden");
		assertThat(test(find(Grid.class).withId("ki-abweichungen").single()).size()).isZero();
	}

	@Test
	@UseCase(id = "UC-009", scenario = "Main Success Scenario", businessRules = { "BR-006" })
	void bestanden_ab_genau_90_prozent() {
		// BR-006: Der Zielwert ist 90 Prozent; darunter nicht bestanden.
		assertThat(KiBewertungMitTestfaellenAbsichernView.urteil(new BigDecimal("90.00"))).isEqualTo("Bestanden");
		assertThat(KiBewertungMitTestfaellenAbsichernView.urteil(new BigDecimal("89.99"))).isEqualTo("Nicht bestanden");
	}

	@Test
	@UseCase(id = "UC-009", scenario = "Main Success Scenario", requirements = { "NFR-012" })
	void zeigt_den_wartehinweis_bis_der_testlauf_vorliegt() {
		navigate(KiBewertungMitTestfaellenAbsichernView.class);
		waehleKatalog(ENTWURF);

		HINTERGRUND.anhalten();
		test(button("ki-ausfuehren")).click();

		// Schritt 7, NFR-012: der Wartehinweis aus OQ-090, die Ausführung ist gesperrt
		assertThat(text("ki-wartehinweis")).isEqualTo("Die Testfälle werden ausgeführt.");
		assertThat(button("ki-ausfuehren").isEnabled()).isFalse();
		assertThat(KI.bewertungen()).isEmpty();
		HINTERGRUND.reset();
	}

	// ---------------------------------------------------------------- A1

	@Test
	@Transactional
	@UseCase(id = "UC-009", scenario = "A1: Kein Fragenkatalog prüfbar", businessRules = { "BR-002" })
	void ohne_pruefbaren_fragenkatalog_ist_keine_absicherung_moeglich() {
		// Alle Kataloge archivieren; das Zurückrollen des Tests stellt die Testdaten wieder her.
		this.jdbc.sql("""
				update fragenkatalog set status = 'Archiviert', freigegeben_am = coalesce(freigegeben_am, now()),
				freigegeben_von_id = coalesce(freigegeben_von_id, 9801)
				""").update();

		navigate(KiBewertungMitTestfaellenAbsichernView.class);

		assertThat(text("ki-absicherung-kein-katalog")).isEqualTo("Noch kein Fragenkatalog zur Prüfung vorhanden.");
		assertThat(find(Grid.class).all()).isEmpty();
	}

	// ---------------------------------------------------------------- A2

	@Test
	@UseCase(id = "UC-009", scenario = "A2: Angaben zum Testfall ungültig", businessRules = { "BR-001" })
	void fehlende_angaben_werden_am_feld_gemeldet_und_nichts_gespeichert() {
		navigate(KiBewertungMitTestfaellenAbsichernView.class);
		waehleKatalog(ENTWURF);

		test(button("testfall-hinzufuegen-9804")).click();
		test(button("testfall-speichern")).click();

		assertThat(antwortFeld().isInvalid()).isTrue();
		assertThat(antwortFeld().getErrorMessage()).isEqualTo("Antwort fehlt.");
		assertThat(stufeFeld().isInvalid()).isTrue();
		assertThat(stufeFeld().getErrorMessage()).isEqualTo("Stufe fehlt.");
		// Schritt 2 von A2: nichts gespeichert, der Dialog bleibt offen (weiter bei Schritt 5)
		assertThat(testfaelleDerFrage(9804)).containsExactly("UC009 Antwort drei:4");
		assertThat(find(com.vaadin.flow.component.dialog.Dialog.class).all()).hasSize(1);
	}

	@Test
	@Transactional
	@UseCase(id = "UC-009", scenario = "A2: Angaben zum Testfall ungültig", businessRules = { "BR-001" })
	void antwort_ueber_500_zeichen_wird_abgewiesen_500_sind_zulaessig() {
		navigate(KiBewertungMitTestfaellenAbsichernView.class);
		waehleKatalog(ENTWURF);

		test(button("testfall-hinzufuegen-9804")).click();
		fuelleTestfall("a".repeat(501), 2);
		test(button("testfall-speichern")).click();
		assertThat(antwortFeld().getErrorMessage()).isEqualTo("Höchstens 500 Zeichen.");
		assertThat(testfaelleDerFrage(9804)).hasSize(1);

		fuelleTestfall("a".repeat(500), 2);
		test(button("testfall-speichern")).click();
		assertThat(testfaelleDerFrage(9804)).hasSize(2);
	}

	// ---------------------------------------------------------------- A3

	@Test
	@UseCase(id = "UC-009", scenario = "A3: Fragenkatalog ohne Testfall")
	void fragenkatalog_ohne_testfall_hat_nichts_auszufuehren() {
		long laeufeVorher = laeufe();
		navigate(KiBewertungMitTestfaellenAbsichernView.class);
		waehleKatalog(9804);

		test(button("ki-ausfuehren")).click();
		roundTrip();

		assertThat(text("ki-ausfuehren-hinweis")).isEqualTo("Der Fragenkatalog hat keinen Testfall.");
		assertThat(KI.bewertungen()).isEmpty();
		assertThat(laeufe()).isEqualTo(laeufeVorher);
	}

	// ---------------------------------------------------------------- A4

	@Test
	@UseCase(id = "UC-009", scenario = "A4: KI-Modell liefert keine Stufe")
	void ohne_stufe_zu_einem_testfall_wird_kein_testlauf_gespeichert() {
		long laeufeVorher = laeufe();
		String fragenVorher = fragenUndVorgaben();
		navigate(KiBewertungMitTestfaellenAbsichernView.class);
		waehleKatalog(ENTWURF);
		KI.bewertetDerReiheNachMit(3, -1);

		test(button("ki-ausfuehren")).click();
		roundTrip();

		assertThat(text("ki-ausfuehren-hinweis"))
			.isEqualTo("Der Testlauf konnte nicht abgeschlossen werden. Es wurde nichts gespeichert.");
		// Fehlerfall: kein unvollständiger Testlauf
		assertThat(laeufe()).isEqualTo(laeufeVorher);
		// Fehlerfall: Fragen und Entscheidungsmodell sind unverändert
		assertThat(fragenUndVorgaben()).isEqualTo(fragenVorher);
		// Fehlerfall: Das KI-Modell erhielt bis zum Abbruch keine Angabe, die auf eine Person schließen lässt.
		assertThat(KI.bewertungen()).hasSize(2)
			.allSatisfy((anfrage) -> assertThat(anfrage.toString()).doesNotContain("uc009-admin").doesNotContain("@"));
		// weiter bei Schritt 6: die Ausführung lässt sich erneut anfordern
		assertThat(button("ki-ausfuehren").isEnabled()).isTrue();
	}

	@Test
	@UseCase(id = "UC-009", scenario = "A4: KI-Modell liefert keine Stufe")
	void nicht_erreichbares_ki_modell_speichert_keinen_testlauf() {
		long laeufeVorher = laeufe();
		navigate(KiBewertungMitTestfaellenAbsichernView.class);
		waehleKatalog(ENTWURF);
		KI.failNextBewertung();

		test(button("ki-ausfuehren")).click();
		roundTrip();

		assertThat(text("ki-ausfuehren-hinweis"))
			.isEqualTo("Der Testlauf konnte nicht abgeschlossen werden. Es wurde nichts gespeichert.");
		assertThat(laeufe()).isEqualTo(laeufeVorher);
	}

	// ---------------------------------------------------------------- BR-010

	@Test
	@Transactional
	@UseCase(id = "UC-009", businessRules = { "BR-010" })
	void speichern_des_entwurfs_behaelt_die_testfaelle_und_entfernt_die_einer_entfernten_frage() {
		frueherenTestlaufAnlegen();
		// UC-006: den vorhandenen Entwurf fortsetzen und unverändert speichern
		List<KatalogFrage> entwurf = this.fragenkatalogService.ausgangspunkt(9801, Ausgangspunkt.VORHANDENER_ENTWURF);
		this.fragenkatalogService.speichern(9801, entwurf);
		assertThat(testfallIds(9802)).containsExactly(9801L, 9802L, 9803L);

		// UC-006: Frage B entfernen und speichern; ihr Testfall entfällt, der frühere Testlauf behält die Kopie
		this.fragenkatalogService.speichern(9801, entwurf.subList(0, 1));
		assertThat(testfallIds(9802)).containsExactly(9801L, 9802L);
		assertThat(this.jdbc.sql("select coalesce(testfall_id, 0) || ':' || antworttext from testergebnis where id = 9803")
			.query(String.class)
			.single()).isEqualTo("0:UC009 Antwort drei");
	}

	@Test
	@Transactional
	@UseCase(id = "UC-009", businessRules = { "BR-010" })
	void neue_version_aus_der_freigegebenen_uebernimmt_die_testfaelle() {
		// UC-006: eine neue Version aus der freigegebenen Version 9805 beginnen und speichern
		List<KatalogFrage> neu = this.fragenkatalogService.ausgangspunkt(9804, Ausgangspunkt.FREIGEGEBENE_VERSION);
		this.fragenkatalogService.speichern(9804, neu);

		long entwurfId = this.jdbc.sql("select id from fragenkatalog where rolle_id = 9804 and status = 'Entwurf'")
			.query(Long.class)
			.single();
		assertThat(this.jdbc.sql("""
				select t.antworttext || ':' || t.erwartete_stufe from testfall t join frage f on f.id = t.frage_id
				where f.fragenkatalog_id = ?
				""").param(entwurfId).query(String.class).list()).containsExactly("UC009 Antwort der neuen Version:5");
		// die freigegebene Version behält ihren Testfall
		assertThat(testfallIds(9805)).containsExactly(9805L);
	}

	// ---------------------------------------------------------------- BR-011

	@Test
	@WithAnonymousUser
	@UseCase(id = "UC-009", businessRules = { "BR-011" }, requirements = { "NFR-005" })
	void aufruf_ohne_anmeldung_wird_abgewiesen() {
		UI.getCurrent().navigate("ki-absicherung");

		assertThat(find(KiBewertungMitTestfaellenAbsichernView.class).exists()).isFalse();
	}

	@Test
	@WithMockUser(roles = "TEAMLEITUNG")
	@UseCase(id = "UC-009", businessRules = { "BR-011" }, requirements = { "NFR-005" })
	void aufruf_ohne_passende_berechtigung_wird_abgewiesen() {
		UI.getCurrent().navigate("ki-absicherung");

		assertThat(find(KiBewertungMitTestfaellenAbsichernView.class).exists()).isFalse();
	}

	@Test
	@UseCase(id = "UC-009", businessRules = { "BR-011" }, requirements = { "NFR-005" })
	void administrator_erreicht_die_absicherung_und_der_eintrag_ist_aktiv() {
		UI.getCurrent().navigate("ki-absicherung");

		assertThat(find(KiBewertungMitTestfaellenAbsichernView.class).exists()).isTrue();
		assertThat(navigationseintrag("KI-Absicherung").isEnabled()).isTrue();
	}

	@Test
	@WithMockUser(username = "uc001-teamleitung", roles = "TEAMLEITUNG")
	@UseCase(id = "UC-009", businessRules = { "BR-011" }, requirements = { "NFR-005" })
	void navigation_zeigt_den_eintrag_ohne_berechtigung_deaktiviert() {
		navigate(EinladungErstellenUndVersendenView.class);

		assertThat(navigationseintrag("KI-Absicherung").isEnabled()).isFalse();
	}

	// ---------------------------------------------------------------- Hilfen

	/** Ein früherer Testlauf des Entwurfs: 2 von 3 Treffern, 66.67 %; nur in der Transaktion des Tests. */
	private void frueherenTestlaufAnlegen() {
		this.jdbc.sql("""
				insert into testlauf (id, fragenkatalog_id, ausgefuehrt_von_id, ausgefuehrt_am, ki_modell, trefferquote)
				values (9801, 9802, 9801, timestamp '2026-03-01 09:30:00', 'test/frueher', 66.67)
				""").update();
		this.jdbc.sql("""
				insert into testergebnis (id, testlauf_id, testfall_id, frage_text, antworttext, erwartete_stufe,
				ermittelte_stufe)
				values (9801, 9801, 9801, 'UC009 Frage A?', 'UC009 Antwort eins', 3, 3),
				       (9802, 9801, 9802, 'UC009 Frage A?', 'UC009 Antwort zwei', 1, 2),
				       (9803, 9801, 9803, 'UC009 Frage B?', 'UC009 Antwort drei', 4, 4)
				""").update();
	}

	@SuppressWarnings("unchecked")
	private Grid<PruefbarerKatalog> katalogGrid() {
		return find(Grid.class).withId("pruefbare-kataloge").single();
	}

	private List<String> zeile(String rolle, int version) {
		Grid<PruefbarerKatalog> grid = katalogGrid();
		List<PruefbarerKatalog> kataloge = grid.getListDataView().getItems().toList();
		int index = kataloge.indexOf(kataloge.stream()
			.filter((k) -> k.rolle().equals(rolle) && k.version() == version)
			.findFirst()
			.orElseThrow());
		return List.of(test(grid).getCellText(index, 0), test(grid).getCellText(index, 1),
				test(grid).getCellText(index, 2), test(grid).getCellText(index, 3), test(grid).getCellText(index, 4));
	}

	private void waehleKatalog(long katalogId) {
		Grid<PruefbarerKatalog> grid = katalogGrid();
		List<PruefbarerKatalog> kataloge = grid.getListDataView().getItems().toList();
		test(grid).select(kataloge.indexOf(kataloge.stream().filter((k) -> k.id() == katalogId).findFirst().orElseThrow()));
	}

	private Button button(String id) {
		return find(Button.class).withId(id).single();
	}

	private String text(String id) {
		return find(Div.class).withId(id).single().getElement().getTextRecursively();
	}

	private TextArea antwortFeld() {
		return find(TextArea.class).withId("testfall-antwort").single();
	}

	@SuppressWarnings("unchecked")
	private Select<Integer> stufeFeld() {
		return find(Select.class).withId("testfall-stufe").single();
	}

	private void fuelleTestfall(String antwort, int stufe) {
		test(antwortFeld()).setValue(antwort);
		stufeFeld().setValue(stufe);
	}

	private List<String> testfaelleDerFrage(long frageId) {
		return this.jdbc.sql("select antworttext || ':' || erwartete_stufe from testfall where frage_id = ? order by id")
			.param(frageId)
			.query(String.class)
			.list();
	}

	private List<Long> testfallIds(long katalogId) {
		return this.jdbc.sql("""
				select t.id from testfall t join frage f on f.id = t.frage_id where f.fragenkatalog_id = ? order by t.id
				""").param(katalogId).query(Long.class).list();
	}

	private long laeufe() {
		return this.jdbc.sql("select count(*) from testlauf").query(Long.class).single();
	}

	private String fragenUndVorgaben() {
		return String.join("|", this.jdbc.sql("""
				select f.id || ':' || f.text || ':' || v.stufe || ':' || v.beschreibung from frage f
				join stufenvorgabe v on v.frage_id = f.id where f.fragenkatalog_id = 9802 order by f.id, v.stufe
				""").query(String.class).list());
	}

	private SideNavItem navigationseintrag(String beschriftung) {
		return find(SideNavItem.class).all()
			.stream()
			.filter((eintrag) -> beschriftung.equals(eintrag.getLabel()))
			.findFirst()
			.orElseThrow();
	}

	@SuppressWarnings("unused")
	private static boolean abweichung(Testergebnis ergebnis) {
		return ergebnis.abweichung();
	}

}
