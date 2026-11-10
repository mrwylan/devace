package info.wylan.devace.ui;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import info.wylan.devace.TestcontainersConfiguration;
import info.wylan.devace.persistence.RolleListItem;
import info.wylan.devace.service.Ausgangspunkt;
import info.wylan.devace.service.KiModellClient;
import info.wylan.devace.service.KiModellClient.VorgeschlageneFrage;
import info.wylan.devace.service.KiModellClientFake;
import info.wylan.devace.usecase.UseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.convention.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UC-006 Fragenkatalog und Entscheidungsmodell erstellen, getestet über die Ansicht mit echter Datenbank. Das
 * KI-Modell ist durch {@link KiModellClientFake} ersetzt.
 *
 * <p>
 * Testdaten (R__uc006_fragenkatalog.sql): Rolle 9101 ohne Fragenkatalog, Rolle 9102 mit freigegebener Version 2 (zwei
 * Fragen), Rolle 9103 mit Entwurf Version 1 (eine Frage).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@WithMockUser(roles = "ADMINISTRATOR")
class UC006FragenkatalogUndEntscheidungsmodellErstellenTest extends SpringBrowserlessTest {

	private static final String LEER = "UC006 Leer";

	private static final String FREIGEGEBEN = "UC006 Freigegeben";

	private static final String ENTWURF = "UC006 Entwurf";

	private static final long ROLLE_LEER = 9101;

	private static final long ROLLE_FREIGEGEBEN = 9102;

	private static final KiModellClientFake KI = new KiModellClientFake();

	@TestBean
	KiModellClient kiModellClient;

	@Autowired
	private JdbcClient jdbc;

	static KiModellClient kiModellClient() {
		return KI;
	}

	@BeforeEach
	void zuruecksetzen() {
		KI.reset();
	}

	@AfterEach
	void aufraeumen() {
		// nur, was die Tests selbst gespeichert haben: Kataloge der Testrollen außerhalb der Testdaten (Ids ab 9101)
		String katalog = "select id from fragenkatalog where id < 9000 and rolle_id in (9101, 9102, 9103)";
		this.jdbc.sql("delete from stufenvorgabe where frage_id in (select id from frage where fragenkatalog_id in ("
				+ katalog + "))").update();
		this.jdbc.sql("delete from frage where fragenkatalog_id in (" + katalog + ")").update();
		this.jdbc.sql("delete from fragenkatalog where id < 9000 and rolle_id in (9101, 9102, 9103)").update();
	}

	// ---------------------------------------------------------------- Schritte 1 bis 4

	@Test
	@UseCase(id = "UC-006", scenario = "Steps 1-2: Übersicht")
	void uebersicht_zeigt_rollen_mit_version_und_status_ihrer_fragenkataloge() {
		navigate(FragenkatalogUndEntscheidungsmodellErstellenView.class);

		assertThat(zelle(LEER, 1)).isEmpty();
		assertThat(zelle(LEER, 2)).isEmpty();
		assertThat(aktion(LEER).getText()).isEqualTo("Katalog beginnen");
		assertThat(zelle(FREIGEGEBEN, 1)).isEqualTo("Version 2");
		assertThat(zelle(FREIGEGEBEN, 2)).isEmpty();
		assertThat(aktion(FREIGEGEBEN).getText()).isEqualTo("Neue Version beginnen");
		assertThat(zelle(ENTWURF, 1)).isEmpty();
		assertThat(zelle(ENTWURF, 2)).isEqualTo("Version 1");
		assertThat(aktion(ENTWURF).getText()).isEqualTo("Entwurf fortsetzen");
	}

