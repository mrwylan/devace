package info.wylan.devace.ui;

import java.time.LocalDate;

import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextArea;
import info.wylan.devace.TestcontainersConfiguration;
import info.wylan.devace.service.KiModellClient;
import info.wylan.devace.service.KiModellClient.BewertungAnfrage;
import info.wylan.devace.service.KiModellClientFake;
import info.wylan.devace.usecase.UseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.convention.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UC-002 Assessment durchführen, getestet über die Ansicht mit echter Datenbank. Das KI-Modell ist durch
 * {@link KiModellClientFake} ersetzt; ohne Vorgabe ordnet es jeder Antwort Stufe 3 zu.
 *
 * <p>
 * Testdaten (R__uc002_assessment.sql): MAG 9301, Rolle 9301 mit freigegebenem Fragenkatalog 9301 aus zwei Fragen und
 * der Starthinweis 9301. Jeder Test legt seine Einladungen selbst an, weil der Start sie einlöst.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@WithAnonymousUser
class UC002AssessmentDurchfuehrenTest extends SpringBrowserlessTest {

	private static final KiModellClientFake KI = new KiModellClientFake();

	private static final String FRAGE_1 = "UC002 Wie begründen Sie eine Architekturentscheidung?";

	private static final String FRAGE_2 = "UC002 Wie gehen Sie mit technischen Schulden um?";

	private static final String NICHT_NUTZBAR = "Diese Einladung wurde bereits eingelöst oder ist abgelaufen.";

	@TestBean
	KiModellClient kiModellClient;

	@Autowired
	private JdbcClient jdbc;

	static KiModellClient kiModellClient() {
		return KI;
	}

	@BeforeEach
	void zuruecksetzen() {
		KI.reset();
	}

	@AfterEach
	void aufraeumen() {
		this.jdbc.sql("delete from assessment where einladung_id in (select id from einladung where erstellt_von_id = 9301)")
			.update();
		this.jdbc.sql("delete from einladung where erstellt_von_id = 9301").update();
	}

	// ---------------------------------------------------------------- Hauptablauf

	@Test
	@UseCase(id = "UC-002", scenario = "Main Success Scenario", businessRules = { "BR-001", "BR-002", "BR-003", "BR-006" })
	void teilnehmer_bestaetigt_den_starthinweis_und_beantwortet_alle_fragen() {
		String referenz = einladung(LocalDate.now(), 30, "Versendet");
		navigate(AssessmentView.class, referenz);

		// Schritt 2: der Starthinweis mit KI-Modellen und Hinweis auf nicht zu erfassende Angaben und Firmenweisungen
		assertThat(text()).contains("Starthinweis")
			.contains("UC002 Jev (Bewertung)")
			.contains("UC002 Nennen Sie keine Namen von Personen und keine Kundenprojekte. Es gelten die Firmenweisungen zum Umgang mit KI.")
			.contains("Rolle: UC002 Entwicklung");
		// BR-002: ohne Bestätigung lässt sich das Assessment nicht starten
		assertThat(button("assessment-starten").isEnabled()).isFalse();

		// Schritte 3 und 4
		test(find(Checkbox.class).withId("assessment-bestaetigung").single()).click();
		test(button("assessment-starten")).click();
		// BR-003: die Fragen in der Reihenfolge des Fragenkatalogs
		assertThat(text()).contains("Frage 1 von 2").contains(FRAGE_1).doesNotContain(FRAGE_2);

		// Schritte 5 bis 7, zweimal
		antworten("Ich halte Kontext, Alternativen und Folgen fest.");
		assertThat(text()).contains("Frage 2 von 2").contains(FRAGE_2);
		antworten("Wir planen in jedem Sprint Zeit für Aufräumarbeiten ein.");

		// Schritt 8: alle Fragen beantwortet, der Abschluss (UC-003) wird angeboten und ist ausführbar
		assertThat(find(Div.class).withId("assessment-fertig").single().getElement().getTextRecursively())
			.contains("Alle Fragen beantwortet")
			.contains("Sie haben alle 2 Fragen beantwortet.");
		assertThat(button("assessment-abschliessen").isEnabled()).isTrue();

		// Nachbedingungen: ein laufendes Assessment mit dem bestätigten Starthinweis, je Frage Antwort und Stufe
		assertThat(this.jdbc.sql("""
				select count(*) from assessment a join einladung e on e.id = a.einladung_id
				where e.referenz = ? and a.status = 'Laufend' and a.starthinweis_id = 9301 and a.fragenkatalog_id = 9301
				and a.mag_id = 9301 and a.rolle_id = 9301
				""").param(referenz).query(Long.class).single()).isEqualTo(1L);
		assertThat(stufen(referenz)).containsExactly(3, 3);
		// BR-001: mit dem Start ist die Einladung eingelöst
		assertThat(status(referenz)).isEqualTo("Eingelöst");
	}

