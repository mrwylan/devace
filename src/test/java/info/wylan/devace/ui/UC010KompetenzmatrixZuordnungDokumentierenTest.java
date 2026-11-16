package info.wylan.devace.ui;

import java.util.List;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import info.wylan.devace.TestcontainersConfiguration;
import info.wylan.devace.persistence.MatrixZuordnungListItem;
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
 * UC-010 Kompetenzmatrix-Zuordnung dokumentieren, getestet über die Ansicht mit echter Datenbank.
 *
 * <p>
 * Testdaten (R__uc010_kompetenzmatrix.sql): „UC010 Docker“ in Handlungsfeld 2 mit Begründung, „UC010 Scrum“ in
 * Handlungsfeld 5 ohne Begründung, „UC010 Zum Ändern“ in Handlungsfeld 1 und „UC010 Zum Entfernen“ in Handlungsfeld 3.
 * Tests legen Spalten mit „UC010T “ an und entfernen sie danach; geänderte und entfernte Zeilen der Testdaten werden
 * wiederhergestellt.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@WithMockUser(username = "uc010-admin", roles = "ADMINISTRATOR")
class UC010KompetenzmatrixZuordnungDokumentierenTest extends SpringBrowserlessTest {

	private static final String HF2 = "2 Architektur & Engineering";

	private static final String HF4 = "4 Security & DevSecOps";

	private static final String HF6 = "6 Future Skills & AI";

	@Autowired
	private JdbcClient jdbc;

	@AfterEach
	void testdatenWiederherstellen() {
		this.jdbc.sql("delete from matrix_zuordnung where matrix_spalte like 'UC010T%'").update();
		this.jdbc.sql("""
				update matrix_zuordnung set matrix_spalte = 'UC010 Zum Ändern', begruendung = 'UC010 Alte Begründung',
				handlungsfeld_id = (select id from handlungsfeld where nummer = 1) where id = 10003
				""").update();
		this.jdbc.sql("""
				insert into matrix_zuordnung (id, handlungsfeld_id, matrix_spalte, begruendung)
				values (10004, (select id from handlungsfeld where nummer = 3), 'UC010 Zum Entfernen', null)
				on conflict do nothing
				""").update();
	}

	// ---------------------------------------------------------------- Hauptablauf

	@Test
	@UseCase(id = "UC-010", scenario = "Main Success Scenario")
	void zeigt_die_vorhandenen_zuordnungen_je_handlungsfeld() {
		navigate(KompetenzmatrixZuordnungDokumentierenView.class);

		assertThat(spalten(2)).contains("UC010 Docker").doesNotContain("UC010 Scrum");
		assertThat(spalten(5)).contains("UC010 Scrum").doesNotContain("UC010 Docker");
		Grid<MatrixZuordnungListItem> grid = grid(2);
		int zeile = zeile(grid, "UC010 Docker");
		assertThat(test(grid).getCellText(zeile, 1)).isEqualTo("UC010 Container im Alltag");
		assertThat(find(Grid.class).all()).hasSize(6);
	}

	@Test
	@UseCase(id = "UC-010", scenario = "Main Success Scenario", businessRules = { "BR-001" })
	void legt_eine_zuordnung_an_und_zeigt_sie_beim_handlungsfeld() {
		navigate(KompetenzmatrixZuordnungDokumentierenView.class);

		test(button("matrix-anlegen")).click();
		eingeben("UC010T Kubernetes", HF4, "UC010T Betrieb von Containern");
		test(button("matrix-speichern")).click();

		assertThat(find(Dialog.class).exists()).isFalse();
		assertThat(spalten(4)).contains("UC010T Kubernetes");
		// Post-S-1: gespeichert, wie der Administrator es angegeben hat
		assertThat(this.jdbc.sql("""
				select h.nummer || '|' || m.begruendung from matrix_zuordnung m
				join handlungsfeld h on h.id = m.handlungsfeld_id where m.matrix_spalte = 'UC010T Kubernetes'
				""").query(String.class).single()).isEqualTo("4|UC010T Betrieb von Containern");
	}

