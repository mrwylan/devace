package info.wylan.devace.ui;

import java.util.List;
import java.util.UUID;

import com.microsoft.playwright.assertions.PlaywrightAssertions;
import info.wylan.devace.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.vaadin.addons.dramafinder.AbstractBasePlaywrightIT;
import org.vaadin.addons.dramafinder.element.ButtonElement;
import org.vaadin.addons.dramafinder.element.ComboBoxElement;
import org.vaadin.addons.dramafinder.element.GridElement;
import org.vaadin.addons.dramafinder.element.PasswordFieldElement;
import org.vaadin.addons.dramafinder.element.TextFieldElement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Browser test für UC-008 "MAGs und Rollen verwalten". Erwartete Werte stammen aus der Spezifikation und den
 * Testdaten. Die Randwerte der Geschäftsregeln prüfen die Browserless-Tests; hier läuft, was die Administratorin
 * im Browser sieht. Das Anmelden prüft BR-007 mit.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DisplayName("UC-008: MAGs und Rollen verwalten")
class UC008MagsUndRollenVerwaltenIT extends AbstractBasePlaywrightIT {

	private static final String PRAEFIX = "UC008E";

	private static final String ADMIN = "uc008-e2e-admin";

	// zufällig je Lauf: das Startkonto der Anwendung legt den Administrator damit an, nichts davon liegt im Repository
	private static final String ADMIN_PASSWORT = UUID.randomUUID().toString();

	@LocalServerPort
	private int port;

	@Autowired
	private JdbcClient jdbc;

	@DynamicPropertySource
	static void administrator(DynamicPropertyRegistry registry) {
		registry.add("devace.admin.username", () -> ADMIN);
		registry.add("devace.admin.password", () -> ADMIN_PASSWORT);
	}

	@Override
	public String getUrl() {
		return String.format("http://localhost:%d/", this.port);
	}

	@Override
	public String getView() {
		return "mags-und-rollen";
	}

	@BeforeEach
	void anmelden() {
		// Der Aufruf ohne Anmeldung führt zur Anmeldeseite (UC-008 BR-007, NFR-005).
		TextFieldElement.getByLabel(this.page, "Benutzername").setValue(ADMIN);
		PasswordFieldElement.getByLabel(this.page, "Passwort").setValue(ADMIN_PASSWORT);
		ButtonElement.getByText(this.page, "Anmelden").click();
		this.page.waitForURL(url -> !url.contains("login"));
		this.page.navigate(getUrl() + getView());
		GridElement.getById(this.page, "mag-grid").waitForGridToStopLoading();
	}

	@AfterEach
	void aufraeumen() {
		// Die Oberfläche kann nicht löschen (BR-005): gezielt nur, was dieser Test angelegt hat.
		this.jdbc.sql("delete from mag where scrum_team like 'UC008E%'").update();
		this.jdbc.sql("delete from rolle where name like 'UC008E%'").update();
	}

	@Nested
	@DisplayName("Main Success Scenario")
	class MainSuccessScenario {

		@Test
		@DisplayName("Steps 1-2: Übersicht zeigt MAGs mit Teamleitung und Rollen mit Fragenkatalogen")
		void uebersicht() {
			GridElement magGrid = GridElement.getById(page, "mag-grid");
			List<Integer> rows = magGrid.findRowIndexesWithColumnText(0, "UC008 Alpha");
			assertThat(rows).hasSize(1);
			PlaywrightAssertions.assertThat(magGrid.findCell(rows.get(0), "Teamleitung").get().getCellContentLocator())
				.hasText("uc008-teamleitung-a");

			GridElement rolleGrid = GridElement.getById(page, "rolle-grid");
			rolleGrid.waitForGridToStopLoading();
			List<Integer> rollen = rolleGrid.findRowIndexesWithColumnText(0, "UC008 Entwickler");
			assertThat(rollen).hasSize(1);
			PlaywrightAssertions.assertThat(rolleGrid.findCell(rollen.get(0), "Fragenkataloge").get().getCellContentLocator())
				.containsText("Version 1 (Freigegeben)");
		}

