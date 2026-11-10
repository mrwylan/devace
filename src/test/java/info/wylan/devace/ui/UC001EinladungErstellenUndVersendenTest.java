package info.wylan.devace.ui;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import info.wylan.devace.TestcontainersConfiguration;
import info.wylan.devace.persistence.MagListItem;
import info.wylan.devace.persistence.RolleOption;
import info.wylan.devace.service.EmailSystemClient;
import info.wylan.devace.service.EmailSystemClient.Einladungsmail;
import info.wylan.devace.service.EmailSystemClientFake;
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
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UC-001 Einladung erstellen und versenden, getestet über die Ansicht mit echter Datenbank. Das E-Mail-System ist durch
 * {@link EmailSystemClientFake} ersetzt.
 *
 * <p>
 * Testdaten (R__uc001_einladungen.sql): Teamleitung 9201 mit der MAG "UC001 Alpha · UC001 Nord", eine fremde MAG 9202,
 * die Rolle "UC001 Entwicklung" mit freigegebenem Fragenkatalog und die Rolle "UC001 Ohne Freigabe" nur mit Entwurf.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@WithMockUser(username = "uc001-teamleitung", roles = "TEAMLEITUNG")
class UC001EinladungErstellenUndVersendenTest extends SpringBrowserlessTest {

	private static final EmailSystemClientFake MAIL = new EmailSystemClientFake();

	private static final String BETREFF = "Einladung zum devace-Assessment";

	@TestBean
	EmailSystemClient emailSystemClient;

	@Autowired
	private JdbcClient jdbc;

	static EmailSystemClient emailSystemClient() {
		return MAIL;
	}

	@BeforeEach
	void zuruecksetzen() {
		MAIL.reset();
	}

	@AfterEach
	void aufraeumen() {
		this.jdbc.sql("delete from einladung where erstellt_von_id in (9201, 9202)").update();
	}

	// ---------------------------------------------------------------- Schritte 1 bis 4

	@Test
	@UseCase(id = "UC-001", scenario = "Steps 1-2: Auswahl", businessRules = { "BR-002", "BR-003" })
	@SuppressWarnings("unchecked")
	void zeigt_nur_eigene_mags_und_rollen_mit_freigegebenem_fragenkatalog() {
		navigate(EinladungErstellenUndVersendenView.class);

		Select<MagListItem> mag = find(Select.class).withId("einladung-mag").single();
		Select<RolleOption> rolle = find(Select.class).withId("einladung-rolle").single();
		List<String> mags = mag.getListDataView().getItems().map(mag.getItemLabelGenerator()::apply).toList();
		List<String> rollen = rolle.getListDataView().getItems().map(rolle.getItemLabelGenerator()::apply).toList();
		// BR-002: nur die MAG der angemeldeten Teamleitung, nicht die einer anderen
		assertThat(mags).containsExactly("UC001 Alpha · UC001 Nord");
		// BR-003: nur Rollen mit freigegebenem Fragenkatalog
		assertThat(rollen).contains("UC001 Entwicklung").doesNotContain("UC001 Ohne Freigabe");
	}

	@Test
	@UseCase(id = "UC-001", scenario = "Step 4: Angaben", businessRules = { "BR-008" })
	void schlaegt_30_tage_gueltigkeit_vor() {
		navigate(EinladungErstellenUndVersendenView.class);

		assertThat(find(IntegerField.class).withId("einladung-gueltigkeit").single().getValue()).isEqualTo(30);
	}

	// ---------------------------------------------------------------- Hauptablauf, Schritte 5 bis 10