	@Test
	@UseCase(id = "UC-006", scenario = "Steps 3-4: Ausgangspunkt", businessRules = { "GR-004" })
	@SuppressWarnings("unchecked")
	void freigegebene_version_wird_als_ausgangspunkt_angeboten_und_ergibt_die_naechste_version() {
		navigate(FragenkatalogUndEntscheidungsmodellErstellenView.class);

		test(aktion(FREIGEGEBEN)).click();

		RadioButtonGroup<Ausgangspunkt> auswahl = find(RadioButtonGroup.class).withId("ausgangspunkt").single();
		List<String> angebote = auswahl.getListDataView()
			.getItems()
			.map(auswahl.getItemLabelGenerator()::apply)
			.toList();
		assertThat(angebote).containsExactlyInAnyOrder("Leerer Entwurf", "Freigegebene Version 2 als neue Version 3");

		auswahl.setValue(Ausgangspunkt.FREIGEGEBENE_VERSION);
		test(find(Button.class).withId("ausgangspunkt-uebernehmen").single()).click();

		// Schritt 4: der Ausgangspunkt je Handlungsfeld, mit den Fragen der freigegebenen Version
		assertThat(fragen(1)).containsExactly("UC006 Wie gehen Sie mit Anforderungen um?");
		assertThat(fragen(2)).containsExactly("UC006 Wie begründen Sie eine Entscheidung?");
		assertThat(fragen(3)).isEmpty();
		assertThat(untertitel()).contains("Version 3");
	}

	@Test
	@UseCase(id = "UC-006", scenario = "Steps 3-4: Ausgangspunkt", businessRules = { "GR-004" })
	void ohne_freigegebene_version_steht_nur_der_leere_entwurf_zur_verfuegung() {
		navigate(FragenkatalogUndEntscheidungsmodellErstellenView.class);

		test(aktion(LEER)).click();

		// ein einziger Ausgangspunkt: kein Auswahldialog, der leere Entwurf je Handlungsfeld
		assertThat(find(RadioButtonGroup.class).exists()).isFalse();
		assertThat(handlungsfeldGrids()).hasSize(6);
		assertThat(fragenGesamt()).isZero();
		assertThat(untertitel()).contains("Version 1");
	}

	@Test
	@UseCase(id = "UC-006", scenario = "Steps 3-4: Ausgangspunkt", businessRules = { "GR-004" })
	void vorhandener_entwurf_wird_fortgesetzt_statt_einen_weiteren_zu_beginnen() {
		navigate(FragenkatalogUndEntscheidungsmodellErstellenView.class);

		test(aktion(ENTWURF)).click();

		assertThat(find(RadioButtonGroup.class).exists()).isFalse();
		assertThat(fragen(3)).containsExactly("UC006 Wie testen Sie im Alltag?");
		assertThat(fragenGesamt()).isEqualTo(1);
		assertThat(untertitel()).contains("Version 1");
	}

	// ---------------------------------------------------------------- A1 und Schritte 8 bis 12

	@Test
	@UseCase(id = "UC-006", scenario = "A1: Ohne KI-Entwurf", businessRules = { "GR-001", "GR-002", "GR-004" })
	void frage_und_entscheidungsmodell_selbst_erfassen_und_als_entwurf_speichern() {
		navigate(FragenkatalogUndEntscheidungsmodellErstellenView.class);
		test(aktion(LEER)).click();

		test(find(Button.class).withId("frage-hinzufuegen").single()).click();
		// GR-001 und GR-002: genau 500 Zeichen sind zulässig
		fuelleFrage("F".repeat(500), "v0", "v1", "v2", "v3", "v4", "V".repeat(500));
		test(find(Button.class).withId("frage-uebernehmen").single()).click();
		test(find(Button.class).withId("entwurf-speichern").single()).click();

		// Schritt 12: die Bestätigung; danach wieder die Übersicht mit der Version des Entwurfs
		assertThat(test(find(Notification.class).single()).getText()).isEqualTo("Entwurf gespeichert.");
		assertThat(zelle(LEER, 2)).isEqualTo("Version 1");
		// Postcondition aus der Datenbank: Fragenkatalog mit Status Entwurf, Frage in genau einem Handlungsfeld
		assertThat(this.jdbc.sql("select version from fragenkatalog where rolle_id = 9101 and status = 'Entwurf'")
			.query(Integer.class)
			.list()).containsExactly(1);
		assertThat(this.jdbc.sql("""
				select h.nummer from frage f join fragenkatalog k on k.id = f.fragenkatalog_id
				join handlungsfeld h on h.id = f.handlungsfeld_id where k.rolle_id = 9101
				""").query(Integer.class).list()).containsExactly(1);
		// GR-001: genau eine Vorgabe je Stufe von 0 bis 5
		assertThat(this.jdbc.sql("""
				select s.stufe from stufenvorgabe s join frage f on f.id = s.frage_id
				join fragenkatalog k on k.id = f.fragenkatalog_id where k.rolle_id = 9101 order by s.stufe
				""").query(Integer.class).list()).containsExactly(0, 1, 2, 3, 4, 5);
	}

