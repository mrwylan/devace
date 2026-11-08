package info.wylan.devace.ui;

import java.util.List;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.TextField;
import info.wylan.devace.TestcontainersConfiguration;
import info.wylan.devace.persistence.MagListItem;
import info.wylan.devace.persistence.RolleListItem;
import info.wylan.devace.usecase.UseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UC-008 MAGs und Rollen verwalten, getestet über die Ansicht mit echter Datenbank.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@WithMockUser(roles = "ADMINISTRATOR")
class UC008MagsUndRollenVerwaltenTest extends SpringBrowserlessTest {

	private static final String PRAEFIX = "UC008T";

	@Autowired
	private JdbcClient jdbc;

	@AfterEach
	void aufraeumen() {
		// die eine umbenannte Testdaten-Rolle zurücksetzen, dann nur löschen, was die Tests selbst angelegt haben
		this.jdbc.sql("update rolle set name = 'UC008 Umbenennbar' where id = 9002").update();
		this.jdbc.sql("delete from mag where scrum_team like 'UC008T%' or linienteam like 'UC008T%'").update();
		this.jdbc.sql("delete from rolle where name like 'UC008T%'").update();
	}

	// ---------------------------------------------------------------- Hauptablauf

	@Test
	@UseCase(id = "UC-008", scenario = "Steps 1-2: Übersicht")
	void uebersicht_zeigt_mags_mit_teamleitung_und_rollen_mit_fragenkatalogen() {
		navigate(MagsUndRollenView.class);

		Grid<MagListItem> magGrid = magGrid();
		MagListItem mitLeitung = magZeile("UC008 Alpha", "UC008 Nord");
		MagListItem ohneLeitung = magZeile("UC008 Beta", "UC008 Süd");
		assertThat(mitLeitung.teamleitung()).isEqualTo("uc008-teamleitung-a");
		assertThat(ohneLeitung.teamleitung()).isNull();
		assertThat(test(magGrid).getCellText(magGrid.getListDataView().getItems().toList().indexOf(ohneLeitung), 2))
			.isEqualTo("–");

		Grid<RolleListItem> rolleGrid = rolleGrid();
		List<RolleListItem> rollen = rolleGrid.getListDataView().getItems().toList();
		int index = rollen.indexOf(rollen.stream().filter(r -> r.name().equals("UC008 Entwickler")).findFirst().get());
		assertThat(test(rolleGrid).getCellText(index, 1)).contains("Version 1 (Freigegeben)")
			.contains("Version 2 (Entwurf)");
	}

	@Test
	@UseCase(id = "UC-008", scenario = "Step 2: Teamleitungen zur Zuordnung")
	void teamleitungen_werden_zur_zuordnung_angeboten() {
		navigate(MagsUndRollenView.class);
		test(find(Button.class).withId("mag-anlegen").single()).click();

		ComboBox<?> combo = find(ComboBox.class).withId("mag-teamleitung").single();
		assertThat(combo.getListDataView().getItems().map(Object::toString).toList())
			.anyMatch(s -> s.contains("uc008-teamleitung-a"))
			.anyMatch(s -> s.contains("uc008-teamleitung-b"));
	}

	@Test
	@UseCase(id = "UC-008", scenario = "Steps 3-8: MAG anlegen", businessRules = { "GR-004" })
	void mag_mit_teamleitung_anlegen() {
		navigate(MagsUndRollenView.class);
		test(find(Button.class).withId("mag-anlegen").single()).click();
		test(find(TextField.class).withId("mag-scrum-team").single()).setValue(PRAEFIX + " Gamma");
		test(find(TextField.class).withId("mag-linienteam").single()).setValue(PRAEFIX + " West");
		test(find(ComboBox.class).withId("mag-teamleitung").single()).selectItem("uc008-teamleitung-b");
		test(find(Button.class).withId("mag-speichern").single()).click();

		assertThat(find(Dialog.class).exists()).isFalse();
		MagListItem zeile = magZeile(PRAEFIX + " Gamma", PRAEFIX + " West");
		assertThat(zeile.teamleitung()).isEqualTo("uc008-teamleitung-b");
		assertThat(this.jdbc.sql("select teamleitung_id from mag where scrum_team = ?")
			.param(PRAEFIX + " Gamma")
			.query(Long.class)
			.single()).isEqualTo(9002L);
	}

