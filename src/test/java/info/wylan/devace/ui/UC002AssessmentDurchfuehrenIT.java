package info.wylan.devace.ui;

import java.time.LocalDate;
import java.util.UUID;

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
 * Browser test für UC-002 "Assessment durchführen". Erwartete Werte stammen aus der Spezifikation und den Testdaten
 * (R__uc002_assessment.sql). Der Teilnehmer meldet sich nicht an; die Referenz seiner Einladung steht in der Adresse.
 * Das KI-Modell ist durch {@link KiModellClientFake} ersetzt. A2, A5, A6 und die Grenzen der Regeln prüfen die
 * Browserless-Tests; hier läuft, was der Teilnehmer im Browser sieht.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DisplayName("UC-002: Assessment durchführen")
class UC002AssessmentDurchfuehrenIT extends AbstractBasePlaywrightIT {

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

	@BeforeEach
	void warteAufStarthinweis() {
		KI.reset();
		PlaywrightAssertions.assertThat(this.page.getByText("Starthinweis").first()).isVisible();
	}

	@AfterEach
	void aufraeumen() {
		this.jdbc.sql("delete from assessment where einladung_id in (select id from einladung where erstellt_von_id = 9301)")
			.update();
		this.jdbc.sql("delete from einladung where erstellt_von_id = 9301").update();
	}

	@Nested
	@DisplayName("Main Success Scenario")
	class MainSuccessScenario {

		@Test
		@DisplayName("Steps 1-8: Starthinweis bestätigen und alle Fragen beantworten")
		void starthinweis_bestaetigen_und_fragen_beantworten() {
			PlaywrightAssertions.assertThat(page.getByText("UC002 Jev (Bewertung)")).isVisible();
			// NFR-010: die Seite meldet ihre Sprache (WCAG 3.1.1)
			PlaywrightAssertions.assertThat(page.locator("html")).hasAttribute("lang", "de-CH");
			PlaywrightAssertions.assertThat(page.locator("#assessment-starten")).isDisabled();

			page.locator("#assessment-bestaetigung").click();
			page.locator("#assessment-starten").click();

			PlaywrightAssertions.assertThat(page.getByText("Frage 1 von 2")).isVisible();
			PlaywrightAssertions.assertThat(page.getByText("UC002 Wie begründen Sie eine Architekturentscheidung?")).isVisible();
			antworten("Ich halte Kontext, Alternativen und Folgen fest.");

			PlaywrightAssertions.assertThat(page.getByText("Frage 2 von 2")).isVisible();
			antworten("Wir planen in jedem Sprint Zeit für Aufräumarbeiten ein.");

			PlaywrightAssertions.assertThat(page.getByText("Alle Fragen beantwortet")).isVisible();
			PlaywrightAssertions.assertThat(page.locator("#assessment-abschliessen")).isDisabled();
			assertThat(jdbc.sql("""
					select count(*) from bewertung b join antwort an on an.id = b.antwort_id
					join assessment a on a.id = an.assessment_id join einladung e on e.id = a.einladung_id
					where e.referenz = ?
					""").param(referenz).query(Long.class).single()).isEqualTo(2L);
		}

	}

	@Nested
	@DisplayName("A1: Einladung unbekannt, eingelöst oder abgelaufen")
	class EinladungNichtNutzbar {

		@Test
		@DisplayName("Unbekannte Einladung")
		void unbekannte_einladung() {
			page.navigate(getUrl() + "assessment/" + UUID.randomUUID());

			PlaywrightAssertions.assertThat(page.getByText("Einladung nicht nutzbar")).isVisible();
			PlaywrightAssertions.assertThat(page.getByText("Diese Einladung wurde bereits eingelöst oder ist abgelaufen."))
				.isVisible();
		}

	}

	@Nested
	@DisplayName("A3: Teilnehmer bestätigt den Starthinweis nicht")
	class NichtBestaetigt {

		@Test
		@DisplayName("Nicht starten startet kein Assessment")
		void nicht_starten() {
			page.locator("#assessment-nicht-starten").click();

			PlaywrightAssertions.assertThat(page.getByText("Starthinweis")).hasCount(0);
			assertThat(jdbc.sql("select count(*) from assessment a join einladung e on e.id = a.einladung_id where e.referenz = ?")
				.param(referenz)
				.query(Long.class)
				.single()).isZero();
		}

	}

	@Nested
	@DisplayName("A4: Antwort leer oder zu lang")
	class AntwortUngueltig {

		@Test
		@DisplayName("Zu lange Antwort")
		void zu_lange_antwort() {
			page.locator("#assessment-bestaetigung").click();
			page.locator("#assessment-starten").click();

			antworten("a".repeat(501));

			PlaywrightAssertions.assertThat(page.getByText("Die Antwort ist zu lang: 501 von höchstens 500 Zeichen.").first())
				.isVisible();
			PlaywrightAssertions.assertThat(page.getByText("Frage 1 von 2")).isVisible();
		}

	}

	private void antworten(String text) {
		this.page.locator("#assessment-antwort textarea").fill(text);
		this.page.locator("#assessment-senden").click();
	}

	private String einladung() {
		String neu = UUID.randomUUID().toString();
		LocalDate heute = LocalDate.now();
		this.jdbc.sql("""
				insert into einladung (referenz, mag_id, rolle_id, erstellt_von_id, status, erstellt_am, gueltigkeit_tage, laeuft_ab_am)
				values (?, 9301, 9301, 9301, 'Versendet', ?, 30, ?)
				""").params(neu, heute.atTime(9, 0), heute.plusDays(29)).update();
		return neu;
	}

}
