package info.wylan.devace.ui;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextArea;
import info.wylan.devace.TestcontainersConfiguration;
import info.wylan.devace.service.KiModellClient;
import info.wylan.devace.service.KiModellClient.Abschnitt;
import info.wylan.devace.service.KiModellClient.BewerteteFrage;
import info.wylan.devace.service.KiModellClient.Textreport;
import info.wylan.devace.service.KiModellClient.HandlungsfeldAuswertung;
import info.wylan.devace.service.KiModellClient.TextreportAnfrage;
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
 * UC-003 Assessment abschließen und Textreport herunterladen, getestet über die Ansicht mit echter Datenbank. Das
 * KI-Modell ist durch {@link KiModellClientFake} ersetzt; die Uhr lässt sich für den Verfall des Textreports
 * vorstellen.
 *
 * <p>
 * Testdaten (R__uc003_abschluss.sql): MAG 9401, Rolle 9401 mit freigegebenem Fragenkatalog 9401: vier Fragen im
 * Handlungsfeld 1, eine im Handlungsfeld 2, keine in den Handlungsfeldern 3 bis 6. Jeder Test legt seine Einladung an
 * und beantwortet alle Fragen über die Ansicht (Vorbedingung); die Stufen 4, 3, 3, 3 ergeben im Handlungsfeld 1 den
 * Score 13 / 4 = 3.25, kaufmännisch gerundet 3.3, die Stufe 2 im Handlungsfeld 2 den Score 2.0.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@WithAnonymousUser
class UC003AssessmentAbschliessenUndTextreportHerunterladenTest extends SpringBrowserlessTest {

	private static final KiModellClientFake KI = new KiModellClientFake();

	private static final VerstellbareUhr UHR = new VerstellbareUhr();

	private static final String FELD_1 = "Domänenwissen & Business Alignment";

	private static final String FELD_2 = "Architektur & Engineering";

	/** Erste Antwort mit mehr als acht Wörtern, für BR-003. */
	private static final String ANTWORT_LANG = "Wir halten Kontext, Alternativen und Folgen jeder Entscheidung fest, meist in einem ADR.";

	private static final String NICHT_NUTZBAR = "Diese Einladung wurde bereits eingelöst oder ist abgelaufen.";

	@TestBean
	KiModellClient kiModellClient;

	@TestBean
	Clock clock;

	@Autowired
	private JdbcClient jdbc;

	static KiModellClient kiModellClient() {
		return KI;
	}

	static Clock clock() {
		return UHR;
	}

	@BeforeEach
	void zuruecksetzen() {
		KI.reset();
		UHR.zuruecksetzen();
	}

	@AfterEach
	void aufraeumen() {
		this.jdbc.sql("delete from score where mag_id = 9401").update();
		this.jdbc.sql("delete from assessment where einladung_id in (select id from einladung where erstellt_von_id = 9401)")
			.update();
		this.jdbc.sql("delete from einladung where erstellt_von_id = 9401").update();
	}

	// ---------------------------------------------------------------- Hauptablauf

