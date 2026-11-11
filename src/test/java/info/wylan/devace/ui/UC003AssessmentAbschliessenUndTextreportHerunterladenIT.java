package info.wylan.devace.ui;

import java.nio.file.Files;
import java.time.LocalDate;
import java.util.OptionalInt;
import java.util.UUID;

import com.microsoft.playwright.Download;
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
import org.springframework.test.context.bean.override.convention.TestBean;
import org.vaadin.addons.dramafinder.AbstractBasePlaywrightIT;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Browser test für UC-003 "Assessment abschließen und Textreport herunterladen". Erwartete Werte stammen aus der
 * Spezifikation, den beantworteten Fragen zu ihrem Wortlaut (OQ-069, OQ-070) und den Testdaten
 * (R__uc003_abschluss.sql). Das KI-Modell ist durch {@link KiModellClientFake} ersetzt. Der Verfall nach einer Stunde
 * und die Grenzen der Regeln prüfen die Browserless-Tests; hier läuft, was der Teilnehmer im Browser sieht.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DisplayName("UC-003: Assessment abschließen und Textreport herunterladen")
class UC003AssessmentAbschliessenUndTextreportHerunterladenIT extends AbstractBasePlaywrightIT {

	private static final KiModellClientFake KI = new KiModellClientFake();

	@LocalServerPort
	private int port;

	@TestBean
	KiModellClient kiModellClient;

	@Autowired
	private JdbcClient jdbc;

	private String referenz;

	static KiModellClient kiModellClient() {
		return KI;
	}

	@Override
	public String getUrl() {
		return String.format("http://localhost:%d/", this.port);
	}

	@Override
	public String getView() {
		if (this.referenz == null) {
			this.referenz = einladung();
		}
		return "assessment/" + this.referenz;
	}

	/** Vorbedingung: alle fünf Fragen beantwortet, mit den Stufen 4, 3, 3, 3 und 2. */
	@BeforeEach
	void alleFragenBeantworten() {
		KI.reset();
		PlaywrightAssertions.assertThat(this.page.getByText("Starthinweis").first()).isVisible();
		this.page.locator("#assessment-bestaetigung").click();
		this.page.locator("#assessment-starten").click();
		int[] stufen = { 4, 3, 3, 3, 2 };
		for (int i = 0; i < stufen.length; i++) {
			PlaywrightAssertions.assertThat(this.page.getByText("Frage " + (i + 1) + " von 5")).isVisible();
			KI.bewertetNaechsteMit(OptionalInt.of(stufen[i]));
			this.page.locator("#assessment-antwort textarea").fill("Antwort " + (i + 1) + ".");
			this.page.locator("#assessment-senden").click();
		}
		PlaywrightAssertions.assertThat(this.page.getByText("Alle Fragen beantwortet")).isVisible();
	}

	@AfterEach
	void aufraeumen() {
		this.jdbc.sql("delete from score where mag_id = 9401").update();
		this.jdbc.sql("delete from assessment where einladung_id in (select id from einladung where erstellt_von_id = 9401)")
			.update();
		this.jdbc.sql("delete from einladung where erstellt_von_id = 9401").update();
	}

	@Nested
	@DisplayName("Main Success Scenario")
	class MainSuccessScenario {