	@Test
	@UseCase(id = "UC-002", scenario = "Main Success Scenario", businessRules = { "BR-007" }, requirements = { "NFR-001" })
	void das_kimodell_erhaelt_nur_frage_stufenvorgaben_und_antwort() {
		String referenz = einladung(LocalDate.now(), 30, "Versendet");
		starten(referenz);

		antworten("Ich halte Kontext, Alternativen und Folgen fest.");

		assertThat(KI.bewertungen()).containsExactly(new BewertungAnfrage(FRAGE_1,
				List.of("UC002 Vorgabe 9301.0", "UC002 Vorgabe 9301.1", "UC002 Vorgabe 9301.2", "UC002 Vorgabe 9301.3",
						"UC002 Vorgabe 9301.4", "UC002 Vorgabe 9301.5"),
				"Ich halte Kontext, Alternativen und Folgen fest."));
		// keine Einladung und keine andere Angabe zum Teilnehmer
		assertThat(KI.bewertungen().get(0).toString()).doesNotContain(referenz).doesNotContain("@");
	}

	@Test
	@UseCase(id = "UC-002", scenario = "Main Success Scenario", businessRules = { "BR-008" })
	void die_zugeordnete_stufe_bleibt_verborgen() {
		String referenz = einladung(LocalDate.now(), 30, "Versendet");
		starten(referenz);
		KI.bewertetNaechsteMit(OptionalInt.of(5));

		antworten("Ich halte Kontext, Alternativen und Folgen fest.");

		assertThat(stufen(referenz)).containsExactly(5);
		assertThat(text()).contains("Ich halte Kontext, Alternativen und Folgen fest.").doesNotContain("Stufe");
	}

	@Test
	@UseCase(id = "UC-002", scenario = "Main Success Scenario", businessRules = { "BR-005" })
	void jede_frage_wird_genau_einmal_beantwortet() {
		String referenz = einladung(LocalDate.now(), 30, "Versendet");
		starten(referenz);

		antworten("Erste Antwort.");

		// die beantwortete Frage wird nicht erneut gestellt, die Antwort bleibt unverändert
		assertThat(text()).contains("Frage 2 von 2").contains(FRAGE_2);
		assertThat(this.jdbc.sql("""
				select count(*) from antwort an join assessment a on a.id = an.assessment_id
				join einladung e on e.id = a.einladung_id where e.referenz = ? and an.frage_id = 9301
				""").param(referenz).query(Long.class).single()).isEqualTo(1L);
	}

	// ---------------------------------------------------------------- A1

	@Test
	@UseCase(id = "UC-002", scenario = "A1: Einladung unbekannt, eingelöst oder abgelaufen", businessRules = { "BR-001" })
	void unbekannte_einladung_ist_nicht_nutzbar() {
		navigate(AssessmentView.class, UUID.randomUUID().toString());

		assertThat(nichtNutzbar()).contains("Einladung nicht nutzbar").contains(NICHT_NUTZBAR);
	}

	@Test
	@UseCase(id = "UC-002", scenario = "A1: Einladung unbekannt, eingelöst oder abgelaufen", businessRules = { "BR-001" })
	void eingeloeste_einladung_ohne_laufendes_assessment_ist_nicht_nutzbar() {
		String referenz = einladung(LocalDate.now(), 30, "Eingelöst");

		navigate(AssessmentView.class, referenz);

		assertThat(nichtNutzbar()).contains(NICHT_NUTZBAR);
		assertThat(find(Checkbox.class).withId("assessment-bestaetigung").exists()).isFalse();
	}

