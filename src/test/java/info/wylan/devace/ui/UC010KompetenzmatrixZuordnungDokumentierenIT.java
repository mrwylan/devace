package info.wylan.devace.ui;

import java.util.UUID;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.AriaRole;
import info.wylan.devace.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
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
import org.vaadin.addons.dramafinder.AbstractBasePlaywrightIT;
import org.vaadin.addons.dramafinder.element.ButtonElement;
import org.vaadin.addons.dramafinder.element.ComboBoxElement;
import org.vaadin.addons.dramafinder.element.DialogElement;
import org.vaadin.addons.dramafinder.element.GridElement;
import org.vaadin.addons.dramafinder.element.TextFieldElement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Browser-Test für UC-010 "Kompetenzmatrix-Zuordnung dokumentieren". Die erwarteten Werte stammen aus der
 * Spezifikation und den Testdaten (R__uc010_kompetenzmatrix.sql). Die Grenzen der Geschäftsregeln prüfen die
 * Browserless-Tests. Das Anmelden prüft BR-004 mit.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DisplayName("UC-010: Kompetenzmatrix-Zuordnung dokumentieren")
class UC010KompetenzmatrixZuordnungDokumentierenIT extends AbstractBasePlaywrightIT {

	private static final String ADMIN = "uc010-e2e-admin";

	// zufällig je Lauf: das Startkonto der Anwendung legt den Administrator damit an, nichts davon liegt im Repository
	private static final String ADMIN_PASSWORT = UUID.randomUUID().toString();

	private static final String NEUE_SPALTE = "UC010E Terraform";

	private static final String HF4 = "4 Security & DevSecOps";