	@Test
	@UseCase(id = "UC-006", scenario = "Steps 8-12: Nur der Entwurf ändert sich", businessRules = { "GR-004", "GR-005" })
	void speichern_legt_die_naechste_version_an_und_laesst_die_freigegebene_unveraendert() {
		navigate(FragenkatalogUndEntscheidungsmodellErstellenView.class);
		test(aktion(FREIGEGEBEN)).click();
		auswaehlen(Ausgangspunkt.FREIGEGEBENE_VERSION);

		test(find(Button.class).withId("frage-hinzufuegen").single()).click();
		fuelleFrage("UC006 Neue Frage?", "a0", "a1", "a2", "a3", "a4", "a5");
		test(find(Button.class).withId("frage-uebernehmen").single()).click();
		test(find(Button.class).withId("entwurf-speichern").single()).click();

		assertThat(zelle(FREIGEGEBEN, 1)).isEqualTo("Version 2");
		assertThat(zelle(FREIGEGEBEN, 2)).isEqualTo("Version 3");
		// GR-004: Version = höchste Version + 1, Status Entwurf, drei Fragen
		assertThat(this.jdbc.sql("""
				select count(*) from frage f join fragenkatalog k on k.id = f.fragenkatalog_id
				where k.rolle_id = 9102 and k.version = 3 and k.status = 'Entwurf'
				""").query(Long.class).single()).isEqualTo(3L);
		// GR-005: die freigegebene Version 2 bleibt unverändert
		assertThat(this.jdbc.sql("select status from fragenkatalog where id = 9101").query(String.class).single())
			.isEqualTo("Freigegeben");
		assertThat(this.jdbc.sql("select count(*) from frage where fragenkatalog_id = 9101").query(Long.class).single())
			.isEqualTo(2L);
		assertThat(this.jdbc.sql("""
				select count(*) from stufenvorgabe s join frage f on f.id = s.frage_id where f.fragenkatalog_id = 9101
				""").query(Long.class).single()).isEqualTo(12L);
	}

	@Test
	@UseCase(id = "UC-006", scenario = "Step 8: Frage entfernen")
	void frage_wird_aus_dem_entwurf_entfernt() {
		navigate(FragenkatalogUndEntscheidungsmodellErstellenView.class);
		test(aktion(FREIGEGEBEN)).click();
		auswaehlen(Ausgangspunkt.FREIGEGEBENE_VERSION);

		test((Button) test(handlungsfeldGrids().get(0)).getCellComponent(0, 3)).click();
		test(find(Button.class).withId("frage-entfernen").single()).click();

		assertThat(fragen(1)).isEmpty();
		assertThat(fragen(2)).containsExactly("UC006 Wie begründen Sie eine Entscheidung?");
	}

	// ---------------------------------------------------------------- A4, A5