	@Test
	@UseCase(id = "UC-010", scenario = "Main Success Scenario", businessRules = { "BR-001" })
	void legt_eine_zuordnung_ohne_begruendung_an() {
		navigate(KompetenzmatrixZuordnungDokumentierenView.class);

		test(button("matrix-anlegen")).click();
		eingeben("UC010T Ohne Begründung", HF4, "");
		test(button("matrix-speichern")).click();

		assertThat(find(Dialog.class).exists()).isFalse();
		assertThat(this.jdbc.sql("select count(*) from matrix_zuordnung where matrix_spalte = 'UC010T Ohne Begründung'")
			.query(Integer.class)
			.single()).isEqualTo(1);
	}

	@Test
	@UseCase(id = "UC-010", scenario = "Main Success Scenario", businessRules = { "BR-001", "BR-002" })
	void aendert_spalte_handlungsfeld_und_begruendung_einer_zuordnung() {
		navigate(KompetenzmatrixZuordnungDokumentierenView.class);

		test(zeilenButton("matrix-aendern-10003")).click();
		assertThat(feld("matrix-spalte").getValue()).isEqualTo("UC010 Zum Ändern");
		eingeben("UC010 Geändert", HF6, "UC010 Neue Begründung");
		test(button("matrix-speichern")).click();

		assertThat(find(Dialog.class).exists()).isFalse();
		assertThat(spalten(6)).contains("UC010 Geändert");
		assertThat(spalten(1)).doesNotContain("UC010 Zum Ändern");
		assertThat(zeile10003()).isEqualTo("6|UC010 Geändert|UC010 Neue Begründung");
	}

	@Test
	@UseCase(id = "UC-010", scenario = "Main Success Scenario", businessRules = { "BR-002" })
	void aendern_nur_der_begruendung_behaelt_die_eigene_spalte() {
		navigate(KompetenzmatrixZuordnungDokumentierenView.class);

		test(zeilenButton("matrix-aendern-10003")).click();
		test(textArea("matrix-begruendung")).setValue("UC010 Nur die Begründung");
		test(button("matrix-speichern")).click();

		assertThat(find(Dialog.class).exists()).isFalse();
		assertThat(zeile10003()).isEqualTo("1|UC010 Zum Ändern|UC010 Nur die Begründung");
	}

	@Test
	@UseCase(id = "UC-010", scenario = "Main Success Scenario")
	void entfernt_eine_zuordnung_nach_bestaetigung() {
		navigate(KompetenzmatrixZuordnungDokumentierenView.class);

		test(zeilenButton("matrix-entfernen-10004")).click();
		ConfirmDialog bestaetigung = find(ConfirmDialog.class).single();
		test(bestaetigung).confirm();

		assertThat(spalten(3)).doesNotContain("UC010 Zum Entfernen");
		assertThat(this.jdbc.sql("select count(*) from matrix_zuordnung where id = 10004")
			.query(Integer.class)
			.single()).isZero();
	}

	// ---------------------------------------------------------------- A1

	@Test
	@UseCase(id = "UC-010", scenario = "A1: Angaben fehlen oder sind ungültig", businessRules = { "BR-001" })
	void ohne_namen_der_spalte_wird_nichts_gespeichert() {
		navigate(KompetenzmatrixZuordnungDokumentierenView.class);

		test(button("matrix-anlegen")).click();
		eingeben("", HF4, "UC010T A1 ohne Namen");
		test(button("matrix-speichern")).click();

		assertThat(feld("matrix-spalte").isInvalid()).isTrue();
		assertThat(feld("matrix-spalte").getErrorMessage()).isEqualTo("Pflichtangabe fehlt.");
		assertThat(find(Dialog.class).exists()).isTrue();
		assertThat(anzahlMitBegruendung("UC010T A1 ohne Namen")).isZero();
	}

	@Test
	@UseCase(id = "UC-010", scenario = "A1: Angaben fehlen oder sind ungültig", businessRules = { "BR-001" })
	void ohne_handlungsfeld_wird_nichts_gespeichert() {
		navigate(KompetenzmatrixZuordnungDokumentierenView.class);

		test(button("matrix-anlegen")).click();
		test(feld("matrix-spalte")).setValue("UC010T Ohne Handlungsfeld");
		test(button("matrix-speichern")).click();

		ComboBox<?> handlungsfeld = find(ComboBox.class).id("matrix-handlungsfeld");
		assertThat(handlungsfeld.isInvalid()).isTrue();
		assertThat(handlungsfeld.getErrorMessage()).isEqualTo("Pflichtangabe fehlt.");
		assertThat(anzahlSpalte("UC010T Ohne Handlungsfeld")).isZero();
	}