	@LocalServerPort
	private int port;

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
		return "kompetenzmatrix";
	}

	@BeforeEach
	void anmelden() {
		// Der Aufruf ohne Anmeldung führt zur Anmeldeseite (UC-010 BR-004, NFR-005).
		Anmeldung.anmelden(this.page, ADMIN, ADMIN_PASSWORT);
		this.page.navigate(getUrl() + getView());
	}

	/** Entfernt die angelegte Spalte über die Oberfläche, falls der Test vor dem Entfernen abgebrochen ist. */
	@AfterEach
	void aufraeumen() {
		this.page.navigate(getUrl() + getView());
		GridElement grid = GridElement.getById(this.page, "matrix-grid-4");
		grid.waitForGridToStopLoading();
		if (!grid.findRowIndexesWithColumnText(0, NEUE_SPALTE).isEmpty()) {
			ButtonElement.getByText(this.page, "Zuordnung " + NEUE_SPALTE + " entfernen").click();
			ButtonElement.getByText(bestaetigung(this.page), "Entfernen").click();
		}
	}

	/** Drama Finder 1.x hat keinen ConfirmDialogElement: der Dialog nach Rolle und Name, auf sein Tag begrenzt. */
	private static Locator bestaetigung(Page seite) {
		return seite.getByRole(AriaRole.ALERTDIALOG, new Page.GetByRoleOptions().setName("Zuordnung entfernen"))
			.and(seite.locator("vaadin-confirm-dialog"));
	}

	@Nested
	@DisplayName("Main Success Scenario")
	class MainSuccessScenario {

		@Test
		@DisplayName("Steps 1-5: Zuordnungen je Handlungsfeld sehen, eine Zuordnung anlegen und nach Bestätigung entfernen")
		void anlegen_und_entfernen() {
			// Schritt 2: die vorhandenen Zuordnungen je Handlungsfeld
			GridElement feld2 = GridElement.getById(page, "matrix-grid-2");
			feld2.waitForGridToStopLoading();
			assertThat(feld2.findRowIndexesWithColumnText(0, "UC010 Docker")).hasSize(1);

			// Schritte 3 bis 5: anlegen und speichern
			ButtonElement.getByText(page, "Zuordnung anlegen").click();
			DialogElement dialog = DialogElement.getByHeaderText(page, "Zuordnung anlegen");
			TextFieldElement.getByLabel(page, "Spalte der Kompetenzmatrix").setValue(NEUE_SPALTE);
			ComboBoxElement.getByLabel(page, "Handlungsfeld").selectItem(HF4);
			ButtonElement.getByText(page, "Speichern").click();
			dialog.assertClosed();
			GridElement feld4 = GridElement.getById(page, "matrix-grid-4");
			PlaywrightAssertions.assertThat(feld4.getLocator()).containsText(NEUE_SPALTE);

			// Schritte 3 bis 5: entfernen, mit Bestätigung (Schritt 4)
			ButtonElement.getByText(page, "Zuordnung " + NEUE_SPALTE + " entfernen").click();
			Locator bestaetigung = bestaetigung(page);
			PlaywrightAssertions.assertThat(bestaetigung).hasCount(1);
			ButtonElement.getByText(bestaetigung, "Entfernen").click();
			PlaywrightAssertions.assertThat(bestaetigung).hasCount(0);
			PlaywrightAssertions.assertThat(GridElement.getById(page, "matrix-grid-4").getLocator())
				.not()
				.containsText(NEUE_SPALTE);
		}

	}

	@Nested
	@DisplayName("A1: Angaben fehlen oder sind ungültig")
	class A1AngabenFehlen {

		@Test
		@DisplayName("Ohne Namen der Spalte wird am Feld gemeldet")
		void ohne_namen() {
			ButtonElement.getByText(page, "Zuordnung anlegen").click();
			DialogElement dialog = DialogElement.getByHeaderText(page, "Zuordnung anlegen");
			ComboBoxElement.getByLabel(page, "Handlungsfeld").selectItem(HF4);
			ButtonElement.getByText(page, "Speichern").click();

			TextFieldElement.getByLabel(page, "Spalte der Kompetenzmatrix")
				.assertErrorMessage("Pflichtangabe fehlt.");
			dialog.assertOpen();
		}

	}

	@Nested
	@DisplayName("A2: Spalte bereits zugeordnet")
	class A2BereitsZugeordnet {

		@Test
		@DisplayName("Eine zugeordnete Spalte in anderer Schreibweise nennt ihr Handlungsfeld")
		void bereits_zugeordnet() {
			ButtonElement.getByText(page, "Zuordnung anlegen").click();
			DialogElement dialog = DialogElement.getByHeaderText(page, "Zuordnung anlegen");
			TextFieldElement spalte = TextFieldElement.getByLabel(page, "Spalte der Kompetenzmatrix");
			spalte.setValue("uc010 docker");
			ComboBoxElement.getByLabel(page, "Handlungsfeld").selectItem(HF4);
			ButtonElement.getByText(page, "Speichern").click();

			spalte.assertErrorMessage("Diese Spalte ist bereits dem Handlungsfeld 2 Architektur & Engineering zugeordnet.");
			dialog.assertOpen();
		}

	}

	@Nested
	@DisplayName("A3: Administrator bricht ab")
	class A3Abbrechen {

		@Test
		@DisplayName("Abbrechen beim Ändern verwirft die Angaben")
		void abbrechen_beim_aendern() {
			ButtonElement.getByText(page, "Zuordnung UC010 Scrum ändern").click();
			DialogElement dialog = DialogElement.getByHeaderText(page, "Zuordnung ändern");
			TextFieldElement.getByLabel(page, "Spalte der Kompetenzmatrix").setValue("UC010E Verworfen");
			ButtonElement.getByText(page, "Abbrechen").click();

			dialog.assertClosed();
			PlaywrightAssertions.assertThat(GridElement.getById(page, "matrix-grid-5").getLocator())
				.containsText("UC010 Scrum");
			PlaywrightAssertions.assertThat(GridElement.getById(page, "matrix-grid-5").getLocator())
				.not()
				.containsText("UC010E Verworfen");
		}

		@Test
		@DisplayName("Abbrechen beim Entfernen behält die Zuordnung")
		void abbrechen_beim_entfernen() {
			ButtonElement.getByText(page, "Zuordnung UC010 Scrum entfernen").click();
			Locator bestaetigung = bestaetigung(page);
			ButtonElement.getByText(bestaetigung, "Abbrechen").click();

			PlaywrightAssertions.assertThat(bestaetigung).hasCount(0);
			PlaywrightAssertions.assertThat(GridElement.getById(page, "matrix-grid-5").getLocator())
				.containsText("UC010 Scrum");
		}

	}

}