	@Test
	@UseCase(id = "UC-006", scenario = "A4: Entwurf unvollständig oder ungültig", businessRules = { "GR-001" })
	void fehlende_vorgabe_einer_stufe_wird_gemeldet_und_nichts_gespeichert() {
		navigate(FragenkatalogUndEntscheidungsmodellErstellenView.class);
		test(aktion(LEER)).click();
		test(find(Button.class).withId("frage-hinzufuegen").single()).click();
		fuelleFrage("UC006 Frage ohne Stufe 3?", "v0", "v1", "v2", "", "v4", "v5");
		test(find(Button.class).withId("frage-uebernehmen").single()).click();

		test(find(Button.class).withId("entwurf-speichern").single()).click();

		assertThat(fehlerMeldungen()).anyMatch(text -> text.contains("Vorgabe fehlt."));
		assertThat(this.jdbc.sql("select count(*) from fragenkatalog where rolle_id = 9101").query(Long.class).single())
			.isZero();

		// Schritt 8: die betroffene Vorgabe am Feld, dann korrigieren und speichern
		test((Button) test(handlungsfeldGrids().get(0)).getCellComponent(0, 3)).click();
		TextArea vorgabe = find(TextArea.class).withId("vorgabe-3").single();
		assertThat(vorgabe.isInvalid()).isTrue();
		assertThat(vorgabe.getErrorMessage()).isEqualTo("Vorgabe fehlt.");
		test(vorgabe).setValue("v3");
		test(find(Button.class).withId("frage-uebernehmen").single()).click();
		test(find(Button.class).withId("entwurf-speichern").single()).click();

		assertThat(test(find(Notification.class).single()).getText()).isEqualTo("Entwurf gespeichert.");
		assertThat(this.jdbc.sql("select count(*) from fragenkatalog where rolle_id = 9101 and status = 'Entwurf'")
			.query(Long.class)
			.single()).isEqualTo(1L);
	}

	@Test
	@UseCase(id = "UC-006", scenario = "A4: Entwurf unvollständig oder ungültig", businessRules = { "GR-002" })
	void frage_ohne_wortlaut_wird_gemeldet() {
		navigate(FragenkatalogUndEntscheidungsmodellErstellenView.class);
		test(aktion(LEER)).click();
		test(find(Button.class).withId("frage-hinzufuegen").single()).click();
		fuelleFrage("   ", "v0", "v1", "v2", "v3", "v4", "v5");
		test(find(Button.class).withId("frage-uebernehmen").single()).click();

		test(find(Button.class).withId("entwurf-speichern").single()).click();

		assertThat(fehlerMeldungen()).anyMatch(text -> text.contains("Wortlaut fehlt."));
		test((Button) test(handlungsfeldGrids().get(0)).getCellComponent(0, 3)).click();
		TextArea wortlaut = find(TextArea.class).withId("frage-text").single();
		assertThat(wortlaut.isInvalid()).isTrue();
		assertThat(wortlaut.getErrorMessage()).isEqualTo("Wortlaut fehlt.");
		assertThat(this.jdbc.sql("select count(*) from fragenkatalog where rolle_id = 9101").query(Long.class).single())
			.isZero();
	}

	@Test
	@UseCase(id = "UC-006", scenario = "A4: Entwurf unvollständig oder ungültig", businessRules = { "GR-001", "GR-002" })
	void angaben_ueber_500_zeichen_werden_gemeldet() {
		navigate(FragenkatalogUndEntscheidungsmodellErstellenView.class);
		test(aktion(LEER)).click();
		test(find(Button.class).withId("frage-hinzufuegen").single()).click();
		fuelleFrage("F".repeat(501), "v0", "v1", "v2", "v3", "v4", "V".repeat(501));
		test(find(Button.class).withId("frage-uebernehmen").single()).click();

		test(find(Button.class).withId("entwurf-speichern").single()).click();

		test((Button) test(handlungsfeldGrids().get(0)).getCellComponent(0, 3)).click();
		assertThat(find(TextArea.class).withId("frage-text").single().getErrorMessage())
			.isEqualTo("Höchstens 500 Zeichen.");
		assertThat(find(TextArea.class).withId("vorgabe-5").single().getErrorMessage())
			.isEqualTo("Höchstens 500 Zeichen.");
		assertThat(this.jdbc.sql("select count(*) from fragenkatalog where rolle_id = 9101").query(Long.class).single())
			.isZero();
	}

	@Test
	@UseCase(id = "UC-006", scenario = "A5: Administrator bricht ab")
	void abbrechen_verwirft_die_aenderungen_dieser_sitzung() {
		navigate(FragenkatalogUndEntscheidungsmodellErstellenView.class);
		test(aktion(LEER)).click();
		test(find(Button.class).withId("frage-hinzufuegen").single()).click();
		fuelleFrage("UC006 Verworfen?", "v0", "v1", "v2", "v3", "v4", "v5");
		test(find(Button.class).withId("frage-uebernehmen").single()).click();

		test(find(Button.class).withId("entwurf-abbrechen").single()).click();

		assertThat(find(Grid.class).withId("rolle-grid").exists()).isTrue();
		assertThat(this.jdbc.sql("select count(*) from fragenkatalog where rolle_id = 9101").query(Long.class).single())
			.isZero();
	}

