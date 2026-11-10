package info.wylan.devace.ui;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import info.wylan.devace.TestcontainersConfiguration;
import info.wylan.devace.service.KiModellClient;
import info.wylan.devace.service.KiModellClientFake;
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
import org.springframework.test.context.bean.override.convention.TestBean;
import org.vaadin.addons.dramafinder.AbstractBasePlaywrightIT;
import org.vaadin.addons.dramafinder.element.ButtonElement;
import org.vaadin.addons.dramafinder.element.DialogElement;
import org.vaadin.addons.dramafinder.element.GridElement;
import org.vaadin.addons.dramafinder.element.IntegerFieldElement;
import org.vaadin.addons.dramafinder.element.NotificationElement;
import org.vaadin.addons.dramafinder.element.PasswordFieldElement;
import org.vaadin.addons.dramafinder.element.TextAreaElement;
import org.vaadin.addons.dramafinder.element.TextFieldElement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Browser test für UC-006 "Fragenkatalog und Entscheidungsmodell erstellen". Erwartete Werte stammen aus der
 * Spezifikation und den Testdaten (R__uc006_fragenkatalog.sql). Die Randwerte der Geschäftsregeln prüfen die
 * Browserless-Tests; hier läuft, was die Administratorin im Browser sieht. Das KI-Modell ist durch
 * {@link KiModellClientFake} ersetzt, kein Test erreicht ein echtes Modell. Die Hinweise auf die Meldung eines
 * Banners (A2, A4) sind reiner Text ohne Komponente und werden über ihren sichtbaren Text gefunden.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DisplayName("UC-006: Fragenkatalog und Entscheidungsmodell erstellen")
class UC006FragenkatalogUndEntscheidungsmodellErstellenIT extends AbstractBasePlaywrightIT {

	private static final String LEER = "UC006 Leer";

	private static final String ADMIN = "uc006-e2e-admin";

	// zufällig je Lauf: das Startkonto der Anwendung legt den Administrator damit an, nichts davon liegt im Repository
	private static final String ADMIN_PASSWORT = UUID.randomUUID().toString();

	private static final KiModellClientFake KI = new KiModellClientFake();

	@LocalServerPort
	private int port;

	@TestBean
	KiModellClient kiModellClient;