	@Test
	@UseCase(id = "UC-002", scenario = "A1: Einladung unbekannt, eingelöst oder abgelaufen", businessRules = { "BR-001" })
	void einladung_nach_dem_ablaufdatum_ist_nicht_nutzbar() {
		// Ablaufdatum gestern: 30 Tage ab einer Erstellung vor 30 Tagen
		String referenz = einladung(LocalDate.now().minusDays(30), 30, "Versendet");

		navigate(AssessmentView.class, referenz);

		assertThat(nichtNutzbar()).contains(NICHT_NUTZBAR);
	}

	@Test
	@UseCase(id = "UC-002", scenario = "Main Success Scenario", businessRules = { "BR-001" })
	void einladung_gilt_bis_24_uhr_des_ablaufdatums() {
		// Ablaufdatum heute: 30 Tage ab einer Erstellung vor 29 Tagen
		String referenz = einladung(LocalDate.now().minusDays(29), 30, "Versendet");

		navigate(AssessmentView.class, referenz);

		assertThat(find(Checkbox.class).withId("assessment-bestaetigung").exists()).isTrue();
	}

	// ---------------------------------------------------------------- A2, A3, A6

	@Test
	@UseCase(id = "UC-002", scenario = "A2: Laufendes Assessment wiederaufnehmen", businessRules = { "BR-001", "BR-002" })
	void laufendes_assessment_geht_bei_der_ersten_frage_ohne_antwort_weiter() {
		String referenz = einladung(LocalDate.now(), 30, "Versendet");
		starten(referenz);
		antworten("Erste Antwort.");

		// erneuter Aufruf der Einladung, die Einladung ist bereits eingelöst
		navigate(AssessmentView.class, referenz);

		// A2: kein Starthinweis, die erste Frage ohne Antwort, die bisherige Antwort steht im Verlauf
		assertThat(find(Checkbox.class).withId("assessment-bestaetigung").exists()).isFalse();
		assertThat(text()).contains("Frage 2 von 2").contains(FRAGE_2).contains("Erste Antwort.");
	}

	@Test
	@UseCase(id = "UC-002", scenario = "A6: Teilnehmer unterbricht das Assessment")
	void unterbrochenes_assessment_behaelt_die_gegebenen_antworten() {
		String referenz = einladung(LocalDate.now(), 30, "Versendet");
		starten(referenz);
		antworten("Erste Antwort.");

		// der Teilnehmer verlässt das Assessment vor der letzten Frage
		navigate(AssessmentView.class, UUID.randomUUID().toString());

		assertThat(stufen(referenz)).containsExactly(3);
		assertThat(this.jdbc.sql("""
				select an.text from antwort an join assessment a on a.id = an.assessment_id
				join einladung e on e.id = a.einladung_id where e.referenz = ?
				""").param(referenz).query(String.class).list()).containsExactly("Erste Antwort.");
	}

	@Test
	@UseCase(id = "UC-002", scenario = "A3: Teilnehmer bestätigt den Starthinweis nicht", businessRules = { "BR-002" })
	void ohne_bestaetigung_startet_das_assessment_nicht() {
		String referenz = einladung(LocalDate.now(), 30, "Versendet");
		navigate(AssessmentView.class, referenz);

		test(button("assessment-nicht-starten")).click();

		assertThat(this.jdbc.sql("select count(*) from assessment a join einladung e on e.id = a.einladung_id where e.referenz = ?")
			.param(referenz)
			.query(Long.class)
			.single()).isZero();
		assertThat(status(referenz)).isEqualTo("Versendet");
		assertThat(find(TextArea.class).withId("assessment-antwort").exists()).isFalse();
	}

	// ---------------------------------------------------------------- A4