	// ---------------------------------------------------------------- Schritte 5 bis 7, A1 bis A3, A6, A7

	@Test
	@UseCase(id = "UC-006", scenario = "Steps 5-7: KI-Entwurf", businessRules = { "GR-003", "GR-006", "GR-007", "GR-010" })
	void ki_entwurf_ergaenzt_je_handlungsfeld_drei_fragen_und_sendet_nur_rolle_schwerpunkte_und_zahl() {
		navigate(FragenkatalogUndEntscheidungsmodellErstellenView.class);
		test(aktion(LEER)).click();

		// GR-003: vorgeschlagen werden 3 Fragen je Handlungsfeld
		assertThat(find(IntegerField.class).withId("ki-anzahl").single().getValue()).isEqualTo(3);
		find(TextArea.class).withId("ki-schwerpunkte").single().setValue("Praxisfragen zur Zusammenarbeit");
		test(find(Button.class).withId("ki-anfordern").single()).click();

		assertThat(KI.anfragen()).hasSize(1);
		KiModellClient.EntwurfAnfrage anfrage = KI.anfragen().get(0);
		// GR-007: die Anfrage kennt nur Rolle, Handlungsfelder, Schwerpunkte und Zahl der Fragen
		assertThat(anfrage.rolle()).isEqualTo(LEER);
		assertThat(anfrage.schwerpunkte()).isEqualTo("Praxisfragen zur Zusammenarbeit");
		assertThat(anfrage.handlungsfelder()).hasSize(6).allMatch(h -> h.anzahl() == 3);
		// Schritt 7: der Entwurf je Handlungsfeld, drei Fragen je Handlungsfeld, nichts gespeichert (GR-006)
		for (int feld = 1; feld <= 6; feld++) {
			assertThat(fragen(feld)).hasSize(3);
		}
		assertThat(this.jdbc.sql("select count(*) from fragenkatalog where rolle_id = 9101").query(Long.class).single())
			.isZero();
	}

	@Test
	@UseCase(id = "UC-006", scenario = "Steps 5-7: KI-Entwurf", businessRules = { "GR-010" })
	void ki_entwurf_aendert_vorhandene_fragen_nicht_und_ergaenzt_nur_die_fehlenden() {
		navigate(FragenkatalogUndEntscheidungsmodellErstellenView.class);
		test(aktion(FREIGEGEBEN)).click();
		auswaehlen(Ausgangspunkt.FREIGEGEBENE_VERSION);

		test(find(Button.class).withId("ki-anfordern").single()).click();

		Map<Integer, Integer> angefordert = KI.anfragen()
			.get(0)
			.handlungsfelder()
			.stream()
			.collect(Collectors.toMap(KiModellClient.HandlungsfeldAnfrage::nummer, KiModellClient.HandlungsfeldAnfrage::anzahl));
		assertThat(angefordert).containsExactlyInAnyOrderEntriesOf(Map.of(1, 2, 2, 2, 3, 3, 4, 3, 5, 3, 6, 3));
		assertThat(fragen(1)).hasSize(3).first().isEqualTo("UC006 Wie gehen Sie mit Anforderungen um?");
		assertThat(fragen(2)).hasSize(3).first().isEqualTo("UC006 Wie begründen Sie eine Entscheidung?");
	}

	@Test
	@UseCase(id = "UC-006", scenario = "A3: Angaben für den KI-Entwurf ungültig", businessRules = { "GR-003" })
	void zahl_der_fragen_ausserhalb_von_1_bis_10_wird_gemeldet() {
		navigate(FragenkatalogUndEntscheidungsmodellErstellenView.class);
		test(aktion(LEER)).click();
		IntegerField anzahl = find(IntegerField.class).withId("ki-anzahl").single();

		for (int ungueltig : new int[] { 0, 11 }) {
			anzahl.setValue(ungueltig);
			test(find(Button.class).withId("ki-anfordern").single()).click();
			assertThat(anzahl.isInvalid()).isTrue();
			assertThat(anzahl.getErrorMessage()).isEqualTo("Die Zahl muss zwischen 1 und 10 liegen.");
		}
		assertThat(KI.anfragen()).isEmpty();

		// die Grenzen 1 und 10 sind zulässig
		anzahl.setValue(10);
		test(find(Button.class).withId("ki-anfordern").single()).click();
		assertThat(KI.anfragen()).hasSize(1);
		assertThat(KI.anfragen().get(0).handlungsfelder()).allMatch(h -> h.anzahl() == 10);
	}