	@Test
	@UseCase(id = "UC-008", scenario = "Steps 3-8: MAG ohne Teamleitung anlegen", businessRules = { "GR-004" })
	void mag_ohne_teamleitung_ist_zulaessig() {
		navigate(MagsUndRollenView.class);
		test(find(Button.class).withId("mag-anlegen").single()).click();
		test(find(TextField.class).withId("mag-scrum-team").single()).setValue(PRAEFIX + " Delta");
		test(find(TextField.class).withId("mag-linienteam").single()).setValue(PRAEFIX + " Ost");
		test(find(Button.class).withId("mag-speichern").single()).click();

		assertThat(find(Dialog.class).exists()).isFalse();
		assertThat(this.jdbc.sql("select count(*) from mag where scrum_team = ? and teamleitung_id is null")
			.param(PRAEFIX + " Delta")
			.query(Integer.class)
			.single()).isEqualTo(1);
	}

	@Test
	@UseCase(id = "UC-008", scenario = "Steps 3-8: Rolle anlegen")
	void rolle_anlegen() {
		navigate(MagsUndRollenView.class);
		test(find(Button.class).withId("rolle-anlegen").single()).click();
		test(find(TextField.class).withId("rolle-name").single()).setValue(PRAEFIX + " Tester");
		test(find(Button.class).withId("rolle-speichern").single()).click();

		assertThat(find(Dialog.class).exists()).isFalse();
		assertThat(rolleGrid().getListDataView().getItems().anyMatch(r -> r.name().equals(PRAEFIX + " Tester")))
			.isTrue();
		assertThat(this.jdbc.sql("select count(*) from rolle where name = ?")
			.param(PRAEFIX + " Tester")
			.query(Integer.class)
			.single()).isEqualTo(1);
	}

	@Test
	@UseCase(id = "UC-008", scenario = "Steps 3-8: MAG ändern", businessRules = { "GR-006" })
	void mag_aendern_behaelt_die_identitaet() {
		navigate(MagsUndRollenView.class);
		legeMagAn(PRAEFIX + " Eps", PRAEFIX + " Mitte");
		long idVorher = magZeile(PRAEFIX + " Eps", PRAEFIX + " Mitte").id();

		test(magAendernButton(PRAEFIX + " Eps", PRAEFIX + " Mitte")).click();
		test(find(TextField.class).withId("mag-scrum-team").single()).setValue(PRAEFIX + " Eps2");
		test(find(ComboBox.class).withId("mag-teamleitung").single()).selectItem("uc008-teamleitung-a");
		test(find(Button.class).withId("mag-speichern").single()).click();

		assertThat(find(Dialog.class).exists()).isFalse();
		MagListItem danach = magZeile(PRAEFIX + " Eps2", PRAEFIX + " Mitte");
		assertThat(danach.id()).isEqualTo(idVorher);
		assertThat(danach.teamleitung()).isEqualTo("uc008-teamleitung-a");
		assertThat(this.jdbc.sql("select count(*) from mag where scrum_team = ?")
			.param(PRAEFIX + " Eps")
			.query(Integer.class)
			.single()).isZero();
	}