		@Test
		@DisplayName("Steps 1-7: abschließen, Auswertung sehen und Textreport als PDF herunterladen")
		void abschliessen_und_herunterladen() throws Exception {
			page.locator("#assessment-abschliessen").click();

			// Schritt 5: Auswertung mit Radar-Chart, Scores mit einer Nachkommastelle und Textreport
			PlaywrightAssertions.assertThat(page.locator(".devace-seitentitel").getByText("Ihre Auswertung")).isVisible();
			PlaywrightAssertions.assertThat(page.getByRole(com.microsoft.playwright.options.AriaRole.IMG,
					new com.microsoft.playwright.Page.GetByRoleOptions().setName("Radar-Chart der Scores je Handlungsfeld")))
				.isVisible();
			PlaywrightAssertions.assertThat(page.locator("#auswertung-scores")).containsText("3.3");
			PlaywrightAssertions.assertThat(page.locator("#auswertung-scores")).containsText("2.0");
			PlaywrightAssertions.assertThat(page.locator("#textreport-ueberblick")).hasText("Überblick des KI-Modells.");
			PlaywrightAssertions
				.assertThat(page.getByText("Scores und Textreport sind kein Bestandteil der Leistungsbeurteilung.",
						new com.microsoft.playwright.Page.GetByTextOptions().setExact(false)))
				.isVisible();

			// Schritte 6 und 7: der Textreport als PDF (OQ-070)
			Download download = page.waitForDownload(() -> page.locator("#textreport-herunterladen").click());
			byte[] pdf = Files.readAllBytes(download.path());
			assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
			assertThat(download.suggestedFilename()).endsWith(".pdf");
		}

	}

	@Nested
	@DisplayName("A1: KI-Modell liefert keinen Textreport")
	class TextreportNichtErstellt {

		@Test
		@DisplayName("Hinweis und erneuter Abschluss")
		void hinweis_und_erneut_anfordern() {
			KI.failNextTextreport();

			page.locator("#assessment-abschliessen").click();

			PlaywrightAssertions.assertThat(page.locator("#textreport-nicht-erstellt")).containsText("Textreport nicht erstellt");
			PlaywrightAssertions.assertThat(page.locator("#textreport-nicht-erstellt"))
				.containsText("Das KI-Modell hat keinen Textreport geliefert. Es wurde nichts gespeichert, Ihre Antworten bleiben erhalten. "
						+ "Sie können den Abschluss erneut anfordern.");

			page.locator("#assessment-abschluss-erneut").click();

			PlaywrightAssertions.assertThat(page.locator(".devace-seitentitel").getByText("Ihre Auswertung")).isVisible();
		}

	}

	@Nested
	@DisplayName("A2: Teilnehmer lädt den Textreport nicht herunter")
	class NichtHeruntergeladen {

		@Test
		@DisplayName("Der Textreport wird nicht erneut angeboten")
		void nicht_erneut_angeboten() {
			page.locator("#assessment-abschliessen").click();
			PlaywrightAssertions.assertThat(page.locator(".devace-seitentitel").getByText("Ihre Auswertung")).isVisible();

			page.navigate(getUrl() + getView());

			PlaywrightAssertions.assertThat(page.getByText("Diese Einladung wurde bereits eingelöst oder ist abgelaufen."))
				.isVisible();
			PlaywrightAssertions.assertThat(page.locator("#textreport-herunterladen")).hasCount(0);
		}

	}

	@Nested
	@DisplayName("A3: Textreport nicht mehr verfügbar")
	class NichtMehrVerfuegbar {

		@Test
		@DisplayName("Zweite Anforderung nach dem Download")
		void zweite_anforderung() {
			page.locator("#assessment-abschliessen").click();
			page.waitForDownload(() -> page.locator("#textreport-herunterladen").click());

			page.locator("#textreport-herunterladen").click();

			PlaywrightAssertions.assertThat(page.locator("#textreport-nicht-verfuegbar")).containsText("Textreport nicht mehr verfügbar");
			PlaywrightAssertions.assertThat(page.locator("#textreport-nicht-verfuegbar"))
				.containsText("Der Textreport wurde bereits heruntergeladen oder ist verfallen. Ihre Scores bleiben ohne Personenbezug gespeichert.");
		}

	}

	private String einladung() {
		String neu = UUID.randomUUID().toString();
		LocalDate heute = LocalDate.now();
		this.jdbc.sql("""
				insert into einladung (referenz, mag_id, rolle_id, erstellt_von_id, status, erstellt_am, gueltigkeit_tage, laeuft_ab_am)
				values (?, 9401, 9401, 9401, 'Versendet', ?, 30, ?)
				""").params(neu, heute.atTime(9, 0), heute.plusDays(29)).update();
		return neu;
	}

}
