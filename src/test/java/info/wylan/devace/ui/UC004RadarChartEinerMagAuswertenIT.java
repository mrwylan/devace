package info.wylan.devace.ui;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.AriaRole;
import info.wylan.devace.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.vaadin.addons.dramafinder.AbstractBasePlaywrightIT;
import org.vaadin.addons.dramafinder.element.ButtonElement;
import org.vaadin.addons.dramafinder.element.PasswordFieldElement;
import org.vaadin.addons.dramafinder.element.TextFieldElement;

/**
 * Browser-Test für UC-004 "Radar-Chart einer MAG auswerten". Die erwarteten Werte stammen aus der Spezifikation und
 * den Testdaten (R__uc004_auswertung.sql). Die Randfälle der Geschäftsregeln prüfen die Browserless-Tests; hier läuft,
 * was die Teamleitung im Browser sieht. Das Anmelden prüft BR-007 mit.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DisplayName("UC-004: Radar-Chart einer MAG auswerten")
class UC004RadarChartEinerMagAuswertenIT extends AbstractBasePlaywrightIT {

	private static final String TEAMLEITUNG = "uc004-teamleitung";

	// Testkonto der Testdaten (R__uc004_auswertung.sql), kein echtes Passwort
	private static final String PASSWORT = "uc004-test";

	@LocalServerPort
	private int port;

	@Override
	public String getUrl() {
		return String.format("http://localhost:%d/", this.port);
	}

	@Override
	public String getView() {
		return "auswertung";
	}

	@BeforeEach
	void anmelden() {
		// Der Aufruf ohne Anmeldung führt zur Anmeldeseite (UC-004 BR-007, NFR-005).
		TextFieldElement.getByLabel(this.page, "Benutzername").setValue(TEAMLEITUNG);
		PasswordFieldElement.getByLabel(this.page, "Passwort").setValue(PASSWORT);
		ButtonElement.getByText(this.page, "Anmelden").click();
		this.page.waitForURL(url -> !url.contains("login"));
		this.page.navigate(getUrl() + getView());
	}

	@Nested
	@DisplayName("Main Success Scenario")
	class MainSuccessScenario {

		@Test
		@DisplayName("Steps 1-4: MAG und Rolle wählen und das Radar-Chart sehen")
		void radar_chart_der_mag_und_rolle() {
			// NFR-007: das Radar-Chart ist spätestens 3 s nach dem Aufruf vollständig dargestellt
			PlaywrightAssertions.assertThat(radarChart()).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(3_000));
			// BR-006: Handlungsfeld 4 ohne Score hat keinen Punkt: fünf Punkte bei sechs Achsen
			PlaywrightAssertions.assertThat(page.locator("svg.devace-radar-svg circle.devace-radar-punkt"))
				.hasCount(5, new LocatorAssertions.HasCountOptions().setTimeout(3_000));
			// BR-001: Mittelwert 3.1666… kaufmännisch gerundet 3.2, 2.25 gerundet 2.3
			PlaywrightAssertions.assertThat(page.locator("#auswertung-werte")).containsText("3.2");
			PlaywrightAssertions.assertThat(page.locator("#auswertung-werte")).containsText("2.3");
			PlaywrightAssertions.assertThat(page.locator("#auswertung-werte")).containsText("–");
		}

	}

	@Nested
	@DisplayName("A2: Teamleitung wechselt MAG oder Rolle")
	class A2RolleWechseln {

		@Test
		@DisplayName("Andere Rolle zeigt das Radar-Chart zur neuen Auswahl")
		void andere_rolle() {
			PlaywrightAssertions.assertThat(radarChart()).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(3_000));

			page.locator("#auswertung-rolle").click();
			page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("UC004 Test")).click();

			// BR-003: auch der Score eines einzigen Assessments ergibt ein Radar-Chart, hier mit einem Punkt
			PlaywrightAssertions.assertThat(page.locator("svg.devace-radar-svg circle.devace-radar-punkt"))
				.hasCount(1, new LocatorAssertions.HasCountOptions().setTimeout(3_000));
			PlaywrightAssertions.assertThat(page.locator("#auswertung-werte")).containsText("1.0");
		}

	}

	private com.microsoft.playwright.Locator radarChart() {
		return page.getByRole(AriaRole.IMG, new Page.GetByRoleOptions().setName("Radar-Chart der Scores je Handlungsfeld"));
	}

}