	@Test
	@UseCase(id = "UC-010", scenario = "A1: Angaben fehlen oder sind ungültig", businessRules = { "BR-001" })
	void name_der_spalte_mit_201_zeichen_wird_abgewiesen() {
		navigate(KompetenzmatrixZuordnungDokumentierenView.class);
		String zuLang = "UC010T " + "x".repeat(194);

		test(button("matrix-anlegen")).click();
		eingeben(zuLang, HF4, "");
		test(button("matrix-speichern")).click();

		assertThat(zuLang).hasSize(201);
		assertThat(feld("matrix-spalte").getErrorMessage()).isEqualTo("Höchstens 200 Zeichen.");
		assertThat(anzahlSpalte(zuLang)).isZero();
	}

	@Test
	@UseCase(id = "UC-010", scenario = "Main Success Scenario", businessRules = { "BR-001" })
	void name_der_spalte_mit_200_zeichen_wird_gespeichert() {
		navigate(KompetenzmatrixZuordnungDokumentierenView.class);
		String grenze = "UC010T " + "x".repeat(193);

		test(button("matrix-anlegen")).click();
		eingeben(grenze, HF4, "");
		test(button("matrix-speichern")).click();

		assertThat(grenze).hasSize(200);
		assertThat(anzahlSpalte(grenze)).isEqualTo(1);
	}

	@Test
	@UseCase(id = "UC-010", scenario = "A1: Angaben fehlen oder sind ungültig", businessRules = { "BR-001" })
	void begruendung_mit_501_zeichen_wird_abgewiesen() {
		navigate(KompetenzmatrixZuordnungDokumentierenView.class);

		test(button("matrix-anlegen")).click();
		eingeben("UC010T Lange Begründung", HF4, "b".repeat(501));
		test(button("matrix-speichern")).click();

		assertThat(textArea("matrix-begruendung").getErrorMessage()).isEqualTo("Höchstens 500 Zeichen.");
		assertThat(anzahlSpalte("UC010T Lange Begründung")).isZero();
	}

	@Test
	@UseCase(id = "UC-010", scenario = "Main Success Scenario", businessRules = { "BR-001" })
	void begruendung_mit_500_zeichen_wird_gespeichert() {
		navigate(KompetenzmatrixZuordnungDokumentierenView.class);

		test(button("matrix-anlegen")).click();
		eingeben("UC010T Begründung an der Grenze", HF4, "b".repeat(500));
		test(button("matrix-speichern")).click();

		assertThat(anzahlSpalte("UC010T Begründung an der Grenze")).isEqualTo(1);
	}

	// ---------------------------------------------------------------- A2

	@Test
	@UseCase(id = "UC-010", scenario = "A2: Spalte bereits zugeordnet", businessRules = { "BR-002" })
	void bereits_zugeordnete_spalte_in_anderer_schreibweise_wird_abgewiesen() {
		navigate(KompetenzmatrixZuordnungDokumentierenView.class);

		test(button("matrix-anlegen")).click();
		eingeben("  uc010 DOCKER  ", HF4, "UC010T A2");
		test(button("matrix-speichern")).click();

		assertThat(feld("matrix-spalte").isInvalid()).isTrue();
		assertThat(feld("matrix-spalte").getErrorMessage())
			.isEqualTo("Diese Spalte ist bereits dem Handlungsfeld 2 Architektur & Engineering zugeordnet.");
		assertThat(find(Dialog.class).exists()).isTrue();
		// Post-F-1, Post-F-2: unverändert, die Spalte bleibt bei genau einem Handlungsfeld
		assertThat(anzahlMitBegruendung("UC010T A2")).isZero();
		assertThat(this.jdbc.sql("select count(*) from matrix_zuordnung where lower(trim(matrix_spalte)) = 'uc010 docker'")
			.query(Integer.class)
			.single()).isEqualTo(1);
	}