	@Autowired
	private JdbcClient jdbc;

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
		return "fragenkatalog-erstellen";
	}

	@BeforeEach
	void anmelden() {
		KI.reset();
		// Der Aufruf ohne Anmeldung führt zur Anmeldeseite (UC-006 GR-009, NFR-005).
		TextFieldElement.getByLabel(this.page, "Benutzername").setValue(ADMIN);
		PasswordFieldElement.getByLabel(this.page, "Passwort").setValue(ADMIN_PASSWORT);
		ButtonElement.getByText(this.page, "Anmelden").click();
		this.page.waitForURL(url -> !url.contains("login"));
		this.page.navigate(getUrl() + getView());
		GridElement.getById(this.page, "rolle-grid").waitForGridToStopLoading();
	}

	@AfterEach
	void aufraeumen() {
		// Die Oberfläche kann Entwürfe nicht löschen: gezielt nur, was dieser Test gespeichert hat (Ids unter 9000).
		String katalog = "select id from fragenkatalog where id < 9000 and rolle_id in (9101, 9102, 9103)";
		this.jdbc.sql("delete from stufenvorgabe where frage_id in (select id from frage where fragenkatalog_id in ("
				+ katalog + "))").update();
		this.jdbc.sql("delete from frage where fragenkatalog_id in (" + katalog + ")").update();
		this.jdbc.sql("delete from fragenkatalog where id < 9000 and rolle_id in (9101, 9102, 9103)").update();
	}

	@Nested
	@DisplayName("Main Success Scenario")
	class MainSuccessScenario {

		@Test
		@DisplayName("Steps 1-12: Entwurf mit KI-Vorschlag erstellen und speichern")
		void ki_entwurf_erstellen_und_speichern() {
			beginnen(LEER, "Katalog beginnen");
			IntegerFieldElement.getByLabel(page, "Fragen je Handlungsfeld").setValue("1");
			ButtonElement.getByText(page, "KI-Entwurf anfordern").click();

			// Schritt 7: der Entwurf je Handlungsfeld; das erste Handlungsfeld ist geöffnet
			GridElement fragen = fragenGrid(1);
			PlaywrightAssertions.assertThat(fragen.findCell(0, "Frage").get().getCellContentLocator())
				.hasText("KI-Frage 1.1?");

			ButtonElement.getByText(page, "Entwurf speichern").click();

			// Schritt 12: die Bestätigung; die Übersicht zeigt den Entwurf der Rolle
			PlaywrightAssertions.assertThat(new NotificationElement(page).getContentLocator())
				.hasText("Entwurf gespeichert.");
			GridElement rollen = GridElement.getById(page, "rolle-grid");
			rollen.waitForGridToStopLoading();
			int zeile = rollen.findRowIndexesWithColumnText(0, LEER).get(0);
			PlaywrightAssertions.assertThat(rollen.findCell(zeile, 2).get().getCellContentLocator())
				.hasText("Version 1");
		}

	}

	@Nested
	@DisplayName("A1: Ohne KI-Entwurf")
	class OhneKiEntwurf {

		@Test
		@DisplayName("Frage und Entscheidungsmodell selbst erfassen und speichern")
		void selbst_erfassen() {
			beginnen(LEER, "Katalog beginnen");
			ButtonElement.getByText(page, "Frage hinzufügen").click();
			frageAusfuellen("UC006 Wie gehen Sie mit Anforderungen um?", "v0", "v1", "v2", "v3", "v4", "v5");
			ButtonElement.getByText(page, "Übernehmen").click();

			GridElement fragen = fragenGrid(1);
			PlaywrightAssertions.assertThat(fragen.findCell(0, "Frage").get().getCellContentLocator())
				.hasText("UC006 Wie gehen Sie mit Anforderungen um?");
			ButtonElement.getByText(page, "Entwurf speichern").click();

			PlaywrightAssertions.assertThat(new NotificationElement(page).getContentLocator())
				.hasText("Entwurf gespeichert.");
		}

	}

	@Nested
	@DisplayName("A2: KI-Modell liefert keinen Entwurf")
	class KiModellLiefertKeinenEntwurf {

		@Test
		@DisplayName("Hinweis, Angaben bleiben, erneuter Versuch bei Schritt 5")
		void hinweis_und_erneuter_versuch() {
			beginnen(LEER, "Katalog beginnen");
			IntegerFieldElement.getByLabel(page, "Fragen je Handlungsfeld").setValue("2");

			KI.failNext();
			ButtonElement.getByText(page, "KI-Entwurf anfordern").click();

			PlaywrightAssertions.assertThat(page.getByText("Das KI-Modell hat keinen Entwurf geliefert.")).isVisible();
			IntegerFieldElement.getByLabel(page, "Fragen je Handlungsfeld").assertValue("2");

			ButtonElement.getByText(page, "KI-Entwurf anfordern").click();
			fragenGrid(2);
		}

	}

	@Nested
	@DisplayName("A3: Angaben für den KI-Entwurf ungültig")
	class AngabenUngueltig {

		@Test
		@DisplayName("Zahl der Fragen außerhalb von 1 bis 10")
		void zahl_ausserhalb() {
			beginnen(LEER, "Katalog beginnen");
			IntegerFieldElement anzahl = IntegerFieldElement.getByLabel(page, "Fragen je Handlungsfeld");
			anzahl.setValue("11");

			ButtonElement.getByText(page, "KI-Entwurf anfordern").click();

			anzahl.assertErrorMessage("Die Zahl muss zwischen 1 und 10 liegen.");
			assertThat(KI.anfragen()).isEmpty();
		}

	}

	@Nested
	@DisplayName("A4: Entwurf unvollständig oder ungültig")
	class EntwurfUnvollstaendig {

		@Test
		@DisplayName("Fehlende Vorgabe einer Stufe wird gemeldet und lässt sich korrigieren")
		void fehlende_vorgabe() {
			beginnen(LEER, "Katalog beginnen");
			ButtonElement.getByText(page, "Frage hinzufügen").click();
			frageAusfuellen("UC006 Frage ohne Stufe 3?", "v0", "v1", "v2", "", "v4", "v5");
			ButtonElement.getByText(page, "Übernehmen").click();

			ButtonElement.getByText(page, "Entwurf speichern").click();

			PlaywrightAssertions.assertThat(page.getByText("Vorgabe fehlt.")).isVisible();
			GridElement fragen = fragenGrid(1);
			ButtonElement.getByText(fragen.findCell(0, 3).get().getCellContentLocator(), "Bearbeiten").click();
			TextAreaElement stufe3 = TextAreaElement.getByLabel(page, "Stufe 3 – Sicherer Umgang");
			stufe3.assertErrorMessage("Vorgabe fehlt.");

			stufe3.setValue("v3");
			ButtonElement.getByText(page, "Übernehmen").click();
			ButtonElement.getByText(page, "Entwurf speichern").click();
			PlaywrightAssertions.assertThat(new NotificationElement(page).getContentLocator())
				.hasText("Entwurf gespeichert.");
		}

	}

	@Nested
	@DisplayName("A5: Administrator bricht ab")
	class Abbruch {

		@Test
		@DisplayName("Abbrechen verwirft die Änderungen dieser Sitzung")
		void abbrechen() {
			beginnen(LEER, "Katalog beginnen");
			ButtonElement.getByText(page, "Frage hinzufügen").click();
			frageAusfuellen("UC006 Verworfen?", "v0", "v1", "v2", "v3", "v4", "v5");
			ButtonElement.getByText(page, "Übernehmen").click();
			fragenGrid(1);

			ButtonElement.getByText(page, "Abbrechen").click();

			GridElement rollen = GridElement.getById(page, "rolle-grid");
			rollen.waitForGridToStopLoading();
			int zeile = rollen.findRowIndexesWithColumnText(0, LEER).get(0);
			PlaywrightAssertions.assertThat(rollen.findCell(zeile, 2).get().getCellContentLocator()).hasText("");
			PlaywrightAssertions.assertThat(rollen.findCell(zeile, 3).get().getCellContentLocator())
				.containsText("Katalog beginnen");
		}

	}

	@Nested
	@DisplayName("A6: Genügend Fragen vorhanden")
	class GenuegendFragen {

		@Test
		@DisplayName("Hinweis statt KI-Anfrage, weitere Fragen ergänzen")
		void hinweis_und_weitere_fragen() {
			beginnen(LEER, "Katalog beginnen");
			IntegerFieldElement.getByLabel(page, "Fragen je Handlungsfeld").setValue("1");
			ButtonElement.getByText(page, "KI-Entwurf anfordern").click();
			fragenGrid(1);

			// jedes Handlungsfeld hat schon eine Frage: Hinweis statt Anfrage an das KI-Modell
			ButtonElement.getByText(page, "KI-Entwurf anfordern").click();
			DialogElement hinweis = DialogElement.getByHeaderText(page, "Genügend Fragen vorhanden");
			hinweis.assertOpen();
			assertThat(KI.anfragen()).hasSize(1);

			// A6: weitere Fragen ergänzen
			IntegerFieldElement.getByLabel(page, "Weitere Fragen je Handlungsfeld").setValue("1");
			ButtonElement.getByText(hinweis.getLocator(), "Weitere Fragen ergänzen").click();

			fragenGrid(2);
		}

	}

	@Nested
	@DisplayName("A7: Administrator bricht die KI-Anforderung ab")
	class KiAnforderungAbbrechen {

		@Test
		@DisplayName("Abbruch nach dem Hinweis lässt den Entwurf unverändert")
		void abbruch_nach_dem_hinweis() {
			beginnen(LEER, "Katalog beginnen");
			IntegerFieldElement.getByLabel(page, "Fragen je Handlungsfeld").setValue("1");
			ButtonElement.getByText(page, "KI-Entwurf anfordern").click();
			fragenGrid(1);
			ButtonElement.getByText(page, "KI-Entwurf anfordern").click();
			DialogElement hinweis = DialogElement.getByHeaderText(page, "Genügend Fragen vorhanden");
			hinweis.assertOpen();

			ButtonElement.getByText(hinweis.getLocator(), "Abbrechen").click();

			hinweis.assertClosed();
			fragenGrid(1);
			assertThat(KI.anfragen()).hasSize(1);
		}

	}

	/** Das Raster des ersten, geöffneten Handlungsfelds; wartet, bis es die erwartete Zahl Fragen zeigt. */
	private GridElement fragenGrid(int erwartet) {
		GridElement grid = GridElement.get(this.page);
		grid.assertVisible();
		await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
			grid.waitForGridToStopLoading();
			assertThat(grid.getTotalRowCount()).isEqualTo(erwartet);
		});
		return grid;
	}

	private void beginnen(String rolle, String aktion) {
		GridElement rollen = GridElement.getById(this.page, "rolle-grid");
		rollen.waitForGridToStopLoading();
		List<Integer> zeilen = rollen.findRowIndexesWithColumnText(0, rolle);
		assertThat(zeilen).hasSize(1);
		Locator zelle = rollen.findCell(zeilen.get(0), 3).get().getCellContentLocator();
		ButtonElement.getByText(zelle, aktion).click();
	}

	private void frageAusfuellen(String wortlaut, String... vorgaben) {
		TextAreaElement.getByLabel(this.page, "Fragetext").setValue(wortlaut);
		String[] namen = { "Nie gehört", "Schon mal gehört", "Grundverständnis", "Sicherer Umgang",
				"Fortgeschrittene Expertise", "Expert:in / Lehrniveau" };
		for (int stufe = 0; stufe < vorgaben.length; stufe++) {
			TextAreaElement.getByLabel(this.page, "Stufe " + stufe + " – " + namen[stufe]).setValue(vorgaben[stufe]);
		}
	}

}