	@Test
	@UseCase(id = "UC-008", scenario = "Steps 3-8: Rolle ändern", businessRules = { "GR-006" })
	void rolle_aendern_laesst_fragenkatalog_zugeordnet() {
		navigate(MagsUndRollenView.class);

		test(rolleAendernButton("UC008 Umbenennbar")).click();
		test(find(TextField.class).withId("rolle-name").single()).setValue(PRAEFIX + " Neu");
		test(find(Button.class).withId("rolle-speichern").single()).click();

		assertThat(find(Dialog.class).exists()).isFalse();
		assertThat(this.jdbc.sql("select name from rolle where id = 9002").query(String.class).single())
			.isEqualTo(PRAEFIX + " Neu");
		assertThat(this.jdbc.sql("select count(*) from fragenkatalog where rolle_id = 9002")
			.query(Integer.class)
			.single()).isEqualTo(1);
		RolleListItem zeile = rolleGrid().getListDataView()
			.getItems()
			.filter(r -> r.id() == 9002L)
			.findFirst()
			.orElseThrow();
		assertThat(zeile.name()).isEqualTo(PRAEFIX + " Neu");
		assertThat(zeile.fragenkataloge()).hasSize(1);
	}

	// ---------------------------------------------------------------- A1

	@Test
	@UseCase(id = "UC-008", scenario = "A1: Angaben fehlen oder sind ungültig", businessRules = { "GR-001" })
	void mag_ohne_scrum_team_wird_abgewiesen() {
		navigate(MagsUndRollenView.class);
		long vorher = anzahl("mag");
		test(find(Button.class).withId("mag-anlegen").single()).click();
		test(find(TextField.class).withId("mag-linienteam").single()).setValue(PRAEFIX + " X");
		test(find(Button.class).withId("mag-speichern").single()).click();

		TextField feld = find(TextField.class).withId("mag-scrum-team").single();
		assertThat(feld.isInvalid()).isTrue();
		assertThat(feld.getErrorMessage()).isEqualTo("Pflichtangabe fehlt.");
		assertThat(find(Dialog.class).exists()).isTrue();
		assertThat(anzahl("mag")).isEqualTo(vorher);
	}

	@Test
	@UseCase(id = "UC-008", scenario = "A1: Angaben fehlen oder sind ungültig", businessRules = { "GR-001" })
	void angabe_nur_aus_leerzeichen_gilt_als_fehlend() {
		navigate(MagsUndRollenView.class);
		long vorher = anzahl("rolle");
		test(find(Button.class).withId("rolle-anlegen").single()).click();
		test(find(TextField.class).withId("rolle-name").single()).setValue("   ");
		test(find(Button.class).withId("rolle-speichern").single()).click();

		TextField feld = find(TextField.class).withId("rolle-name").single();
		assertThat(feld.getErrorMessage()).isEqualTo("Pflichtangabe fehlt.");
		assertThat(anzahl("rolle")).isEqualTo(vorher);
	}

	@Test
	@UseCase(id = "UC-008", scenario = "A1: Angaben fehlen oder sind ungültig", businessRules = { "GR-001" })
	void mag_angaben_ueber_100_zeichen_werden_abgewiesen_100_sind_zulaessig() {
		navigate(MagsUndRollenView.class);
		test(find(Button.class).withId("mag-anlegen").single()).click();
		test(find(TextField.class).withId("mag-scrum-team").single()).setValue(PRAEFIX + "x".repeat(94));
		test(find(TextField.class).withId("mag-linienteam").single()).setValue(PRAEFIX + "y".repeat(94));
		test(find(Button.class).withId("mag-speichern").single()).click();
		assertThat(find(Dialog.class).exists()).isFalse();

		test(find(Button.class).withId("mag-anlegen").single()).click();
		test(find(TextField.class).withId("mag-scrum-team").single()).setValue(PRAEFIX + "z".repeat(96));
		test(find(TextField.class).withId("mag-linienteam").single()).setValue(PRAEFIX + "w".repeat(96));
		test(find(Button.class).withId("mag-speichern").single()).click();

		assertThat(find(TextField.class).withId("mag-scrum-team").single().getErrorMessage())
			.isEqualTo("Höchstens 100 Zeichen.");
		assertThat(find(TextField.class).withId("mag-linienteam").single().getErrorMessage())
			.isEqualTo("Höchstens 100 Zeichen.");
		assertThat(this.jdbc.sql("select count(*) from mag where scrum_team like ?")
			.param(PRAEFIX + "z%")
			.query(Integer.class)
			.single()).isZero();
	}

