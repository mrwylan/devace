package info.wylan.devace.ui;

import com.microsoft.playwright.Locator;
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

/**
 * Browser-Test für UC-005 "Radar-Charts für die Schulungsplanung auswerten". Die erwarteten Werte stammen aus der
 * Spezifikation und den Testdaten (R__uc005_schulungsplanung.sql). Die Randfälle der Geschäftsregeln prüfen die
 * Browserless-Tests; hier läuft, was die Schulungsverantwortliche im Browser sieht. Das Anmelden prüft BR-007 mit.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DisplayName("UC-005: Radar-Charts für die Schulungsplanung auswerten")
class UC005RadarChartsFuerDieSchulungsplanungAuswertenIT extends AbstractBasePlaywrightIT {

	private static final String SCHULUNG = "uc005-schulung";

	// Testkonto der Testdaten (R__uc005_schulungsplanung.sql), kein echtes Passwort
	private static final String PASSWORT = "uc005-test";

	@LocalServerPort
	private int port;

	@Override
	public String getUrl() {
		return String.format("http://localhost:%d/", this.port);
	}

	@Override
	public String getView() {
		return "schulungsplanung";
	}

	@BeforeEach
	void anmelden() {
		// Der Aufruf ohne Anmeldung führt zur Anmeldeseite (UC-005 BR-007, NFR-005).
		Anmeldung.anmelden(this.page, SCHULUNG, PASSWORT);
		this.page.navigate(getUrl() + getView());
	}

	@Nested
	@DisplayName("Main Success Scenario")
	class MainSuccessScenario {

		@Test
		@DisplayName("Steps 1-4: Rolle wählen und die Radar-Charts aller MAGs sehen")
		void radar_charts_der_rolle() {
			// Schritt 3: ohne Wahl kein Radar-Chart
			PlaywrightAssertions.assertThat(page.locator("#schulung-rolle")).isVisible();
			PlaywrightAssertions.assertThat(radarCharts()).hasCount(0);
			waehleRolle("UC005 Entwicklung");

			// NFR-007: die Radar-Charts sind spätestens 3 s nach dem Aufruf vollständig dargestellt
			PlaywrightAssertions.assertThat(radarCharts()).hasCount(2, new LocatorAssertions.HasCountOptions().setTimeout(3_000));
			// BR-002: alle MAGs mit Scores der Rolle; BR-001: 2.25 kaufmännisch gerundet 2.3
			PlaywrightAssertions.assertThat(page.locator("#schulung-mag-9701")).containsText("UC005 Alpha · UC005 Nord");
			PlaywrightAssertions.assertThat(page.locator("#schulung-mag-9701")).containsText("3.5");
			PlaywrightAssertions.assertThat(page.locator("#schulung-mag-9701")).containsText("2.3");
			PlaywrightAssertions.assertThat(page.locator("#schulung-mag-9702")).containsText("UC005 Beta · UC005 Süd");
		}

	}

	@Nested
	@DisplayName("A2: Schulungsverantwortliche wechselt die Rolle")
	class A2RolleWechseln {

		@Test
		@DisplayName("Andere Rolle zeigt die Radar-Charts zur neuen Rolle")
		void andere_rolle() {
			waehleRolle("UC005 Entwicklung");
			PlaywrightAssertions.assertThat(radarCharts()).hasCount(2, new LocatorAssertions.HasCountOptions().setTimeout(3_000));

			waehleRolle("UC005 Test");

			PlaywrightAssertions.assertThat(radarCharts()).hasCount(1, new LocatorAssertions.HasCountOptions().setTimeout(3_000));
			PlaywrightAssertions.assertThat(page.locator("#schulung-mag-9703")).containsText("5.0");
		}

	}

	private void waehleRolle(String rolle) {
		PlaywrightAssertions.assertThat(page.locator("#schulung-rolle")).not().hasAttribute("disabled", "");
		page.locator("#schulung-rolle").click();
		page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(rolle)).click();
	}

	private Locator radarCharts() {
		return page.getByRole(AriaRole.IMG, new Page.GetByRoleOptions().setName("Radar-Chart der Scores je Handlungsfeld"));
	}

}
