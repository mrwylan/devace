package info.wylan.devace.ui;

import java.util.List;
import java.util.UUID;

import com.microsoft.playwright.Locator;
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
import org.vaadin.addons.dramafinder.element.DialogElement;
import org.vaadin.addons.dramafinder.element.GridElement;
import org.vaadin.addons.dramafinder.element.NotificationElement;
import org.vaadin.addons.dramafinder.element.PasswordFieldElement;
import org.vaadin.addons.dramafinder.element.TextFieldElement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Browser test für UC-007 "Fragenkatalog freigeben". Erwartete Werte stammen aus der Spezifikation, den Antworten
 * OQ-075 bis OQ-077 und den Testdaten (R__uc007_freigabe.sql). Die Randwerte der Geschäftsregeln prüfen die
 * Browserless-Tests; hier läuft, was der Administrator im Browser sieht. A1 (kein Entwurf vorhanden) prüft nur die
 * Integrationsschicht: die laufende Anwendung hat einen Datenbestand mit Entwürfen. Die Hinweise sind reiner Text
 * ohne eigene Komponente und werden über ihren sichtbaren Text gefunden.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DisplayName("UC-007: Fragenkatalog freigeben")
class UC007FragenkatalogFreigebenIT extends AbstractBasePlaywrightIT {

	private static final String NACHFOLGER = "UC007 Nachfolger";

	private static final String UNVOLLSTAENDIG = "UC007 Unvollständig";

