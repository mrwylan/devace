package info.wylan.devace.ui;

import java.util.UUID;

import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import info.wylan.devace.TestcontainersConfiguration;
import info.wylan.devace.service.KiModellClient;
import info.wylan.devace.service.KiModellClientFake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.vaadin.addons.dramafinder.AbstractBasePlaywrightIT;

/**
 * Browser-Test für UC-009 "KI-Bewertung mit Testfällen absichern". Die erwarteten Werte stammen aus der Spezifikation
 * und den Testdaten (R__uc009_absicherung.sql). Das KI-Modell ist durch {@link KiModellClientFake} ersetzt, das jeder
 * Antwort die Stufe 3 zuordnet. Die Randfälle der Geschäftsregeln prüfen die Browserless-Tests. Das Anmelden prüft
 * BR-011 mit.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DisplayName("UC-009: KI-Bewertung mit Testfällen absichern")
class UC009KiBewertungMitTestfaellenAbsichernIT extends AbstractBasePlaywrightIT {

	private static final String ADMIN = "uc009-e2e-admin";

	// zufällig je Lauf: das Startkonto der Anwendung legt den Administrator damit an, nichts davon liegt im Repository
	private static final String ADMIN_PASSWORT = UUID.randomUUID().toString();

	private static final KiModellClientFake KI = new KiModellClientFake();

	@LocalServerPort
	private int port;

	@TestBean
	KiModellClient kiModellClient;

	static KiModellClient kiModellClient() {
		return KI;
	}

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
		return "ki-absicherung";
	}

	@BeforeEach
	void anmelden() {
		KI.reset();
		// Der Aufruf ohne Anmeldung führt zur Anmeldeseite (UC-009 BR-011, NFR-005).
		Anmeldung.anmelden(this.page, ADMIN, ADMIN_PASSWORT);
		this.page.navigate(getUrl() + getView());
	}

	@Nested
	@DisplayName("Main Success Scenario")
	class MainSuccessScenario {

		@Test
		@DisplayName("Steps 1-9: Fragenkatalog wählen, Testfälle ausführen, Trefferquote und Abweichungen sehen")
		void testfaelle_ausfuehren() {
			// Schritt 2 und 3: der Entwurf von UC009 Entwicklung, die einzige Zeile mit drei Testfällen und Status Entwurf
			waehle("UC009 Entwicklung", 1);

			// Schritt 4: die Testfälle je Frage
			PlaywrightAssertions.assertThat(page.locator("#ki-frage-9803")).containsText("UC009 Antwort eins");

			// Schritte 6 bis 9: Stufe 3 für alle; erwartet 3, 1, 4: ein Treffer von drei
			page.locator("#ki-ausfuehren").click();
			PlaywrightAssertions.assertThat(page.locator("#ki-trefferquote"))
				.hasText("Trefferquote 33.33 % · Nicht bestanden", new LocatorAssertions.HasTextOptions().setTimeout(10_000));
			PlaywrightAssertions.assertThat(page.locator("#ki-abweichungen")).containsText("UC009 Antwort zwei");
			PlaywrightAssertions.assertThat(page.locator("#ki-abweichungen")).containsText("UC009 Antwort drei");
		}

	}


	@Nested
	@DisplayName("A2: Angaben zum Testfall ungültig")
	class A2AngabenUngueltig {

		@Test
		@DisplayName("Testfall ohne Antwort und Stufe wird am Feld gemeldet")
		void ohne_antwort_und_stufe() {
			waehleEntwurf();
			page.locator("#testfall-hinzufuegen-9804").click();
			page.locator("#testfall-speichern").click();

			PlaywrightAssertions.assertThat(page.getByText("Antwort fehlt.")).isVisible();
			PlaywrightAssertions.assertThat(page.getByText("Stufe fehlt.")).isVisible();
		}

	}

	@Nested
	@DisplayName("A3: Fragenkatalog ohne Testfall")
	class A3OhneTestfall {

		@Test
		@DisplayName("Ausführen ohne Testfall weist darauf hin, dass es nichts auszuführen gibt")
		void ohne_testfall() {
			waehle("UC009 Ohne Testfall", 0);
			page.locator("#ki-ausfuehren").click();

			PlaywrightAssertions.assertThat(page.locator("#ki-ausfuehren-hinweis"))
				.hasText("Der Fragenkatalog hat keinen Testfall.", new LocatorAssertions.HasTextOptions().setTimeout(10_000));
		}

	}

	@Nested
	@DisplayName("A4: KI-Modell liefert keine Stufe")
	class A4KeineStufe {

		@Test
		@DisplayName("Nicht erreichbares KI-Modell speichert keinen Testlauf")
		void nicht_erreichbar() {
			waehleEntwurf();
			KI.failNextBewertung();
			page.locator("#ki-ausfuehren").click();

			PlaywrightAssertions.assertThat(page.locator("#ki-ausfuehren-hinweis"))
				.hasText("Der Testlauf konnte nicht abgeschlossen werden. Es wurde nichts gespeichert.",
						new LocatorAssertions.HasTextOptions().setTimeout(10_000));
		}

	}

	/**
	 * Schritt 3: der Entwurf von UC009 Entwicklung, die zweite Zeile dieser Rolle (Version 2). Die Liste scrollt in sich
	 * (ADR-006); die Suche bringt die Zeilen der Rolle nach oben.
	 */
	private void waehleEntwurf() {
		waehle("UC009 Entwicklung", 1);
		PlaywrightAssertions.assertThat(page.locator("#ki-frage-9803")).isVisible();
	}

	/** Die Liste scrollt in sich (ADR-006); die Suche bringt die Zeilen der Rolle nach oben, dann die n-te Zeile. */
	private void waehle(String rolle, int zeile) {
		page.locator("#pruefbare-kataloge-suche input").fill(rolle);
		PlaywrightAssertions.assertThat(page.locator("#pruefbare-kataloge-anzahl"))
			.hasText(java.util.regex.Pattern.compile("^\\d+ von \\d+$"));
		page.locator("vaadin-grid-cell-content")
			.filter(new com.microsoft.playwright.Locator.FilterOptions().setHasText(rolle))
			.nth(zeile)
			.click();
	}

}