	@Test
	@UseCase(id = "UC-008", scenario = "A1: Angaben fehlen oder sind ungültig", businessRules = { "GR-001" })
	void rollenname_ueber_50_zeichen_wird_abgewiesen_50_sind_zulaessig() {
		navigate(MagsUndRollenView.class);
		test(find(Button.class).withId("rolle-anlegen").single()).click();
		test(find(TextField.class).withId("rolle-name").single()).setValue(PRAEFIX + "a".repeat(44));
		test(find(Button.class).withId("rolle-speichern").single()).click();
		assertThat(find(Dialog.class).exists()).isFalse();

		test(find(Button.class).withId("rolle-anlegen").single()).click();
		test(find(TextField.class).withId("rolle-name").single()).setValue(PRAEFIX + "b".repeat(45));
		test(find(Button.class).withId("rolle-speichern").single()).click();

		assertThat(find(TextField.class).withId("rolle-name").single().getErrorMessage())
			.isEqualTo("Höchstens 50 Zeichen.");
		assertThat(this.jdbc.sql("select count(*) from rolle where name like ?")
			.param(PRAEFIX + "b%")
			.query(Integer.class)
			.single()).isZero();
	}

	// ---------------------------------------------------------------- A2

	@Test
	@UseCase(id = "UC-008", scenario = "A2: MAG oder Rolle besteht bereits", businessRules = { "GR-002" })
	void doppelte_mag_ohne_beachtung_von_gross_kleinschreibung_und_leerzeichen_wird_abgewiesen() {
		navigate(MagsUndRollenView.class);
		long vorher = anzahl("mag");
		test(find(Button.class).withId("mag-anlegen").single()).click();
		test(find(TextField.class).withId("mag-scrum-team").single()).setValue("  uc008 ALPHA ");
		test(find(TextField.class).withId("mag-linienteam").single()).setValue("uc008 nord  ");
		test(find(Button.class).withId("mag-speichern").single()).click();

		TextField feld = find(TextField.class).withId("mag-linienteam").single();
		assertThat(feld.isInvalid()).isTrue();
		assertThat(feld.getErrorMessage()).isEqualTo("Diese MAG besteht bereits.");
		assertThat(find(Dialog.class).exists()).isTrue();
		assertThat(anzahl("mag")).isEqualTo(vorher);
	}

	@Test
	@UseCase(id = "UC-008", scenario = "A2: MAG oder Rolle besteht bereits", businessRules = { "GR-002" })
	void gleiches_scrum_team_mit_anderem_linienteam_ist_eine_andere_mag_und_nach_korrektur_wird_gespeichert() {
		navigate(MagsUndRollenView.class);
		test(find(Button.class).withId("mag-anlegen").single()).click();
		test(find(TextField.class).withId("mag-scrum-team").single()).setValue("UC008 Alpha");
		test(find(TextField.class).withId("mag-linienteam").single()).setValue("UC008 Nord");
		test(find(Button.class).withId("mag-speichern").single()).click();
		assertThat(find(TextField.class).withId("mag-linienteam").single().getErrorMessage())
			.isEqualTo("Diese MAG besteht bereits.");

		// Der Administrator korrigiert die Angabe; der Ablauf geht bei Schritt 4 weiter.
		test(find(TextField.class).withId("mag-scrum-team").single()).setValue(PRAEFIX + " Alpha");
		test(find(TextField.class).withId("mag-linienteam").single()).setValue("UC008 Nord");
		test(find(Button.class).withId("mag-speichern").single()).click();

		assertThat(find(Dialog.class).exists()).isFalse();
		assertThat(magZeile(PRAEFIX + " Alpha", "UC008 Nord")).isNotNull();
	}