	@Test
	@UseCase(id = "UC-001", scenario = "Main Success Scenario", businessRules = { "BR-001", "BR-004", "BR-007", "BR-008" })
	void erstellt_und_versendet_je_adresse_eine_einladung_und_speichert_keine_adresse() {
		navigate(EinladungErstellenUndVersendenView.class);
		adressen("anna.beispiel@example.com", "ben.muster@example.com", "carla.test@example.com");
		gueltigkeit(14);

		anfordern();

		// Schritt 6: die Zusammenfassung
		assertThat(zusammenfassung()).isEqualTo("Es werden 3 Einladungen für UC001 Alpha · UC001 Nord, Rolle UC001 Entwicklung "
				+ "erstellt und per E-Mail versendet. Gültigkeit: 14 Tage.");
		// Schritt 7 bis 10
		test(find(Button.class).withId("einladung-versenden").single()).click();

		assertThat(ergebnis()).startsWith("3 Einladungen versendet");
		// Postcondition aus der Datenbank: je Adresse eine Einladung mit Status Versendet
		List<String> referenzen = this.jdbc.sql("""
				select referenz from einladung where erstellt_von_id = 9201 and mag_id = 9201 and rolle_id = 9201
				and status = 'Versendet' and gueltigkeit_tage = 14
				""").query(String.class).list();
		assertThat(referenzen).hasSize(3).doesNotHaveDuplicates().allSatisfy(r -> {
			// BR-001: zufällig vergeben, keine Zeichenfolge aus dem Namen oder der Adresse
			assertThat(r).hasSize(36).doesNotContain("@", "anna", "beispiel", "muster", "carla");
		});
		// Jede Teilnehmerin erhält eine E-Mail mit Betreff, kurzem Text und dem Link zur Einladung (BR-007).
		List<Einladungsmail> gesendet = MAIL.gesendet();
		assertThat(gesendet).extracting(Einladungsmail::adresse)
			.containsExactlyInAnyOrder("anna.beispiel@example.com", "ben.muster@example.com", "carla.test@example.com");
		assertThat(gesendet).extracting(Einladungsmail::betreff).containsOnly(BETREFF);
		assertThat(gesendet).allSatisfy(mail -> assertThat(mail.text())
			.startsWith("Sie sind zu einem devace-Assessment eingeladen. Über den folgenden Link starten Sie es:\n\n"
					+ "http://localhost:8080/assessment/")
			.satisfies(text -> assertThat(referenzen).anyMatch(text::endsWith)));
		// BR-004: keine E-Mail-Adresse ist gespeichert, die Tabelle hat keine Spalte dafür
		assertThat(this.jdbc.sql("select column_name from information_schema.columns where table_name = 'einladung'")
			.query(String.class)
			.list()).containsExactlyInAnyOrder("id", "referenz", "mag_id", "rolle_id", "erstellt_von_id", "status",
					"erstellt_am", "gueltigkeit_tage", "laeuft_ab_am");
		// BR-004: die Adresse steht in keiner Tabelle, in keiner Spalte
		for (String tabelle : this.jdbc.sql("select table_name from information_schema.tables where table_schema = 'public'")
			.query(String.class)
			.list()) {
			assertThat(this.jdbc.sql("select count(*) from \"" + tabelle + "\" t where t::text like '%beispiel@example.com%'")
				.query(Long.class)
				.single()).as("Tabelle " + tabelle).isZero();
		}
	}

	@Test
	@UseCase(id = "UC-001", scenario = "Main Success Scenario", businessRules = { "BR-005" })
	void doppelte_adressen_ergeben_eine_einladung() {
		navigate(EinladungErstellenUndVersendenView.class);
		adressen("anna.beispiel@example.com", "ANNA.beispiel@example.com", "  anna.beispiel@example.com ");

		anfordern();
		assertThat(zusammenfassung()).contains("1 Einladung");
		test(find(Button.class).withId("einladung-versenden").single()).click();

		assertThat(MAIL.gesendet()).hasSize(1);
		assertThat(this.jdbc.sql("select count(*) from einladung where erstellt_von_id = 9201").query(Long.class).single())
			.isEqualTo(1L);
	}

	// ---------------------------------------------------------------- A1

	@Test
	@UseCase(id = "UC-001", scenario = "A1: Angaben fehlen oder sind ungültig", businessRules = { "BR-006" })
	void keine_adresse_wird_gemeldet() {
		navigate(EinladungErstellenUndVersendenView.class);
		adressen("", "   ");

		anfordern();

		assertThat(adressenFeld().isInvalid()).isTrue();
		assertThat(adressenFeld().getErrorMessage()).isEqualTo("Bitte mindestens eine Adresse eingeben.");
		keineEinladungErstelltOderVersendet();
	}

	@Test
	@UseCase(id = "UC-001", scenario = "A1: Angaben fehlen oder sind ungültig", businessRules = { "BR-006" })
	void ungueltige_adresse_wird_mit_der_adresse_gemeldet() {
		navigate(EinladungErstellenUndVersendenView.class);

		for (String ungueltig : List.of("anna@", "@example.com", "anna beispiel@example.com", "anna.example.com")) {
			adressen("ben.muster@example.com", ungueltig);
			anfordern();
			assertThat(adressenFeld().isInvalid()).as(ungueltig).isTrue();
			assertThat(adressenFeld().getErrorMessage())
				.isEqualTo("Mindestens eine Adresse ist ungültig: " + ungueltig + ". Bitte korrigieren.");
		}
		keineEinladungErstelltOderVersendet();
	}