	@Test
	@UseCase(id = "UC-002", scenario = "A4: Antwort leer oder zu lang", businessRules = { "BR-004" })
	void leere_antwort_wird_gemeldet() {
		String referenz = einladung(LocalDate.now(), 30, "Versendet");
		starten(referenz);

		antworten("   ");

		TextArea feld = antwortFeld();
		assertThat(feld.isInvalid()).isTrue();
		assertThat(feld.getErrorMessage()).isEqualTo("Bitte geben Sie eine Antwort ein.");
		assertThat(stufen(referenz)).isEmpty();
		assertThat(KI.bewertungen()).isEmpty();
	}

	@Test
	@UseCase(id = "UC-002", scenario = "A4: Antwort leer oder zu lang", businessRules = { "BR-004" })
	void zu_lange_antwort_wird_mit_der_zahl_der_zeichen_gemeldet() {
		String referenz = einladung(LocalDate.now(), 30, "Versendet");
		starten(referenz);

		antworten("a".repeat(501));

		assertThat(antwortFeld().getErrorMessage()).isEqualTo("Die Antwort ist zu lang: 501 von höchstens 500 Zeichen.");
		assertThat(stufen(referenz)).isEmpty();

		// Schritt 5: genau 500 Zeichen sind zulässig
		antworten("a".repeat(500));
		assertThat(stufen(referenz)).containsExactly(3);
	}

	// ---------------------------------------------------------------- A5

	@Test
	@UseCase(id = "UC-002", scenario = "A5: Bewertung nicht möglich", businessRules = { "BR-006" })
	void nicht_erreichbares_kimodell_ergibt_stufe_0() {
		String referenz = einladung(LocalDate.now(), 30, "Versendet");
		starten(referenz);
		KI.failNextBewertung();

		antworten("Erste Antwort.");

		assertThat(stufen(referenz)).containsExactly(0);
		assertThat(text()).contains("Frage 2 von 2");
	}

	@Test
	@UseCase(id = "UC-002", scenario = "A5: Bewertung nicht möglich", businessRules = { "BR-006" })
	void ohne_zugeordnete_stufe_gilt_stufe_0() {
		String referenz = einladung(LocalDate.now(), 30, "Versendet");
		starten(referenz);
		KI.bewertetNaechsteMit(OptionalInt.empty());

		antworten("Erste Antwort.");

		assertThat(stufen(referenz)).containsExactly(0);
	}

	// ---------------------------------------------------------------- Ergänzungen nach dem Coverage-Audit

	@Test
	@UseCase(id = "UC-002", scenario = "A2: Laufendes Assessment wiederaufnehmen", businessRules = { "BR-001" })
	void gestartetes_assessment_laeuft_nach_dem_ablaufdatum_weiter() {
		String referenz = einladung(LocalDate.now(), 30, "Versendet");
		starten(referenz);
		antworten("Erste Antwort.");
		// die Einladung ist inzwischen abgelaufen: Erstellung vor 40 Tagen, 30 Tage gültig
		LocalDate erstellt = LocalDate.now().minusDays(40);
		this.jdbc.sql("update einladung set erstellt_am = ?, laeuft_ab_am = ? where referenz = ?")
			.params(erstellt.atTime(9, 0), erstellt.plusDays(29), referenz)
			.update();

		navigate(AssessmentView.class, referenz);

		assertThat(text()).contains("Frage 2 von 2").contains(FRAGE_2);
		antworten("Zweite Antwort.");
		assertThat(stufen(referenz)).containsExactly(3, 3);
	}

	@Test
	@UseCase(id = "UC-002", scenario = "Main Success Scenario", businessRules = { "BR-003" })
	void fragen_eines_entwurfs_derselben_rolle_erscheinen_nicht() {
		String referenz = einladung(LocalDate.now(), 30, "Versendet");
		starten(referenz);

		assertThat(text()).contains("Frage 1 von 2").contains(FRAGE_1).doesNotContain("UC002 Entwurfsfrage");
		antworten("Erste Antwort.");
		antworten("Zweite Antwort.");

		// beantwortet sind genau die zwei Fragen des freigegebenen Fragenkatalogs, keine des Entwurfs
		assertThat(this.jdbc.sql("""
				select an.frage_id from antwort an join assessment a on a.id = an.assessment_id
				join einladung e on e.id = a.einladung_id where e.referenz = ? order by an.frage_id
				""").param(referenz).query(Long.class).list()).containsExactly(9301L, 9302L);
	}