	@Test
	@UseCase(id = "UC-006", scenario = "A3: Angaben für den KI-Entwurf ungültig", businessRules = { "GR-003" })
	void schwerpunkte_ueber_2000_zeichen_werden_gemeldet() {
		navigate(FragenkatalogUndEntscheidungsmodellErstellenView.class);
		test(aktion(LEER)).click();
		TextArea schwerpunkte = find(TextArea.class).withId("ki-schwerpunkte").single();

		schwerpunkte.setValue("S".repeat(2001));
		test(find(Button.class).withId("ki-anfordern").single()).click();

		assertThat(schwerpunkte.isInvalid()).isTrue();
		assertThat(schwerpunkte.getErrorMessage()).isEqualTo("Höchstens 2000 Zeichen.");
		assertThat(KI.anfragen()).isEmpty();

		// genau 2000 Zeichen sind zulässig
		schwerpunkte.setValue("S".repeat(2000));
		test(find(Button.class).withId("ki-anfordern").single()).click();
		assertThat(KI.anfragen()).hasSize(1);
		assertThat(KI.anfragen().get(0).schwerpunkte()).hasSize(2000);
	}

	@Test
	@UseCase(id = "UC-006", scenario = "A2: KI-Modell liefert keinen Entwurf")
	void nicht_erreichbares_ki_modell_laesst_ausgangspunkt_und_angaben_unveraendert() {
		navigate(FragenkatalogUndEntscheidungsmodellErstellenView.class);
		test(aktion(FREIGEGEBEN)).click();
		auswaehlen(Ausgangspunkt.FREIGEGEBENE_VERSION);
		find(IntegerField.class).withId("ki-anzahl").single().setValue(4);
		find(TextArea.class).withId("ki-schwerpunkte").single().setValue("Schwerpunkt A");

		KI.failNext();
		test(find(Button.class).withId("ki-anfordern").single()).click();

		assertThat(fehlerMeldungen()).anyMatch(text -> text.contains("Das KI-Modell hat keinen Entwurf geliefert."));
		assertThat(fragenGesamt()).isEqualTo(2);
		assertThat(find(IntegerField.class).withId("ki-anzahl").single().getValue()).isEqualTo(4);
		assertThat(find(TextArea.class).withId("ki-schwerpunkte").single().getValue()).isEqualTo("Schwerpunkt A");

		// der Use Case wird bei Schritt 5 fortgesetzt: ein erneuter Versuch ergänzt den Entwurf
		test(find(Button.class).withId("ki-anfordern").single()).click();
		assertThat(fragenGesamt()).isEqualTo(24);
	}

	@Test
	@UseCase(id = "UC-006", scenario = "A2: KI-Modell liefert keinen Entwurf")
	void ki_modell_ohne_frage_gilt_als_nicht_verwertbar() {
		navigate(FragenkatalogUndEntscheidungsmodellErstellenView.class);
		test(aktion(LEER)).click();

		KI.antwortet(List.of());
		test(find(Button.class).withId("ki-anfordern").single()).click();

		assertThat(fehlerMeldungen()).anyMatch(text -> text.contains("Das KI-Modell hat keinen Entwurf geliefert."));
		assertThat(fragenGesamt()).isZero();
	}