	@Test
	@UseCase(id = "UC-001", scenario = "A1: Angaben fehlen oder sind ungültig", businessRules = { "BR-006" })
	void domaenenteil_braucht_mindestens_einen_punkt() {
		navigate(EinladungErstellenUndVersendenView.class);

		// ohne Punkt im Domänenteil ungültig (OQ-053), das Mockup zeigt dasselbe Beispiel
		adressen("ben.muster@example");
		anfordern();
		assertThat(adressenFeld().isInvalid()).isTrue();
		assertThat(adressenFeld().getErrorMessage())
			.isEqualTo("Mindestens eine Adresse ist ungültig: ben.muster@example. Bitte korrigieren.");

		// mit Punkt gültig
		adressen("ben.muster@example.com");
		anfordern();
		assertThat(adressenFeld().isInvalid()).isFalse();
		assertThat(find(Dialog.class).single().isOpened()).isTrue();
	}

	@Test
	@UseCase(id = "UC-001", scenario = "A1: Angaben fehlen oder sind ungültig", businessRules = { "BR-010" })
	void hoechstens_50_adressen_je_durchgang() {
		navigate(EinladungErstellenUndVersendenView.class);

		adressen(adressen(51));
		anfordern();
		assertThat(adressenFeld().isInvalid()).isTrue();
		assertThat(adressenFeld().getErrorMessage()).isEqualTo("Höchstens 50 Adressen je Durchgang.");

		// genau 50 sind zulässig
		adressen(adressen(50));
		anfordern();
		assertThat(adressenFeld().isInvalid()).isFalse();
		assertThat(find(Dialog.class).single().isOpened()).isTrue();
	}

	@Test
	@UseCase(id = "UC-001", scenario = "A1: Angaben fehlen oder sind ungültig", businessRules = { "BR-005", "BR-010" })
	void doppelte_adressen_zaehlen_nur_einmal_gegen_die_grenze() {
		navigate(EinladungErstellenUndVersendenView.class);
		String[] zeilen = adressen(51);
		zeilen[50] = zeilen[0];

		adressen(zeilen);
		anfordern();

		assertThat(adressenFeld().isInvalid()).isFalse();
		assertThat(zusammenfassung()).contains("50 Einladungen");
	}

	@Test
	@UseCase(id = "UC-001", scenario = "A1: Angaben fehlen oder sind ungültig", businessRules = { "BR-008" })
	void gueltigkeit_ausserhalb_von_1_bis_30_wird_gemeldet() {
		navigate(EinladungErstellenUndVersendenView.class);
		adressen("anna.beispiel@example.com");

		for (int ungueltig : new int[] { 0, 31 }) {
			gueltigkeit(ungueltig);
			anfordern();
			IntegerField feld = find(IntegerField.class).withId("einladung-gueltigkeit").single();
			assertThat(feld.isInvalid()).as("Gültigkeit " + ungueltig).isTrue();
			assertThat(feld.getErrorMessage()).isEqualTo("Die Zahl muss zwischen 1 und 30 liegen.");
		}
		keineEinladungErstelltOderVersendet();

		// die Grenzen 1 und 30 sind zulässig
		for (int gueltig : new int[] { 1, 30 }) {
			gueltigkeit(gueltig);
			anfordern();
			assertThat(find(IntegerField.class).withId("einladung-gueltigkeit").single().isInvalid()).isFalse();
		}
	}

	// ---------------------------------------------------------------- A2, A3, A4

	@Test
	@Transactional
	@UseCase(id = "UC-001", scenario = "A2: Keine Rolle mit freigegebenem Fragenkatalog", businessRules = { "BR-003" })
	void ohne_rolle_mit_freigegebenem_fragenkatalog_ist_noch_keine_einladung_moeglich() {
		// Alle freigegebenen Fragenkataloge entfernen; das Zurückrollen des Tests stellt die Testdaten wieder her.
		this.jdbc.sql("""
				delete from stufenvorgabe where frage_id in (
				select f.id from frage f join fragenkatalog k on k.id = f.fragenkatalog_id where k.status = 'Freigegeben')
				""").update();
		this.jdbc.sql("delete from frage where fragenkatalog_id in (select id from fragenkatalog where status = 'Freigegeben')")
			.update();
		this.jdbc.sql("delete from fragenkatalog where status = 'Freigegeben'").update();

		navigate(EinladungErstellenUndVersendenView.class);

		List<String> hinweise = find(Div.class).withClassName("devace-hinweis")
			.all()
			.stream()
			.map(div -> div.getElement().getTextRecursively())
			.toList();
		assertThat(hinweise).anyMatch(text -> text.contains("Noch keine Einladung möglich")
				&& text.contains("Es gibt noch keine Rolle mit freigegebenem Fragenkatalog. Bitte wenden Sie sich an einen Administrator."));
		assertThat(find(TextArea.class).withId("einladung-adressen").exists()).isFalse();
	}