	@Test
	@UseCase(id = "UC-003", scenario = "Main Success Scenario", businessRules = { "BR-001", "BR-002", "BR-005" },
			requirements = { "NFR-002", "C-003" })
	void teilnehmer_schliesst_ab_und_sieht_seine_auswertung() {
		String referenz = alleFragenBeantwortet();

		// Schritt 1
		test(button("assessment-abschliessen")).click();

		// Schritt 5: Auswertung mit Radar-Chart, Scores je Handlungsfeld und Textreport
		assertThat(text()).contains("Ihre Auswertung").contains("Rolle: UC003 Entwicklung");
		assertThat(find(Div.class).withId("assessment-abgeschlossen").single().getElement().getTextRecursively())
			.contains("Assessment abgeschlossen")
			.contains("Ihre Auswertung ist fertig. Radar-Chart, Scores und Textreport sehen nur Sie, und nur jetzt.");
		assertThat(text()).contains(
				"Der Textreport steht einmal zum Download bereit. Ihre Antworten sind bereits gelöscht, Ihre Scores bleiben ohne Personenbezug erhalten.");
		assertThat(find(Html.class).single().getElement().getOuterHTML()).contains("Radar-Chart der Scores je Handlungsfeld");
		// BR-001: kaufmännisch auf eine Nachkommastelle gerundet und immer mit einer Nachkommastelle ausgegeben;
		// ein Handlungsfeld ohne Frage hat keinen Score
		String scores = find(Div.class).withId("auswertung-scores").single().getElement().getTextRecursively();
		assertThat(scores).contains(FELD_1 + "3.3").contains(FELD_2 + "2.0").contains("Qualität & Testing–");
		assertThat(text()).contains("Überblick des KI-Modells.")
			.contains("Würdigung " + FELD_1 + ".")
			.contains("Nächster Schritt: Nächster Schritt " + FELD_2 + ".");

		// Schritt 4, BR-002: je Handlungsfeld mit Fragen ein Score mit MAG, Rolle, Handlungsfeld, Fragenkatalog,
		// Wert und Datum
		assertThat(this.jdbc.sql("""
				select h.nummer, s.wert, s.mag_id, s.rolle_id, s.fragenkatalog_id, s.erfasst_am from score s
				join handlungsfeld h on h.id = s.handlungsfeld_id where s.mag_id = 9401 order by h.nummer
				""").query((rs, i) -> List.of(rs.getInt(1), rs.getBigDecimal(2), rs.getLong(3), rs.getLong(4), rs.getLong(5),
				rs.getDate(6).toLocalDate())).list())
			.containsExactly(List.of(1, new BigDecimal("3.3"), 9401L, 9401L, 9401L, LocalDate.now()),
					List.of(2, new BigDecimal("2.0"), 9401L, 9401L, 9401L, LocalDate.now()));
		// BR-002, NFR-002: der Score hat keine Spalte für Einladung, Assessment, Antwort, E-Mail-Adresse oder Person
		assertThat(this.jdbc.sql("select column_name from information_schema.columns where table_name = 'score' order by column_name")
			.query(String.class)
			.list()).containsExactlyInAnyOrder("id", "mag_id", "rolle_id", "handlungsfeld_id", "fragenkatalog_id", "wert",
					"erfasst_am");
		// Schritt 4, BR-007: das Assessment ist abgeschlossen, die Antworten sind schon vor dem Download gelöscht
		assertThat(assessmentStatus(referenz)).isEqualTo("Abgeschlossen");
		assertThat(antworten(referenz)).isZero();
		assertThat(bewertungen(referenz)).isZero();
	}

	@Test
	@UseCase(id = "UC-003", scenario = "Main Success Scenario", businessRules = { "BR-001" })
	void radar_chart_zeigt_die_skala_nur_in_ganzzahlen_und_einen_punkt_je_score() {
		alleFragenBeantwortet();

		test(button("assessment-abschliessen")).click();

		String svg = find(Html.class).single().getElement().getOuterHTML();
		// BR-001: fünf Ringe für die Ganzzahlen 1 bis 5, eine Achse je Handlungsfeld, ein Punkt je Score
		assertThat(svg.split("devace-radar-ring", -1)).hasSize(6);
		assertThat(svg.split("devace-radar-achse", -1)).hasSize(7);
		assertThat(svg.split("devace-radar-punkt", -1)).hasSize(3);
		assertThat(svg).contains("Domänenwissen &amp; Business Alignment").contains("Future Skills");
	}

