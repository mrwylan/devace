package info.wylan.devace.service;

import java.time.LocalDate;

import info.wylan.devace.TestcontainersConfiguration;
import info.wylan.devace.usecase.UseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.convention.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UC-003, Service-Ebene: BR-002, die Aufbewahrung der Scores über fünf Jahre, die ein Zeitplan auslöst und keine
 * Ansicht erreicht. Die übrigen Regeln prüfen die Tests der Ansicht.
 *
 * <p>
 * Testdaten (R__uc003_abschluss.sql): MAG 9401, Rolle 9401, Fragenkatalog 9401. Die Scores legt der Test selbst an.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UC003AssessmentAbschliessenUndTextreportHerunterladenServiceTest {

	@TestBean
	KiModellClient kiModellClient;

	@Autowired
	private AssessmentService service;

	@Autowired
	private JdbcClient jdbc;

	static KiModellClient kiModellClient() {
		return new KiModellClientFake();
	}

	@AfterEach
	void aufraeumen() {
		this.jdbc.sql("delete from score where mag_id = 9401").update();
	}

	@Test
	@UseCase(id = "UC-003", businessRules = { "BR-002" }, requirements = { "C-003" })
	void scores_werden_fuenf_jahre_aufbewahrt_und_danach_geloescht() {
		LocalDate heute = LocalDate.now();
		score(heute.minusYears(5).minusDays(1), "1.0");
		score(heute.minusYears(5), "2.0");
		score(heute.minusYears(1), "3.0");

		this.service.loescheAlteScores();

		// Scores aus den letzten fünf Jahren bleiben, ältere sind gelöscht
		assertThat(this.jdbc.sql("select wert from score where mag_id = 9401 order by wert")
			.query(String.class)
			.list()).containsExactly("2.0", "3.0");
	}

	private void score(LocalDate erfasst, String wert) {
		this.jdbc.sql("""
				insert into score (mag_id, rolle_id, handlungsfeld_id, fragenkatalog_id, wert, erfasst_am)
				values (9401, 9401, (select id from handlungsfeld where nummer = 1), 9401, cast(? as numeric), ?)
				""").params(wert, erfasst).update();
	}

}