	@Test
	@UseCase(id = "UC-001", scenario = "A3: Teamleitung bricht ab")
	void abbruch_bei_der_zusammenfassung_verwirft_die_angaben() {
		navigate(EinladungErstellenUndVersendenView.class);
		adressen("anna.beispiel@example.com");
		anfordern();

		test(find(Button.class).withId("zusammenfassung-abbrechen").single()).click();

		assertThat(find(Dialog.class).all().stream().noneMatch(Dialog::isOpened)).isTrue();
		keineEinladungErstelltOderVersendet();
	}

	@Test
	@UseCase(id = "UC-001", scenario = "A3: Teamleitung bricht ab")
	void abbruch_im_formular_verwirft_die_angaben() {
		navigate(EinladungErstellenUndVersendenView.class);
		adressen("anna.beispiel@example.com");
		gueltigkeit(7);

		test(find(Button.class).withId("einladung-abbrechen").single()).click();

		assertThat(adressenFeld().getValue()).isEmpty();
		assertThat(find(IntegerField.class).withId("einladung-gueltigkeit").single().getValue()).isEqualTo(30);
		keineEinladungErstelltOderVersendet();
	}

	@Test
	@UseCase(id = "UC-001", scenario = "A4: Versand scheitert")
	void gescheiterter_versand_verwirft_nur_die_einladung_dieser_adresse() {
		MAIL.lehneAb("ben.muster@example.com");
		navigate(EinladungErstellenUndVersendenView.class);
		adressen("anna.beispiel@example.com", "ben.muster@example.com", "carla.test@example.com");
		anfordern();

		test(find(Button.class).withId("einladung-versenden").single()).click();

		// Schritt 10: die Zahl der versendeten Einladungen und nur die Adresse, bei der der Versand scheiterte
		assertThat(ergebnis()).startsWith("2 Einladungen versendet");
		assertThat(find(Div.class).withId("einladung-fehler").single().getElement().getTextRecursively())
			.startsWith("1 Einladung nicht versendet");
		@SuppressWarnings("unchecked")
		Grid<String> liste = find(Grid.class).withId("nicht-versendet-grid").single();
		assertThat(liste.getListDataView().getItems().toList()).containsExactly("ben.muster@example.com");
		// A4 Schritt 2: die übrigen Adressen werden weiter versendet
		assertThat(MAIL.gesendet()).extracting(Einladungsmail::adresse)
			.containsExactlyInAnyOrder("anna.beispiel@example.com", "carla.test@example.com");
		// Fehlerfall: für die gescheiterte Adresse bleibt keine Einladung gespeichert
		assertThat(this.jdbc.sql("select count(*) from einladung where erstellt_von_id = 9201").query(Long.class).single())
			.isEqualTo(2L);
		assertThat(this.jdbc.sql("select count(*) from einladung where erstellt_von_id = 9201 and status <> 'Versendet'")
			.query(Long.class)
			.single()).isZero();
	}

	// ---------------------------------------------------------------- BR-009

	@Test
	@WithAnonymousUser
	@UseCase(id = "UC-001", businessRules = { "BR-009" }, requirements = { "NFR-005" })
	void aufruf_ohne_anmeldung_wird_abgewiesen() {
		UI.getCurrent().navigate("einladungen");

		assertThat(find(EinladungErstellenUndVersendenView.class).exists()).isFalse();
	}

	@Test
	@WithMockUser(roles = "ADMINISTRATOR")
	@UseCase(id = "UC-001", businessRules = { "BR-009" }, requirements = { "NFR-005" })
	void aufruf_ohne_passende_berechtigung_wird_abgewiesen() {
		UI.getCurrent().navigate("einladungen");

		assertThat(find(EinladungErstellenUndVersendenView.class).exists()).isFalse();
	}

	@Test
	@UseCase(id = "UC-001", businessRules = { "BR-009" }, requirements = { "NFR-005" })
	void teamleitung_erreicht_dieselbe_route() {
		UI.getCurrent().navigate("einladungen");

		assertThat(find(EinladungErstellenUndVersendenView.class).exists()).isTrue();
	}