	@Test
	@UseCase(id = "UC-003", scenario = "Main Success Scenario", businessRules = { "BR-004" }, requirements = { "NFR-001" })
	void das_kimodell_erhaelt_nur_rolle_handlungsfelder_fragen_antworten_stufen_und_scores() {
		String referenz = alleFragenBeantwortet();

		test(button("assessment-abschliessen")).click();

		assertThat(KI.textreportAnfragen()).hasSize(1);
		TextreportAnfrage anfrage = KI.textreportAnfragen().get(0);
		assertThat(anfrage.rolle()).isEqualTo("UC003 Entwicklung");
		assertThat(anfrage.handlungsfelder()).extracting(HandlungsfeldAuswertung::name, HandlungsfeldAuswertung::score)
			.containsExactly(org.assertj.core.groups.Tuple.tuple(FELD_1, new BigDecimal("3.3")),
					org.assertj.core.groups.Tuple.tuple(FELD_2, new BigDecimal("2.0")));
		assertThat(anfrage.handlungsfelder().get(1).fragen()).containsExactly(
				new BewerteteFrage("UC003 Wie begründen Sie eine Architekturentscheidung?", "Antwort 5.", 2));
		// keine Einladung, keine E-Mail-Adresse und keine andere Angabe zum Teilnehmer
		assertThat(anfrage.toString()).doesNotContain(referenz).doesNotContain("@").doesNotContain("UC003 Beta");
	}

	@Test
	@UseCase(id = "UC-003", scenario = "Main Success Scenario", businessRules = { "BR-003" }, requirements = { "C-002" })
	void auswertung_sagt_dass_scores_und_textreport_kein_bestandteil_der_leistungsbeurteilung_sind() {
		alleFragenBeantwortet();

		test(button("assessment-abschliessen")).click();

		assertThat(text()).contains("Scores und Textreport sind kein Bestandteil der Leistungsbeurteilung.");
	}