	@Test
	@UseCase(id = "UC-002", scenario = "A1: Einladung unbekannt, eingelöst oder abgelaufen", businessRules = { "BR-009" },
			requirements = { "NFR-004" })
	void assessment_ohne_eingabe_seit_7_tagen_laeuft_nicht_mehr() {
		String referenz = einladung(LocalDate.now(), 30, "Versendet");
		starten(referenz);
		antworten("Erste Antwort.");
		// die letzte Eingabe liegt 8 Tage zurück, der Löschlauf hat das Assessment noch nicht entfernt
		this.jdbc.sql("""
				update assessment set letzte_eingabe_am = now() - interval '8 days'
				where einladung_id = (select id from einladung where referenz = ?)
				""").param(referenz).update();

		navigate(AssessmentView.class, referenz);

		// die Einladung ist eingelöst und es gibt kein laufendes Assessment mehr: A1
		assertThat(nichtNutzbar()).contains(NICHT_NUTZBAR);
		assertThat(find(TextArea.class).withId("assessment-antwort").exists()).isFalse();
	}

	@Test
	@UseCase(id = "UC-002", scenario = "Main Success Scenario", requirements = { "NFR-010" })
	void antwortfeld_hat_einen_zugaenglichen_namen() {
		String referenz = einladung(LocalDate.now(), 30, "Versendet");
		starten(referenz);

		assertThat(antwortFeld().getAriaLabel()).contains("Ihre Antwort");
	}

	@Test
	@UseCase(id = "UC-002", scenario = "Main Success Scenario", requirements = { "NFR-010" })
	void oberflaeche_formatiert_nach_de_ch() {
		String referenz = einladung(LocalDate.now(), 30, "Versendet");
		navigate(AssessmentView.class, referenz);

		assertThat(com.vaadin.flow.component.UI.getCurrent().getLocale()).isEqualTo(java.util.Locale.forLanguageTag("de-CH"));
	}

	// ---------------------------------------------------------------- Hilfen

	/** Legt eine Einladung an; das Ablaufdatum folgt aus Erstellung und Gültigkeit (UC-001 BR-008). */
	private String einladung(LocalDate erstellt, int tage, String status) {
		String referenz = UUID.randomUUID().toString();
		this.jdbc.sql("""
				insert into einladung (referenz, mag_id, rolle_id, erstellt_von_id, status, erstellt_am, gueltigkeit_tage, laeuft_ab_am)
				values (?, 9301, 9301, 9301, ?, ?, ?, ?)
				""")
			.params(referenz, status, erstellt.atTime(9, 0), tage, erstellt.plusDays(tage - 1L))
			.update();
		return referenz;
	}

	private void starten(String referenz) {
		navigate(AssessmentView.class, referenz);
		test(find(Checkbox.class).withId("assessment-bestaetigung").single()).click();
		test(button("assessment-starten")).click();
	}

	private void antworten(String text) {
		test(antwortFeld()).setValue(text);
		test(button("assessment-senden")).click();
	}

	private TextArea antwortFeld() {
		return find(TextArea.class).withId("assessment-antwort").single();
	}

	private Button button(String id) {
		return find(Button.class).withId(id).single();
	}

	private String text() {
		return find(AssessmentView.class).single().getElement().getTextRecursively();
	}

	private String nichtNutzbar() {
		return find(Div.class).withId("assessment-nicht-nutzbar").single().getElement().getTextRecursively();
	}

	private String status(String referenz) {
		return this.jdbc.sql("select status from einladung where referenz = ?").param(referenz).query(String.class).single();
	}

	private List<Integer> stufen(String referenz) {
		return this.jdbc.sql("""
				select b.stufe from bewertung b join antwort an on an.id = b.antwort_id
				join assessment a on a.id = an.assessment_id join einladung e on e.id = a.einladung_id
				join frage f on f.id = an.frage_id where e.referenz = ? order by f.position
				""").param(referenz).query(Integer.class).list();
	}


}