	@Test
	@UseCase(id = "UC-008", scenario = "A2: MAG oder Rolle besteht bereits", businessRules = { "GR-002" })
	void mag_aendern_auf_vorhandene_kombination_wird_abgewiesen() {
		navigate(MagsUndRollenView.class);
		legeMagAn(PRAEFIX + " Zeta", PRAEFIX + " Mitte");
		test(magAendernButton(PRAEFIX + " Zeta", PRAEFIX + " Mitte")).click();
		test(find(TextField.class).withId("mag-scrum-team").single()).setValue("UC008 Alpha");
		test(find(TextField.class).withId("mag-linienteam").single()).setValue("UC008 Nord");
		test(find(Button.class).withId("mag-speichern").single()).click();

		assertThat(find(TextField.class).withId("mag-linienteam").single().getErrorMessage())
			.isEqualTo("Diese MAG besteht bereits.");
		assertThat(this.jdbc.sql("select count(*) from mag where scrum_team = ?")
			.param(PRAEFIX + " Zeta")
			.query(Integer.class)
			.single()).isEqualTo(1);
	}

	@Test
	@UseCase(id = "UC-008", scenario = "A2: MAG oder Rolle besteht bereits", businessRules = { "GR-003" })
	void doppelte_rolle_ohne_beachtung_von_gross_kleinschreibung_und_leerzeichen_wird_abgewiesen() {
		navigate(MagsUndRollenView.class);
		long vorher = anzahl("rolle");
		test(find(Button.class).withId("rolle-anlegen").single()).click();
		test(find(TextField.class).withId("rolle-name").single()).setValue("  uc008 ENTWICKLER ");
		test(find(Button.class).withId("rolle-speichern").single()).click();

		TextField feld = find(TextField.class).withId("rolle-name").single();
		assertThat(feld.isInvalid()).isTrue();
		assertThat(feld.getErrorMessage()).isEqualTo("Diese Rolle besteht bereits.");
		assertThat(anzahl("rolle")).isEqualTo(vorher);
	}

	@Test
	@UseCase(id = "UC-008", scenario = "A2: MAG oder Rolle besteht bereits", businessRules = { "GR-003" })
	void rolle_aendern_auf_vorhandenen_namen_wird_abgewiesen() {
		navigate(MagsUndRollenView.class);
		test(rolleAendernButton("UC008 Umbenennbar")).click();
		test(find(TextField.class).withId("rolle-name").single()).setValue("UC008 Entwickler");
		test(find(Button.class).withId("rolle-speichern").single()).click();

		assertThat(find(TextField.class).withId("rolle-name").single().getErrorMessage())
			.isEqualTo("Diese Rolle besteht bereits.");
		assertThat(this.jdbc.sql("select name from rolle where id = 9002").query(String.class).single())
			.isEqualTo("UC008 Umbenennbar");
	}

	// ---------------------------------------------------------------- A3

	@Test
	@UseCase(id = "UC-008", scenario = "A3: Administrator bricht ab")
	void abbrechen_verwirft_die_angaben() {
		navigate(MagsUndRollenView.class);
		long magVorher = anzahl("mag");
		test(find(Button.class).withId("mag-anlegen").single()).click();
		test(find(TextField.class).withId("mag-scrum-team").single()).setValue(PRAEFIX + " Abbruch");
		test(find(TextField.class).withId("mag-linienteam").single()).setValue(PRAEFIX + " Abbruch");
		test(find(Button.class).withId("mag-abbrechen").single()).click();
		assertThat(find(Dialog.class).exists()).isFalse();
		assertThat(anzahl("mag")).isEqualTo(magVorher);

		long rolleVorher = anzahl("rolle");
		test(find(Button.class).withId("rolle-anlegen").single()).click();
		test(find(TextField.class).withId("rolle-name").single()).setValue(PRAEFIX + " Abbruch");
		test(find(Button.class).withId("rolle-abbrechen").single()).click();
		assertThat(find(Dialog.class).exists()).isFalse();
		assertThat(anzahl("rolle")).isEqualTo(rolleVorher);
	}

	// ---------------------------------------------------------------- Regeln und Nachbedingungen

	@Test
	@UseCase(id = "UC-008", businessRules = { "GR-005" })
	void die_ansicht_bietet_kein_loeschen_an() {
		navigate(MagsUndRollenView.class);

		assertThat(find(Button.class).all().stream().map(Button::getText))
			.noneMatch(text -> text != null && text.toLowerCase().contains("lösch"));
	}