	private static final String ADMIN = "uc007-e2e-admin";

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
		return "fragenkatalog-freigeben";
	}

	@BeforeEach
	void anmelden() {
		// Der Aufruf ohne Anmeldung führt zur Anmeldeseite (UC-007 BR-007, NFR-005).
		TextFieldElement.getByLabel(this.page, "Benutzername").setValue(ADMIN);
		PasswordFieldElement.getByLabel(this.page, "Passwort").setValue(ADMIN_PASSWORT);
		ButtonElement.getByText(this.page, "Anmelden").click();
		this.page.waitForURL(url -> !url.contains("login"));
		this.page.navigate(getUrl() + getView());
		GridElement.getById(this.page, "freigabe-grid").waitForGridToStopLoading();
	}

	@AfterEach
	void aufraeumen() {
		// Die Oberfläche kann eine Freigabe nicht zurücknehmen: gezielt nur die Freigabe der Rolle 9502 zurücksetzen,
		// zuerst den Entwurf, dann die bisherige Version (eine freigegebene Version je Rolle).
		this.jdbc.sql("""
				update fragenkatalog set status = 'Entwurf', freigegeben_am = null, freigegeben_von_id = null
				where id = 9503""").update();
		this.jdbc.sql("""
				update fragenkatalog set status = 'Freigegeben', freigegeben_am = timestamp '2026-01-02 10:00:00',
				freigegeben_von_id = 9301 where id = 9502""").update();
	}

	@Nested
	@DisplayName("Main Success Scenario")
	class MainSuccessScenario {

		@Test
		@DisplayName("Steps 1-9: Entwurf prüfen und freigeben")
		void entwurf_pruefen_und_freigeben() {
			// Schritte 3-4: Rolle wählen, Entwurf je Handlungsfeld mit Fragen und Entscheidungsmodell
			pruefen(NACHFOLGER);
			PlaywrightAssertions.assertThat(page.getByText("UC007 Frage 1 in Katalog 9503")).isVisible();
			PlaywrightAssertions.assertThat(page.getByText("Stufe 0 – Nie gehört: UC007 Vorgabe 0 zu Frage 95031"))
				.isVisible();

			// Schritte 5-6: Freigabe anfordern, Zusammenfassung
			ButtonElement.getByText(page, "Freigabe anfordern").click();
			DialogElement dialog = DialogElement.getByHeaderText(page, "Fragenkatalog freigeben");
			dialog.assertOpen();
			PlaywrightAssertions.assertThat(dialog.getLocator().getByText("Rolle " + NACHFOLGER + " · Version 2"))
				.isVisible();

			// Schritte 7-9: bestätigen, Bestätigung der Freigabe
			ButtonElement.getByText(dialog.getLocator(), "Freigeben").click();
			dialog.assertClosed();
			PlaywrightAssertions.assertThat(new NotificationElement(page).getContentLocator())
				.hasText("Fragenkatalog freigegeben.");

			// Die Rolle hat keinen Entwurf mehr und steht nicht mehr zur Auswahl.
			GridElement grid = GridElement.getById(page, "freigabe-grid");
			grid.waitForGridToStopLoading();
			assertThat(grid.findRowIndexesWithColumnText(0, NACHFOLGER)).isEmpty();
		}

	}

	@Nested
	@DisplayName("A2: Entwurf unvollständig")
	class A2EntwurfUnvollstaendig {

		@Test
		@DisplayName("A2: Hinweis auf Handlungsfelder ohne Frage und unvollständige Entscheidungsmodelle")
		void unvollstaendiger_entwurf_wird_nicht_freigegeben() {
			pruefen(UNVOLLSTAENDIG);

			ButtonElement.getByText(page, "Freigabe anfordern").click();

			PlaywrightAssertions.assertThat(page.getByText("Der Entwurf ist unvollständig und wird nicht freigegeben."))
				.isVisible();
			PlaywrightAssertions.assertThat(page.getByText("Handlungsfelder ohne Frage:")).isVisible();
			PlaywrightAssertions
				.assertThat(page.getByText("Fragen mit unvollständigem Entscheidungsmodell: Frage 2"))
				.isVisible();
			PlaywrightAssertions.assertThat(page.getByRole(com.microsoft.playwright.options.AriaRole.DIALOG))
				.hasCount(0);
		}

	}

	@Nested
	@DisplayName("A3: Administrator bricht ab")
	class A3AdministratorBrichtAb {

		@Test
		@DisplayName("A3: Abbruch bei der Zusammenfassung und vor der Anforderung")
		void abbruch_gibt_nicht_frei() {
			pruefen(NACHFOLGER);
			ButtonElement.getByText(page, "Freigabe anfordern").click();
			DialogElement dialog = DialogElement.getByHeaderText(page, "Fragenkatalog freigeben");
			dialog.assertOpen();

			// Abbruch bei der Zusammenfassung (Schritt 7)
			ButtonElement.getByText(dialog.getLocator(), "Abbrechen").click();
			dialog.assertClosed();

			// Abbruch vor der Anforderung (Schritt 5): zurück zur Auswahl, der Entwurf steht weiter zur Freigabe
			// der geschlossene Dialog behält seinen „Abbrechen“-Knopf im DOM: auf die Karte des Entwurfs einschränken
			ButtonElement.getByText(page.locator(".devace-karte"), "Abbrechen").click();
			GridElement grid = GridElement.getById(page, "freigabe-grid");
			grid.waitForGridToStopLoading();
			List<Integer> zeilen = grid.findRowIndexesWithColumnText(0, NACHFOLGER);
			assertThat(zeilen).hasSize(1);
			PlaywrightAssertions.assertThat(grid.findCell(zeilen.get(0), 1).get().getCellContentLocator())
				.hasText("Version 2");
		}

	}

	private void pruefen(String rolle) {
		GridElement grid = GridElement.getById(this.page, "freigabe-grid");
		grid.waitForGridToStopLoading();
		List<Integer> zeilen = grid.findRowIndexesWithColumnText(0, rolle);
		assertThat(zeilen).hasSize(1);
		Locator zelle = grid.findCell(zeilen.get(0), 3).get().getCellContentLocator();
		ButtonElement.getByText(zelle, "Prüfen").click();
		PlaywrightAssertions.assertThat(this.page.getByText("Entwurf prüfen")).isVisible();
	}

}