		@Test
		@DisplayName("Steps 3-8: MAG anlegen und in der aktualisierten Übersicht sehen")
		void mag_anlegen() {
			ButtonElement.getByText(page, "MAG anlegen").click();
			TextFieldElement.getByLabel(page, "Scrum Team").setValue(PRAEFIX + " Alpha");
			TextFieldElement.getByLabel(page, "Linienteam").setValue(PRAEFIX + " Nord");
			ComboBoxElement.getByLabel(page, "Teamleitung").selectItem("uc008-teamleitung-b");
			ButtonElement.getByText(page, "Speichern").click();

			GridElement grid = GridElement.getById(page, "mag-grid");
			grid.waitForGridToStopLoading();
			List<Integer> rows = grid.findRowIndexesWithColumnText(0, PRAEFIX + " Alpha");
			assertThat(rows).hasSize(1);
			PlaywrightAssertions.assertThat(grid.findCell(rows.get(0), "Teamleitung").get().getCellContentLocator())
				.hasText("uc008-teamleitung-b");
		}

		@Test
		@DisplayName("Steps 3-8: Rolle anlegen und in der aktualisierten Übersicht sehen")
		void rolle_anlegen() {
			ButtonElement.getByText(page, "Rolle anlegen").click();
			TextFieldElement.getByLabel(page, "Name").setValue(PRAEFIX + " Tester");
			ButtonElement.getByText(page, "Speichern").click();

			GridElement grid = GridElement.getById(page, "rolle-grid");
			grid.waitForGridToStopLoading();
			assertThat(grid.findRowIndexesWithColumnText(0, PRAEFIX + " Tester")).hasSize(1);
		}

	}

	@Nested
	@DisplayName("A1: Angaben fehlen oder sind ungültig")
	class A1AngabenFehlenOderSindUngueltig {

		@Test
		@DisplayName("BR-001: Fehlendes Scrum Team zeigt den Hinweis")
		void pflichtangabe_fehlt() {
			ButtonElement.getByText(page, "MAG anlegen").click();
			TextFieldElement.getByLabel(page, "Linienteam").setValue(PRAEFIX + " Nord");
			ButtonElement.getByText(page, "Speichern").click();

			TextFieldElement scrumTeam = TextFieldElement.getByLabel(page, "Scrum Team");
			scrumTeam.assertInvalid();
			scrumTeam.assertErrorMessage("Pflichtangabe fehlt.");
		}

	}

	@Nested
	@DisplayName("A2: MAG oder Rolle besteht bereits")
	class A2MagOderRolleBestehtBereits {

		@Test
		@DisplayName("BR-002: Vorhandene MAG wird mit Hinweis abgewiesen")
		void mag_besteht_bereits() {
			ButtonElement.getByText(page, "MAG anlegen").click();
			TextFieldElement.getByLabel(page, "Scrum Team").setValue("uc008 alpha");
			TextFieldElement.getByLabel(page, "Linienteam").setValue("UC008 NORD");
			ButtonElement.getByText(page, "Speichern").click();

			TextFieldElement.getByLabel(page, "Linienteam").assertErrorMessage("Diese MAG besteht bereits.");
		}

		@Test
		@DisplayName("BR-003: Vorhandene Rolle wird mit Hinweis abgewiesen")
		void rolle_besteht_bereits() {
			ButtonElement.getByText(page, "Rolle anlegen").click();
			TextFieldElement.getByLabel(page, "Name").setValue("uc008 entwickler");
			ButtonElement.getByText(page, "Speichern").click();

			TextFieldElement.getByLabel(page, "Name").assertErrorMessage("Diese Rolle besteht bereits.");
		}

	}

	@Nested
	@DisplayName("A3: Administrator bricht ab")
	class A3AdministratorBrichtAb {

		@Test
		@DisplayName("Abbrechen verwirft die Angaben")
		void abbrechen() {
			ButtonElement.getByText(page, "Rolle anlegen").click();
			TextFieldElement.getByLabel(page, "Name").setValue(PRAEFIX + " Abbruch");
			ButtonElement.getByText(page, "Abbrechen").click();

			GridElement grid = GridElement.getById(page, "rolle-grid");
			grid.waitForGridToStopLoading();
			assertThat(grid.findRowIndexesWithColumnText(0, PRAEFIX + " Abbruch")).isEmpty();
		}

	}

}
