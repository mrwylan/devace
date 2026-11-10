package info.wylan.devace.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import info.wylan.devace.TestcontainersConfiguration;
import info.wylan.devace.service.EmailSystemClient;
import info.wylan.devace.service.EmailSystemClientFake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.vaadin.addons.dramafinder.AbstractBasePlaywrightIT;
import org.vaadin.addons.dramafinder.element.ButtonElement;
import org.vaadin.addons.dramafinder.element.IntegerFieldElement;
import org.vaadin.addons.dramafinder.element.PasswordFieldElement;
import org.vaadin.addons.dramafinder.element.TextAreaElement;
import org.vaadin.addons.dramafinder.element.TextFieldElement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Browser test für UC-001 "Einladung erstellen und versenden". Erwartete Werte stammen aus der Spezifikation und den
 * Testdaten (R__uc001_einladungen.sql). Die Randwerte der Geschäftsregeln und A2 (keine Rolle mit freigegebenem
 * Fragenkatalog) prüfen die Browserless-Tests; hier läuft, was die Teamleitung im Browser sieht. Das E-Mail-System ist
 * durch {@link EmailSystemClientFake} ersetzt, kein Test erreicht einen echten Mailserver.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DisplayName("UC-001: Einladung erstellen und versenden")
class UC001EinladungErstellenUndVersendenIT extends AbstractBasePlaywrightIT {

	private static final String TEAMLEITUNG = "uc001-teamleitung";

	// Testkonto der Testdaten (R__uc001_einladungen.sql), kein echtes Passwort
	private static final String PASSWORT = "uc001-test";

	private static final EmailSystemClientFake MAIL = new EmailSystemClientFake();

	@LocalServerPort
	private int port;

	@TestBean
	EmailSystemClient emailSystemClient;

	@Autowired
	private JdbcClient jdbc;

	static EmailSystemClient emailSystemClient() {
		return MAIL;
	}

	@Override
	public String getUrl() {
		return String.format("http://localhost:%d/", this.port);
	}

	@Override
	public String getView() {
		return "einladungen";
	}

	@BeforeEach
	void anmelden() {
		MAIL.reset();
		// Der Aufruf ohne Anmeldung führt zur Anmeldeseite (UC-001 BR-009, NFR-005).
		TextFieldElement.getByLabel(this.page, "Benutzername").setValue(TEAMLEITUNG);
		PasswordFieldElement.getByLabel(this.page, "Passwort").setValue(PASSWORT);
		ButtonElement.getByText(this.page, "Anmelden").click();
		this.page.waitForURL(url -> !url.contains("login"));
		this.page.navigate(getUrl() + getView());
		PlaywrightAssertions.assertThat(this.page.getByText("Hinweis zur Einladung")).isVisible();
	}

	@AfterEach
	void aufraeumen() {
		this.jdbc.sql("delete from einladung where erstellt_von_id in (9201, 9202)").update();
	}

	@Nested
	@DisplayName("Main Success Scenario")
	class MainSuccessScenario {

		@Test
		@DisplayName("Steps 1-10: Einladungen erstellen und versenden")
		void einladungen_erstellen_und_versenden() {
			adressen("anna.beispiel@example.com\nben.muster@example.com");
			ButtonElement.getByText(page, "Einladungen erstellen und versenden").click();

			// Schritt 6: die Zusammenfassung
			PlaywrightAssertions.assertThat(page.getByText("Einladungen versenden?").first()).isVisible();
			PlaywrightAssertions.assertThat(page.getByText("Es werden 2 Einladungen für UC001 Alpha · UC001 Nord, "
					+ "Rolle UC001 Entwicklung erstellt und per E-Mail versendet. Gültigkeit: 30 Tage.")).isVisible();
			page.locator("#einladung-versenden").click();

			// Schritt 10: das Ergebnis
			PlaywrightAssertions.assertThat(page.getByText("2 Einladungen versendet")).isVisible();
			await().untilAsserted(() -> assertThat(MAIL.gesendet()).hasSize(2));
			assertThat(this.dbAnzahl()).isEqualTo(2L);
		}

		private long dbAnzahl() {
			return jdbc.sql("select count(*) from einladung where erstellt_von_id = 9201 and status = 'Versendet'")
				.query(Long.class)
				.single();
		}

	}

	@Nested
	@DisplayName("A1: Angaben fehlen oder sind ungültig")
	class AngabenUngueltig {

		@Test
		@DisplayName("Ohne Adresse und mit ungültiger Gültigkeit")
		void meldungen() {
			ButtonElement.getByText(page, "Einladungen erstellen und versenden").click();
			TextAreaElement.getByLabel(page, "E-Mail-Adressen der Teilnehmer").assertErrorMessage("Bitte mindestens eine Adresse eingeben.");

			adressen("anna.beispiel@example.com");
			IntegerFieldElement.getByLabel(page, "Gültigkeit in Tagen").setValue("31");
			ButtonElement.getByText(page, "Einladungen erstellen und versenden").click();
			IntegerFieldElement.getByLabel(page, "Gültigkeit in Tagen")
				.assertErrorMessage("Die Zahl muss zwischen 1 und 30 liegen.");
			assertThat(MAIL.gesendet()).isEmpty();
		}

	}

	@Nested
	@DisplayName("A3: Teamleitung bricht ab")
	class Abbruch {

		@Test
		@DisplayName("Abbruch bei der Zusammenfassung erstellt nichts")
		void abbruch() {
			adressen("anna.beispiel@example.com");
			ButtonElement.getByText(page, "Einladungen erstellen und versenden").click();
			PlaywrightAssertions.assertThat(page.getByText("Einladungen versenden?").first()).isVisible();

			page.locator("#zusammenfassung-abbrechen").click();

			PlaywrightAssertions.assertThat(page.getByText("Einladungen versenden?")).hasCount(0);
			assertThat(MAIL.gesendet()).isEmpty();
			assertThat(jdbc.sql("select count(*) from einladung where erstellt_von_id = 9201")
				.query(Long.class)
				.single()).isZero();
		}

	}

	@Nested
	@DisplayName("A4: Versand scheitert")
	class VersandScheitert {

		@Test
		@DisplayName("Nur die Adresse mit gescheitertem Versand wird gemeldet")
		void gescheiterte_adresse_wird_gemeldet() {
			MAIL.lehneAb("ben.muster@example.com");
			adressen("anna.beispiel@example.com\nben.muster@example.com");
			ButtonElement.getByText(page, "Einladungen erstellen und versenden").click();
			page.locator("#einladung-versenden").click();

			PlaywrightAssertions.assertThat(page.getByText("1 Einladung versendet")).isVisible();
			PlaywrightAssertions.assertThat(page.getByText("1 Einladung nicht versendet")).isVisible();
			PlaywrightAssertions.assertThat(page.getByText("ben.muster@example.com")).isVisible();
			assertThat(MAIL.gesendet()).hasSize(1);
		}

	}

	private void adressen(String text) {
		TextAreaElement.getByLabel(this.page, "E-Mail-Adressen der Teilnehmer").setValue(text);
	}

}