	@Test
	@UseCase(id = "UC-010", scenario = "A2: Spalte bereits zugeordnet", businessRules = { "BR-002" })
	void aendern_auf_eine_bereits_zugeordnete_spalte_wird_abgewiesen() {
		navigate(KompetenzmatrixZuordnungDokumentierenView.class);

		test(zeilenButton("matrix-aendern-10003")).click();
		eingeben("UC010 Scrum", HF6, "UC010 Neue Begründung");
		test(button("matrix-speichern")).click();

		assertThat(feld("matrix-spalte").getErrorMessage())
			.isEqualTo("Diese Spalte ist bereits dem Handlungsfeld 5 Methodik & Agile Delivery zugeordnet.");
		assertThat(zeile10003()).isEqualTo("1|UC010 Zum Ändern|UC010 Alte Begründung");
	}

	// ---------------------------------------------------------------- A3

	@Test
	@UseCase(id = "UC-010", scenario = "A3: Administrator bricht ab")
	void abbrechen_beim_aendern_verwirft_die_angaben() {
		navigate(KompetenzmatrixZuordnungDokumentierenView.class);

		test(zeilenButton("matrix-aendern-10003")).click();
		eingeben("UC010 Verworfen", HF6, "UC010 Verworfene Begründung");
		test(button("matrix-abbrechen")).click();

		assertThat(find(Dialog.class).exists()).isFalse();
		assertThat(zeile10003()).isEqualTo("1|UC010 Zum Ändern|UC010 Alte Begründung");
	}

	@Test
	@UseCase(id = "UC-010", scenario = "A3: Administrator bricht ab")
	void abbrechen_beim_anlegen_speichert_nichts() {
		navigate(KompetenzmatrixZuordnungDokumentierenView.class);

		test(button("matrix-anlegen")).click();
		eingeben("UC010T Verworfen", HF4, "");
		test(button("matrix-abbrechen")).click();

		assertThat(anzahlSpalte("UC010T Verworfen")).isZero();
	}

	@Test
	@UseCase(id = "UC-010", scenario = "A3: Administrator bricht ab")
	void abbrechen_beim_entfernen_behaelt_die_zuordnung() {
		navigate(KompetenzmatrixZuordnungDokumentierenView.class);

		test(zeilenButton("matrix-entfernen-10004")).click();
		test(find(ConfirmDialog.class).single()).cancel();

		assertThat(spalten(3)).contains("UC010 Zum Entfernen");
		assertThat(this.jdbc.sql("select count(*) from matrix_zuordnung where id = 10004")
			.query(Integer.class)
			.single()).isEqualTo(1);
	}

	// ---------------------------------------------------------------- BR-003

	@Test
	@UseCase(id = "UC-010", businessRules = { "BR-003" }, requirements = { "C-007", "C-008" })
	void gespeichert_werden_nur_spalte_handlungsfeld_und_begruendung() {
		// keine Werte der Matrix, keine Anbindung: die Zuordnung hält nur diese Angaben
		assertThat(this.jdbc.sql("""
				select column_name from information_schema.columns
				where table_name = 'matrix_zuordnung' order by column_name
				""").query(String.class).list())
			.containsExactly("begruendung", "handlungsfeld_id", "id", "matrix_spalte");
	}

	// ---------------------------------------------------------------- BR-004

	@Test
	@WithAnonymousUser
	@UseCase(id = "UC-010", businessRules = { "BR-004" }, requirements = { "NFR-005" })
	void aufruf_ohne_anmeldung_wird_abgewiesen() {
		UI.getCurrent().navigate("kompetenzmatrix");

		assertThat(find(KompetenzmatrixZuordnungDokumentierenView.class).exists()).isFalse();
	}

	@Test
	@WithMockUser(roles = "TEAMLEITUNG")
	@UseCase(id = "UC-010", businessRules = { "BR-004" }, requirements = { "NFR-005" })
	void aufruf_ohne_passende_berechtigung_wird_abgewiesen() {
		UI.getCurrent().navigate("kompetenzmatrix");

		assertThat(find(KompetenzmatrixZuordnungDokumentierenView.class).exists()).isFalse();
	}