	@Test
	@UseCase(id = "UC-006", scenario = "A4: Entwurf unvollständig oder ungültig", businessRules = { "GR-011" })
	void unvollstaendige_frage_des_ki_entwurfs_erscheint_und_muss_vor_dem_speichern_ergaenzt_werden() {
		navigate(FragenkatalogUndEntscheidungsmodellErstellenView.class);
		test(aktion(LEER)).click();

		KI.antwortet(List.of(new VorgeschlageneFrage(1, "UC006 Unvollständige KI-Frage?",
				List.of("v0", "v1", "v2", "v3", "", ""))));
		test(find(Button.class).withId("ki-anfordern").single()).click();

		assertThat(fragen(1)).containsExactly("UC006 Unvollständige KI-Frage?");
		assertThat(test(handlungsfeldGrids().get(0)).getCellComponent(0, 2).getElement().getTextRecursively())
			.isEqualTo("4/6 Stufen");
		test(find(Button.class).withId("entwurf-speichern").single()).click();
		assertThat(fehlerMeldungen()).anyMatch(text -> text.contains("Vorgabe fehlt."));
		assertThat(this.jdbc.sql("select count(*) from fragenkatalog where rolle_id = 9101").query(Long.class).single())
			.isZero();
	}

	@Test
	@UseCase(id = "UC-006", scenario = "A6: Genügend Fragen vorhanden", businessRules = { "GR-010" })
	void genuegend_fragen_ruft_das_ki_modell_nicht_auf_und_bietet_weitere_fragen_an() {
		navigate(FragenkatalogUndEntscheidungsmodellErstellenView.class);
		test(aktion(LEER)).click();
		test(find(Button.class).withId("ki-anfordern").single()).click();
		assertThat(fragenGesamt()).isEqualTo(18);

		test(find(Button.class).withId("ki-anfordern").single()).click();

		// GR-010: jedes Handlungsfeld hat schon 3 Fragen: kein weiterer Aufruf, stattdessen der Hinweis
		assertThat(KI.anfragen()).hasSize(1);
		assertThat(find(Dialog.class).single().getHeaderTitle()).isEqualTo("Genügend Fragen vorhanden");
		assertThat(find(Paragraph.class).all().stream().map(p -> p.getText()))
			.anyMatch(text -> text.contains("Jedes Handlungsfeld hat bereits mindestens 3 Fragen."));

		// A6 Schritt 2 und 3: weitere Fragen je Handlungsfeld
		find(IntegerField.class).withId("weitere-anzahl").single().setValue(2);
		test(find(Button.class).withId("genug-ergaenzen").single()).click();

		assertThat(KI.anfragen()).hasSize(2);
		assertThat(KI.anfragen().get(1).handlungsfelder()).hasSize(6).allMatch(h -> h.anzahl() == 2);
		assertThat(fragenGesamt()).isEqualTo(30);
	}

	@Test
	@UseCase(id = "UC-006", scenario = "A6: Genügend Fragen vorhanden", businessRules = { "GR-003" })
	void zahl_weiterer_fragen_ausserhalb_von_1_bis_10_wird_gemeldet() {
		navigate(FragenkatalogUndEntscheidungsmodellErstellenView.class);
		test(aktion(LEER)).click();
		test(find(Button.class).withId("ki-anfordern").single()).click();
		test(find(Button.class).withId("ki-anfordern").single()).click();
		IntegerField weitere = find(IntegerField.class).withId("weitere-anzahl").single();

		for (int ungueltig : new int[] { 0, 11 }) {
			weitere.setValue(ungueltig);
			test(find(Button.class).withId("genug-ergaenzen").single()).click();
			assertThat(weitere.isInvalid()).isTrue();
			assertThat(weitere.getErrorMessage()).isEqualTo("Die Zahl muss zwischen 1 und 10 liegen.");
		}

		// der Dialog bleibt offen, das KI-Modell wurde nur für den ersten Entwurf gefragt
		assertThat(find(Dialog.class).single().isOpened()).isTrue();
		assertThat(KI.anfragen()).hasSize(1);
	}

	@Test
	@UseCase(id = "UC-006", scenario = "A7: Administrator bricht die KI-Anforderung ab")
	void abbruch_nach_dem_hinweis_laesst_den_entwurf_unveraendert() {
		navigate(FragenkatalogUndEntscheidungsmodellErstellenView.class);
		test(aktion(LEER)).click();
		test(find(Button.class).withId("ki-anfordern").single()).click();
		test(find(Button.class).withId("ki-anfordern").single()).click();

		test(find(Button.class).withId("genug-abbrechen").single()).click();

		assertThat(find(Dialog.class).all().stream().noneMatch(Dialog::isOpened)).isTrue();
		assertThat(KI.anfragen()).hasSize(1);
		assertThat(fragenGesamt()).isEqualTo(18);
	}