	@Test
	@UseCase(id = "UC-008", businessRules = { "GR-002", "GR-003" })
	void bestehende_eintraege_bleiben_nach_abgewiesenem_speichern_unveraendert() {
		navigate(MagsUndRollenView.class);
		test(find(Button.class).withId("rolle-anlegen").single()).click();
		test(find(TextField.class).withId("rolle-name").single()).setValue("UC008 Entwickler");
		test(find(Button.class).withId("rolle-speichern").single()).click();

		assertThat(this.jdbc.sql("select name from rolle where id = 9001").query(String.class).single())
			.isEqualTo("UC008 Entwickler");
		assertThat(this.jdbc.sql("select scrum_team || '/' || linienteam from mag where id = 9001")
			.query(String.class)
			.single()).isEqualTo("UC008 Alpha/UC008 Nord");
	}

	// ---------------------------------------------------------------- GR-007 / NFR-005

	@Test
	@WithAnonymousUser
	@UseCase(id = "UC-008", businessRules = { "GR-007" }, requirements = { "NFR-005" })
	void aufruf_ohne_anmeldung_wird_abgewiesen() {
		UI.getCurrent().navigate("mags-und-rollen");

		assertThat(find(MagsUndRollenView.class).exists()).isFalse();
	}

	@Test
	@WithMockUser(roles = "TEAMLEITUNG")
	@UseCase(id = "UC-008", businessRules = { "GR-007" }, requirements = { "NFR-005" })
	void aufruf_ohne_passende_berechtigung_wird_abgewiesen() {
		UI.getCurrent().navigate("mags-und-rollen");

		assertThat(find(MagsUndRollenView.class).exists()).isFalse();
	}

	@Test
	@UseCase(id = "UC-008", businessRules = { "GR-007" }, requirements = { "NFR-005" })
	void administrator_erreicht_dieselbe_route() {
		UI.getCurrent().navigate("mags-und-rollen");

		assertThat(find(MagsUndRollenView.class).exists()).isTrue();
	}

	// ---------------------------------------------------------------- Hilfsmethoden

	private void legeMagAn(String scrumTeam, String linienteam) {
		test(find(Button.class).withId("mag-anlegen").single()).click();
		test(find(TextField.class).withId("mag-scrum-team").single()).setValue(scrumTeam);
		test(find(TextField.class).withId("mag-linienteam").single()).setValue(linienteam);
		test(find(Button.class).withId("mag-speichern").single()).click();
	}

	@SuppressWarnings("unchecked")
	private Grid<MagListItem> magGrid() {
		return find(Grid.class).withId("mag-grid").single();
	}

	@SuppressWarnings("unchecked")
	private Grid<RolleListItem> rolleGrid() {
		return find(Grid.class).withId("rolle-grid").single();
	}

	private MagListItem magZeile(String scrumTeam, String linienteam) {
		return magGrid().getListDataView()
			.getItems()
			.filter(m -> m.scrumTeam().equals(scrumTeam) && m.linienteam().equals(linienteam))
			.findFirst()
			.orElseThrow(() -> new AssertionError("MAG nicht in der Übersicht: " + scrumTeam + "/" + linienteam));
	}

	private Button magAendernButton(String scrumTeam, String linienteam) {
		Grid<MagListItem> grid = magGrid();
		int index = grid.getListDataView().getItems().toList().indexOf(magZeile(scrumTeam, linienteam));
		return (Button) test(grid).getCellComponent(index, 3);
	}

	private Button rolleAendernButton(String name) {
		Grid<RolleListItem> grid = rolleGrid();
		List<RolleListItem> rollen = grid.getListDataView().getItems().toList();
		int index = rollen.indexOf(rollen.stream().filter(r -> r.name().equals(name)).findFirst().orElseThrow());
		return (Button) test(grid).getCellComponent(index, 2);
	}

	private long anzahl(String tabelle) {
		return this.jdbc.sql("select count(*) from " + tabelle).query(Long.class).single();
	}

}