	@Test
	@UseCase(id = "UC-010", businessRules = { "BR-004" }, requirements = { "NFR-005" })
	void administrator_erreicht_die_zuordnung_und_der_eintrag_ist_aktiv() {
		UI.getCurrent().navigate("kompetenzmatrix");

		assertThat(find(KompetenzmatrixZuordnungDokumentierenView.class).exists()).isTrue();
		assertThat(navigationseintrag("Kompetenzmatrix").isEnabled()).isTrue();
	}

	@Test
	@WithMockUser(username = "uc001-teamleitung", roles = "TEAMLEITUNG")
	@UseCase(id = "UC-010", businessRules = { "BR-004" }, requirements = { "NFR-005" })
	void navigation_zeigt_den_eintrag_ohne_berechtigung_deaktiviert() {
		navigate(EinladungErstellenUndVersendenView.class);

		assertThat(navigationseintrag("Kompetenzmatrix").isEnabled()).isFalse();
	}

	// ---------------------------------------------------------------- Hilfen

	private void eingeben(String spalte, String handlungsfeld, String begruendung) {
		test(feld("matrix-spalte")).setValue(spalte);
		test(find(ComboBox.class).id("matrix-handlungsfeld")).selectItem(handlungsfeld);
		test(textArea("matrix-begruendung")).setValue(begruendung);
	}

	private Button button(String id) {
		return find(Button.class).id(id);
	}

	/** Die Schaltflächen einer Zeile liegen in der letzten Spalte ihres Grids, nicht im Komponentenbaum. */
	private Button zeilenButton(String id) {
		for (int nummer = 1; nummer <= 6; nummer++) {
			Grid<MatrixZuordnungListItem> grid = grid(nummer);
			for (int i = 0; i < test(grid).size(); i++) {
				java.util.Optional<Button> button = test(grid).getCellComponent(i, 2)
					.getChildren()
					.filter((kind) -> kind instanceof Button && id.equals(kind.getId().orElse(null)))
					.map(Button.class::cast)
					.findFirst();
				if (button.isPresent()) {
					return button.get();
				}
			}
		}
		throw new AssertionError("Schaltfläche nicht gefunden: " + id);
	}

	private TextField feld(String id) {
		return find(TextField.class).id(id);
	}

	private TextArea textArea(String id) {
		return find(TextArea.class).id(id);
	}

	@SuppressWarnings("unchecked")
	private Grid<MatrixZuordnungListItem> grid(int nummer) {
		return find(Grid.class).id("matrix-grid-" + nummer);
	}

	private List<String> spalten(int nummer) {
		Grid<MatrixZuordnungListItem> grid = grid(nummer);
		int anzahl = test(grid).size();
		return java.util.stream.IntStream.range(0, anzahl).mapToObj((i) -> test(grid).getCellText(i, 0)).toList();
	}

	private int zeile(Grid<MatrixZuordnungListItem> grid, String spalte) {
		for (int i = 0; i < test(grid).size(); i++) {
			if (spalte.equals(test(grid).getCellText(i, 0))) {
				return i;
			}
		}
		throw new AssertionError("Spalte nicht gefunden: " + spalte);
	}

	private String zeile10003() {
		return this.jdbc.sql("""
				select h.nummer || '|' || m.matrix_spalte || '|' || coalesce(m.begruendung, '') from matrix_zuordnung m
				join handlungsfeld h on h.id = m.handlungsfeld_id where m.id = 10003
				""").query(String.class).single();
	}

	private int anzahlSpalte(String spalte) {
		return this.jdbc.sql("select count(*) from matrix_zuordnung where matrix_spalte = ?")
			.param(spalte)
			.query(Integer.class)
			.single();
	}

	private int anzahlMitBegruendung(String begruendung) {
		return this.jdbc.sql("select count(*) from matrix_zuordnung where begruendung = ?")
			.param(begruendung)
			.query(Integer.class)
			.single();
	}

	private SideNavItem navigationseintrag(String beschriftung) {
		return find(SideNavItem.class).all()
			.stream()
			.filter((eintrag) -> beschriftung.equals(eintrag.getLabel()))
			.findFirst()
			.orElseThrow();
	}

}
