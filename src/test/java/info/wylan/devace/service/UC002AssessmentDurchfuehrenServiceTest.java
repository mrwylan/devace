package info.wylan.devace.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import info.wylan.devace.TestcontainersConfiguration;
import info.wylan.devace.persistence.AssessmentRepository;
import info.wylan.devace.usecase.UseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.convention.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * UC-002, Service-Ebene: BR-002 ohne Bestätigung (die Oberfläche lässt den Start ohne sie nicht zu), BR-005 mit einer
 * zweiten Antwort auf dieselbe Frage (nur bei Nebenläufigkeit erreichbar) und BR-009, die Löschung nach 7 Tagen ohne
 * Eingabe, die ein Zeitplan auslöst. Die übrigen Regeln prüfen die Tests der Ansicht.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UC002AssessmentDurchfuehrenServiceTest {

	private static final KiModellClientFake KI = new KiModellClientFake();

	@TestBean
	KiModellClient kiModellClient;

	@Autowired
	private AssessmentService service;

	@Autowired
	private AssessmentRepository assessmentRepository;

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

	@Test
	@UseCase(id = "UC-002", scenario = "A3: Teilnehmer bestätigt den Starthinweis nicht", businessRules = { "BR-002" })
	void ohne_bestaetigten_starthinweis_beginnt_kein_assessment() {
		String referenz = einladung();

		assertThatThrownBy(() -> this.service.starten(referenz, false))
			.isInstanceOf(StarthinweisNichtBestaetigtException.class);
		assertThat(anzahlAssessments(referenz)).isZero();
	}

	@Test
	@UseCase(id = "UC-002", businessRules = { "BR-009" }, requirements = { "NFR-004" })
	void laufendes_assessment_wird_7_tage_nach_der_letzten_eingabe_mit_den_antworten_geloescht() {
		String referenz = einladung();
		this.service.starten(referenz, true);
		this.service.antworten(referenz, "Erste Antwort.");
		letzteEingabeVorTagen(referenz, 8);

		this.service.loescheLiegengebliebene();

		assertThat(anzahlAssessments(referenz)).isZero();
		assertThat(this.jdbc.sql("select count(*) from antwort where text = 'Erste Antwort.' and assessment_id not in (select id from assessment)")
			.query(Long.class)
			.single()).isZero();
		// die Einladung bleibt eingelöst, ein erneuter Aufruf ist A1
		assertThat(this.jdbc.sql("select status from einladung where referenz = ?").param(referenz).query(String.class).single())
			.isEqualTo("Eingelöst");
		assertThat(this.service.einstieg(referenz)).isInstanceOf(AssessmentService.Einstieg.NichtNutzbar.class);
	}

	@Test
	@UseCase(id = "UC-002", businessRules = { "BR-009" }, requirements = { "NFR-004" })
	void laufendes_assessment_bleibt_vor_ablauf_von_7_tagen_erhalten() {
		String referenz = einladung();
		this.service.starten(referenz, true);
		this.service.antworten(referenz, "Erste Antwort.");
		letzteEingabeVorTagen(referenz, 6);

		this.service.loescheLiegengebliebene();

		assertThat(anzahlAssessments(referenz)).isEqualTo(1L);
		assertThat(this.service.einstieg(referenz)).isInstanceOf(AssessmentService.Einstieg.Fortsetzen.class);
	}

	@Test
	@UseCase(id = "UC-002", businessRules = { "BR-005" })
	void eine_zweite_antwort_auf_dieselbe_frage_wird_abgewiesen_die_erste_bleibt_unveraendert() {
		String referenz = einladung();
		this.service.starten(referenz, true);
		this.service.antworten(referenz, "Erste Antwort.");
		long assessmentId = this.jdbc.sql("select a.id from assessment a join einladung e on e.id = a.einladung_id where e.referenz = ?")
			.param(referenz)
			.query(Long.class)
			.single();

		// eine zweite, gleichzeitige Antwort auf Frage 1 (nur durch Nebenläufigkeit erreichbar)
		assertThatThrownBy(() -> this.assessmentRepository.insertAntwort(assessmentId, 9301, "Zweite Antwort.", 5,
				LocalDateTime.now()))
			.isInstanceOf(DuplicateKeyException.class);

		// Fehlerfall: die gegebene Antwort bleibt mit ihrer Stufe unverändert
		assertThat(this.jdbc.sql("""
				select an.text || ':' || b.stufe from antwort an join bewertung b on b.antwort_id = an.id
				where an.assessment_id = ? and an.frage_id = 9301
				""").param(assessmentId).query(String.class).list()).containsExactly("Erste Antwort.:3");
	}

	private String einladung() {
		String referenz = UUID.randomUUID().toString();
		LocalDate heute = LocalDate.now();
		this.jdbc.sql("""
				insert into einladung (referenz, mag_id, rolle_id, erstellt_von_id, status, erstellt_am, gueltigkeit_tage, laeuft_ab_am)
				values (?, 9301, 9301, 9301, 'Versendet', ?, 30, ?)
				""").params(referenz, heute.atTime(9, 0), heute.plusDays(29)).update();
		return referenz;
	}

	private void letzteEingabeVorTagen(String referenz, int tage) {
		this.jdbc.sql("""
				update assessment set letzte_eingabe_am = now() - make_interval(days => ?)
				where einladung_id = (select id from einladung where referenz = ?)
				""").params(tage, referenz).update();
	}

	private long anzahlAssessments(String referenz) {
		return this.jdbc.sql("select count(*) from assessment a join einladung e on e.id = a.einladung_id where e.referenz = ?")
			.param(referenz)
			.query(Long.class)
			.single();
	}

}