	@Test
	@UseCase(id = "UC-001", scenario = "Main Success Scenario", businessRules = { "BR-005" })
	void unterschiedliche_schreibweise_und_leerzeichen_gelten_als_dieselbe_adresse() {
		navigate(EinladungErstellenUndVersendenView.class);
		adressen("Anna.Beispiel@Example.com", "anna.beispiel@example.com ");

		anfordern();
		test(find(Button.class).withId("einladung-versenden").single()).click();

		// OQ-054: die erste Schreibweise gilt
		assertThat(MAIL.gesendet()).extracting(Einladungsmail::adresse).containsExactly("Anna.Beispiel@Example.com");
	}

	@Test
	@UseCase(id = "UC-001", scenario = "Main Success Scenario", businessRules = { "BR-008" })
	void ablaufdatum_ist_der_letzte_tag_der_gueltigkeit_der_erstellungstag_ist_der_erste() {
		navigate(EinladungErstellenUndVersendenView.class);
		for (int tage : new int[] { 1, 14, 30 }) {
			adressen("tag" + tage + ".beispiel@example.com");
			gueltigkeit(tage);
			anfordern();
			test(find(Button.class).withId("einladung-versenden").single()).click();

			LocalDate ablauf = this.jdbc.sql("select laeuft_ab_am from einladung where erstellt_von_id = 9201 and gueltigkeit_tage = ?")
				.param(tage)
				.query(LocalDate.class)
				.single();
			assertThat(ablauf).as("Gültigkeit " + tage).isEqualTo(LocalDate.now().plusDays(tage - 1L));
			this.jdbc.sql("delete from einladung where erstellt_von_id = 9201").update();
			test(find(Button.class).withId("weitere-einladungen").single()).click();
		}
	}

	@Test
	@WithMockUser(username = "uc001-ohne-mag", roles = "TEAMLEITUNG")
	@UseCase(id = "UC-001", scenario = "Precondition: mindestens eine MAG zugeordnet")
	void ohne_zugeordnete_mag_bleibt_die_aktion_deaktiviert() {
		navigate(EinladungErstellenUndVersendenView.class);

		assertThat(find(Button.class).withId("einladung-erstellen").single().isEnabled()).isFalse();
		keineEinladungErstelltOderVersendet();
	}

	@Test
	@UseCase(id = "UC-001", businessRules = { "BR-009" }, requirements = { "NFR-005" })
	void navigation_zeigt_den_eintrag_der_teamleitung_aktiviert() {
		navigate(EinladungErstellenUndVersendenView.class);

		assertThat(navigationseintrag("Einladungen").isEnabled()).isTrue();
	}

	@Test
	@WithMockUser(roles = "ADMINISTRATOR")
	@UseCase(id = "UC-001", businessRules = { "BR-009" }, requirements = { "NFR-005" })
	void navigation_zeigt_den_eintrag_ohne_berechtigung_deaktiviert() {
		navigate(MagsUndRollenView.class);

		assertThat(navigationseintrag("Einladungen").isEnabled()).isFalse();
	}

	// ---------------------------------------------------------------- Hilfen

	private SideNavItem navigationseintrag(String beschriftung) {
		return find(SideNavItem.class).all()
			.stream()
			.filter(eintrag -> beschriftung.equals(eintrag.getLabel()))
			.findFirst()
			.orElseThrow();
	}

	private TextArea adressenFeld() {
		return find(TextArea.class).withId("einladung-adressen").single();
	}

	private void adressen(String... zeilen) {
		test(adressenFeld()).setValue(String.join("\n", zeilen));
	}

	private static String[] adressen(int anzahl) {
		return IntStream.rangeClosed(1, anzahl).mapToObj(i -> "teilnehmer" + i + "@example.com").toArray(String[]::new);
	}

	private void gueltigkeit(int tage) {
		find(IntegerField.class).withId("einladung-gueltigkeit").single().setValue(tage);
	}

	private void anfordern() {
		test(find(Button.class).withId("einladung-erstellen").single()).click();
	}

	private String zusammenfassung() {
		return find(Paragraph.class).all().stream().map(Paragraph::getText).collect(Collectors.joining(" "));
	}

	private String ergebnis() {
		return find(Div.class).withId("einladung-ergebnis").single().getElement().getTextRecursively();
	}

	private void keineEinladungErstelltOderVersendet() {
		assertThat(MAIL.gesendet()).isEmpty();
		assertThat(this.jdbc.sql("select count(*) from einladung where erstellt_von_id = 9201").query(Long.class).single())
			.isZero();
	}

}
