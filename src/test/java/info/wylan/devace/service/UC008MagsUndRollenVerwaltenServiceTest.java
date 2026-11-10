package info.wylan.devace.service;

import info.wylan.devace.TestcontainersConfiguration;
import info.wylan.devace.usecase.UseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * UC-008, Service-Ebene: BR-004 (Teamleitung aus den vorhandenen Teamleitungen). Die übrigen Regeln prüfen die
 * Tests der Ansicht, die diese Schicht mit durchlaufen.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@WithMockUser(roles = "ADMINISTRATOR")
class UC008MagsUndRollenVerwaltenServiceTest {

	private static final long VORHANDENE_TEAMLEITUNG = 9001L;

	private static final long BENUTZER_OHNE_TEAMLEITUNG = 9003L;

	private static final long UNBEKANNT = 999_999L;

	@Autowired
	private MagService service;

	@Autowired
	private JdbcClient jdbc;

	@AfterEach
	void aufraeumen() {
		this.jdbc.sql("delete from mag where scrum_team like 'UC008S%'").update();
	}

	@Test
	@UseCase(id = "UC-008", businessRules = { "BR-004" })
	void vorhandene_teamleitung_wird_zugeordnet() {
		this.service.anlegen(new MagEingabe("UC008S Eins", "UC008S Nord", VORHANDENE_TEAMLEITUNG));

		assertThat(teamleitungVon("UC008S Eins")).isEqualTo(VORHANDENE_TEAMLEITUNG);
	}

	@Test
	@UseCase(id = "UC-008", businessRules = { "BR-004" })
	void mag_ohne_teamleitung_ist_zulaessig() {
		this.service.anlegen(new MagEingabe("UC008S Zwei", "UC008S Nord", null));

		assertThat(teamleitungVon("UC008S Zwei")).isZero();
	}

	@Test
	@UseCase(id = "UC-008", businessRules = { "BR-004" })
	void nicht_vorhandene_teamleitung_wird_beim_anlegen_abgewiesen() {
		assertThatThrownBy(() -> this.service.anlegen(new MagEingabe("UC008S Drei", "UC008S Nord", UNBEKANNT)))
			.isInstanceOf(TeamleitungNichtVorhandenException.class);

		assertThat(anzahl("UC008S Drei")).isZero();
	}

	@Test
	@UseCase(id = "UC-008", businessRules = { "BR-004" })
	void benutzer_ohne_funktion_teamleitung_wird_beim_anlegen_abgewiesen() {
		assertThatThrownBy(
				() -> this.service.anlegen(new MagEingabe("UC008S Vier", "UC008S Nord", BENUTZER_OHNE_TEAMLEITUNG)))
			.isInstanceOf(TeamleitungNichtVorhandenException.class);

		assertThat(anzahl("UC008S Vier")).isZero();
	}

	@Test
	@UseCase(id = "UC-008", businessRules = { "BR-004" })
	void nicht_vorhandene_teamleitung_wird_beim_aendern_abgewiesen_und_die_mag_bleibt() {
		this.service.anlegen(new MagEingabe("UC008S Fuenf", "UC008S Nord", VORHANDENE_TEAMLEITUNG));
		long id = this.jdbc.sql("select id from mag where scrum_team = 'UC008S Fuenf'").query(Long.class).single();

		assertThatThrownBy(() -> this.service.aendern(id, new MagEingabe("UC008S Fuenf", "UC008S Nord", UNBEKANNT)))
			.isInstanceOf(TeamleitungNichtVorhandenException.class);

		assertThat(teamleitungVon("UC008S Fuenf")).isEqualTo(VORHANDENE_TEAMLEITUNG);
	}

	/** Liefert 0, wenn die MAG keine Teamleitung hat. */
	private long teamleitungVon(String scrumTeam) {
		return this.jdbc.sql("select coalesce(teamleitung_id, 0) from mag where scrum_team = ?")
			.param(scrumTeam)
			.query(Long.class)
			.single();
	}

	private int anzahl(String scrumTeam) {
		return this.jdbc.sql("select count(*) from mag where scrum_team = ?")
			.param(scrumTeam)
			.query(Integer.class)
			.single();
	}

}