	// ---------------------------------------------------------------- GR-009

	@Test
	@WithAnonymousUser
	@UseCase(id = "UC-006", businessRules = { "GR-009" }, requirements = { "NFR-005" })
	void aufruf_ohne_anmeldung_wird_abgewiesen() {
		UI.getCurrent().navigate("fragenkatalog-erstellen");

		assertThat(find(FragenkatalogUndEntscheidungsmodellErstellenView.class).exists()).isFalse();
	}

	@Test
	@WithMockUser(roles = "TEAMLEITUNG")
	@UseCase(id = "UC-006", businessRules = { "GR-009" }, requirements = { "NFR-005" })
	void aufruf_ohne_passende_berechtigung_wird_abgewiesen() {
		UI.getCurrent().navigate("fragenkatalog-erstellen");

		assertThat(find(FragenkatalogUndEntscheidungsmodellErstellenView.class).exists()).isFalse();
	}

	@Test
	@UseCase(id = "UC-006", businessRules = { "GR-009" }, requirements = { "NFR-005" })
	void administrator_erreicht_dieselbe_route() {
		UI.getCurrent().navigate("fragenkatalog-erstellen");

		assertThat(find(FragenkatalogUndEntscheidungsmodellErstellenView.class).exists()).isTrue();
	}

	// ---------------------------------------------------------------- Hilfen

	@SuppressWarnings("unchecked")
	private Grid<RolleListItem> rolleGrid() {
		return find(Grid.class).withId("rolle-grid").single();
	}

	private int zeile(String rolle) {
		List<RolleListItem> rollen = rolleGrid().getListDataView().getItems().toList();
		return rollen.indexOf(rollen.stream().filter(r -> r.name().equals(rolle)).findFirst().orElseThrow());
	}

	private Button aktion(String rolle) {
		return (Button) test(rolleGrid()).getCellComponent(zeile(rolle), 3);
	}

	/** Text der Komponente in Spalte 1 (freigegebene Version) oder 2 (Entwurf); leer ohne Eintrag. */
	private String zelle(String rolle, int spalte) {
		return test(rolleGrid()).getCellComponent(zeile(rolle), spalte).getElement().getTextRecursively();
	}

	private List<Grid<?>> handlungsfeldGrids() {
		return find(Grid.class).all().stream().<Grid<?>>map(grid -> grid).toList();
	}

	private List<String> fragen(int handlungsfeld) {
		Grid<?> grid = handlungsfeldGrids().get(handlungsfeld - 1);
		return java.util.stream.IntStream.range(0, test(grid).size()).mapToObj(i -> test(grid).getCellText(i, 1)).toList();
	}

	private int fragenGesamt() {
		return handlungsfeldGrids().stream().mapToInt(grid -> test(grid).size()).sum();
	}

	private String untertitel() {
		return find(Span.class).withClassName("devace-untertitel").all().get(0).getText();
	}

	@SuppressWarnings("unchecked")
	private void auswaehlen(Ausgangspunkt punkt) {
		RadioButtonGroup<Ausgangspunkt> auswahl = find(RadioButtonGroup.class).withId("ausgangspunkt").single();
		auswahl.setValue(punkt);
		test(find(Button.class).withId("ausgangspunkt-uebernehmen").single()).click();
	}

	private void fuelleFrage(String wortlaut, String... vorgaben) {
		test(find(TextArea.class).withId("frage-text").single()).setValue(wortlaut);
		for (int stufe = 0; stufe < vorgaben.length; stufe++) {
			test(find(TextArea.class).withId("vorgabe-" + stufe).single()).setValue(vorgaben[stufe]);
		}
	}

	private List<String> fehlerMeldungen() {
		return find(Div.class).withAttribute("theme", "fehler")
			.all()
			.stream()
			.map(div -> div.getElement().getTextRecursively())
			.toList();
	}

}