	@Test
	@UseCase(id = "UC-003", scenario = "Main Success Scenario", businessRules = { "BR-006", "BR-007" },
			requirements = { "NFR-003" })
	void teilnehmer_laedt_den_textreport_als_pdf_herunter_und_seine_antworten_sind_geloescht() throws Exception {
		String referenz = alleFragenBeantwortet();
		test(button("assessment-abschliessen")).click();

		// Schritte 6 und 7
		ByteArrayOutputStream pdf = new ByteArrayOutputStream();
		test(download()).download(pdf);

		// OQ-070: der Textreport ist ein PDF
		assertThat(new String(pdf.toByteArray(), 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
		// BR-007: nach dem Download sind alle Antworten des Assessments gelöscht, die Scores bleiben
		assertThat(antworten(referenz)).isZero();
		assertThat(bewertungen(referenz)).isZero();
		assertThat(this.jdbc.sql("select count(*) from score where mag_id = 9401").query(Long.class).single()).isEqualTo(2L);
	}

	// ---------------------------------------------------------------- A1

	@Test
	@UseCase(id = "UC-003", scenario = "A1: KI-Modell liefert keinen Textreport")
	void nicht_erreichbares_kimodell_speichert_nichts_und_der_abschluss_laesst_sich_erneut_anfordern() {
		String referenz = alleFragenBeantwortet();
		KI.failNextTextreport();

		test(button("assessment-abschliessen")).click();

		// A1 Schritt 1: Hinweis im Wortlaut des Mockups (OQ-069)
		assertThat(find(Div.class).withId("textreport-nicht-erstellt").single().getElement().getTextRecursively())
			.contains("Textreport nicht erstellt")
			.contains("Das KI-Modell hat keinen Textreport geliefert. Es wurde nichts gespeichert, Ihre Antworten bleiben erhalten. "
					+ "Sie können den Abschluss erneut anfordern.");
		// A1 Schritt 2, Fehlerfall: keine Scores, das Assessment läuft, die Antworten mit ihren Stufen bleiben
		assertThat(this.jdbc.sql("select count(*) from score where mag_id = 9401").query(Long.class).single()).isZero();
		assertThat(assessmentStatus(referenz)).isEqualTo("Laufend");
		assertThat(antworten(referenz)).isEqualTo(5L);
		assertThat(bewertungen(referenz)).isEqualTo(5L);
		// Fehlerfall: jede gespeicherte Antwort bleibt mit Text und Stufe unverändert
		assertThat(antwortenMitStufen(referenz)).containsExactly("Antwort 1.|4", "Antwort 2.|3", "Antwort 3.|3",
				"Antwort 4.|3", "Antwort 5.|2");

		// A1 Schritt 3: weiter bei Schritt 1
		test(button("assessment-abschluss-erneut")).click();

		assertThat(text()).contains("Ihre Auswertung");
		assertThat(this.jdbc.sql("select count(*) from score where mag_id = 9401").query(Long.class).single()).isEqualTo(2L);
		assertThat(assessmentStatus(referenz)).isEqualTo("Abgeschlossen");
	}

	@Test
	@UseCase(id = "UC-003", scenario = "A1: KI-Modell liefert keinen Textreport")
	void kimodell_ohne_verwertbaren_textreport_speichert_nichts() {
		String referenz = alleFragenBeantwortet();
		KI.liefertNaechstenTextreportNicht();

		test(button("assessment-abschliessen")).click();

		assertThat(find(Div.class).withId("textreport-nicht-erstellt").exists()).isTrue();
		assertThat(this.jdbc.sql("select count(*) from score where mag_id = 9401").query(Long.class).single()).isZero();
		assertThat(assessmentStatus(referenz)).isEqualTo("Laufend");
		assertThat(antworten(referenz)).isEqualTo(5L);
	}

	@Test
	@UseCase(id = "UC-003", scenario = "A1: KI-Modell liefert keinen Textreport", businessRules = { "BR-003" })
	void textreport_mit_acht_woertern_einer_antwort_gilt_als_nicht_erstellt() {
		String referenz = alleFragenBeantwortet(ANTWORT_LANG);
		// acht aufeinanderfolgende Wörter der ersten Antwort, mit anderer Schreibweise und Satzzeichen
		KI.liefertNaechstenTextreport(new Textreport("Überblick.", List.of(new Abschnitt(FELD_1,
				"Sie schreiben: „Wir halten Kontext, Alternativen und Folgen jeder Entscheidung fest“.", "Weiter so."))));

		test(button("assessment-abschliessen")).click();

		assertThat(find(Div.class).withId("textreport-nicht-erstellt").exists()).isTrue();
		assertThat(this.jdbc.sql("select count(*) from score where mag_id = 9401").query(Long.class).single()).isZero();
		assertThat(assessmentStatus(referenz)).isEqualTo("Laufend");
		assertThat(antworten(referenz)).isEqualTo(5L);
	}

	@Test
	@UseCase(id = "UC-003", scenario = "Main Success Scenario", businessRules = { "BR-003" })
	void textreport_mit_sieben_woertern_einer_antwort_wird_angenommen() {
		String referenz = alleFragenBeantwortet(ANTWORT_LANG);
		// nur sieben aufeinanderfolgende Wörter der ersten Antwort
		KI.liefertNaechstenTextreport(new Textreport("Überblick.", List.of(new Abschnitt(FELD_1,
				"Sie halten Kontext, Alternativen und Folgen jeder Entscheidung bewusst fest.", "Weiter so."))));

		test(button("assessment-abschliessen")).click();

		assertThat(text()).contains("Ihre Auswertung");
		assertThat(assessmentStatus(referenz)).isEqualTo("Abgeschlossen");
	}

	// ---------------------------------------------------------------- A2

	@Test
	@UseCase(id = "UC-003", scenario = "A2: Teilnehmer lädt den Textreport nicht herunter", businessRules = { "BR-005",
			"BR-007" }, requirements = { "NFR-003" })
	void ohne_download_wird_der_textreport_nicht_erneut_angeboten_und_die_antworten_sind_geloescht() {
		String referenz = alleFragenBeantwortet();
		test(button("assessment-abschliessen")).click();

		// der Teilnehmer verlässt die Auswertung und ruft seine Einladung erneut auf
		navigate(AssessmentView.class, referenz);

		// A2 Schritt 1, BR-005: keine Auswertung und kein Textreport mehr
		assertThat(find(Div.class).withId("assessment-nicht-nutzbar").single().getElement().getTextRecursively())
			.contains(NICHT_NUTZBAR);
		assertThat(find(Anchor.class).withId("textreport-herunterladen").exists()).isFalse();
		// A2 Schritt 2: die Antworten sind spätestens 1 Stunde nach Erstellung des Textreports gelöscht
		assertThat(antworten(referenz)).isZero();
		// Erfolgsfall A2: Scores gespeichert, Assessment abgeschlossen
		assertThat(this.jdbc.sql("select count(*) from score where mag_id = 9401").query(Long.class).single()).isEqualTo(2L);
		assertThat(assessmentStatus(referenz)).isEqualTo("Abgeschlossen");
	}

	// ---------------------------------------------------------------- A3

	@Test
	@UseCase(id = "UC-003", scenario = "A3: Textreport nicht mehr verfügbar", businessRules = { "BR-006" })
	void zweiter_download_ist_nicht_mehr_verfuegbar() {
		alleFragenBeantwortet();
		test(button("assessment-abschliessen")).click();
		test(download()).download(new ByteArrayOutputStream());

		ByteArrayOutputStream zweiter = new ByteArrayOutputStream();
		test(download()).download(zweiter);
		roundTrip();

		assertThat(zweiter.size()).isZero();
		assertThat(nichtVerfuegbar()).contains("Textreport nicht mehr verfügbar")
			.contains("Der Textreport wurde bereits heruntergeladen oder ist verfallen. Ihre Scores bleiben ohne Personenbezug gespeichert.");
	}

	@Test
	@UseCase(id = "UC-003", scenario = "A3: Textreport nicht mehr verfügbar", businessRules = { "BR-006" })
	void textreport_ist_innerhalb_der_stunde_nach_seiner_erstellung_verfuegbar() {
		alleFragenBeantwortet();
		test(button("assessment-abschliessen")).click();

		// 59 Minuten nach der Erstellung ist er noch verfügbar
		UHR.vorstellen(Duration.ofMinutes(59));
		ByteArrayOutputStream pdf = new ByteArrayOutputStream();
		test(download()).download(pdf);
		assertThat(pdf.size()).isPositive();
	}

	@Test
	@UseCase(id = "UC-003", scenario = "A3: Textreport nicht mehr verfügbar", businessRules = { "BR-006" })
	void verfallener_textreport_ist_nicht_mehr_verfuegbar() {
		alleFragenBeantwortet();
		test(button("assessment-abschliessen")).click();

		UHR.vorstellen(Duration.ofHours(1).plusSeconds(1));
		ByteArrayOutputStream pdf = new ByteArrayOutputStream();
		test(download()).download(pdf);
		roundTrip();

		assertThat(pdf.size()).isZero();
		assertThat(nichtVerfuegbar()).contains("Textreport nicht mehr verfügbar");
	}

	// ---------------------------------------------------------------- NFR-010

	@Test
	@UseCase(id = "UC-003", scenario = "Main Success Scenario", requirements = { "NFR-010" })
	void radar_chart_hat_einen_zugaenglichen_namen() {
		alleFragenBeantwortet();

		test(button("assessment-abschliessen")).click();

		String svg = find(Html.class).single().getElement().getOuterHTML();
		assertThat(svg).contains("role=\"img\"").contains("aria-label=\"Radar-Chart der Scores je Handlungsfeld\"");
	}

	// ---------------------------------------------------------------- Hilfen

	/**
	 * Vorbedingung: Einladung angelegt, Assessment gestartet und alle fünf Fragen beantwortet, mit den Stufen 4, 3, 3,
	 * 3 im Handlungsfeld 1 und 2 im Handlungsfeld 2.
	 */
	private String alleFragenBeantwortet() {
		return alleFragenBeantwortet(null);
	}

	/** Wie {@link #alleFragenBeantwortet()}, die erste Antwort mit dem angegebenen Text. */
	private String alleFragenBeantwortet(String ersteAntwort) {
		String referenz = UUID.randomUUID().toString();
		LocalDate heute = LocalDate.now();
		this.jdbc.sql("""
				insert into einladung (referenz, mag_id, rolle_id, erstellt_von_id, status, erstellt_am, gueltigkeit_tage, laeuft_ab_am)
				values (?, 9401, 9401, 9401, 'Versendet', ?, 30, ?)
				""").params(referenz, heute.atTime(9, 0), heute.plusDays(29)).update();
		navigate(AssessmentView.class, referenz);
		test(find(Checkbox.class).withId("assessment-bestaetigung").single()).click();
		test(button("assessment-starten")).click();
		int[] stufen = { 4, 3, 3, 3, 2 };
		for (int i = 0; i < stufen.length; i++) {
			KI.bewertetNaechsteMit(OptionalInt.of(stufen[i]));
			String text = (i == 0 && ersteAntwort != null) ? ersteAntwort : "Antwort " + (i + 1) + ".";
			test(find(TextArea.class).withId("assessment-antwort").single()).setValue(text);
			test(button("assessment-senden")).click();
		}
		assertThat(find(Div.class).withId("assessment-fertig").exists()).isTrue();
		return referenz;
	}

	private Anchor download() {
		return find(Anchor.class).withId("textreport-herunterladen").single();
	}

	private Button button(String id) {
		return find(Button.class).withId(id).single();
	}

	private String text() {
		return find(AssessmentView.class).single().getElement().getTextRecursively();
	}

	private String nichtVerfuegbar() {
		return find(Div.class).withId("textreport-nicht-verfuegbar").single().getElement().getTextRecursively();
	}

	private String assessmentStatus(String referenz) {
		return this.jdbc.sql("select a.status from assessment a join einladung e on e.id = a.einladung_id where e.referenz = ?")
			.param(referenz)
			.query(String.class)
			.single();
	}

	private long antworten(String referenz) {
		return this.jdbc.sql("""
				select count(*) from antwort an join assessment a on a.id = an.assessment_id
				join einladung e on e.id = a.einladung_id where e.referenz = ?
				""").param(referenz).query(Long.class).single();
	}

	private List<String> antwortenMitStufen(String referenz) {
		return this.jdbc.sql("""
				select an.text || '|' || b.stufe from antwort an join bewertung b on b.antwort_id = an.id
				join assessment a on a.id = an.assessment_id join einladung e on e.id = a.einladung_id
				join frage f on f.id = an.frage_id where e.referenz = ? order by f.position
				""").param(referenz).query(String.class).list();
	}

	private long bewertungen(String referenz) {
		return this.jdbc.sql("""
				select count(*) from bewertung b join antwort an on an.id = b.antwort_id
				join assessment a on a.id = an.assessment_id join einladung e on e.id = a.einladung_id where e.referenz = ?
				""").param(referenz).query(Long.class).single();
	}

	/** Uhr der Anwendung, die ein Test vorstellen kann; ohne Verstellung die Systemzeit. */
	static final class VerstellbareUhr extends Clock {

		private volatile Duration versatz = Duration.ZERO;

		void vorstellen(Duration dauer) {
			this.versatz = this.versatz.plus(dauer);
		}

		void zuruecksetzen() {
			this.versatz = Duration.ZERO;
		}

		@Override
		public ZoneId getZone() {
			return ZoneId.systemDefault();
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return Instant.now().plus(this.versatz);
		}

	}

}
