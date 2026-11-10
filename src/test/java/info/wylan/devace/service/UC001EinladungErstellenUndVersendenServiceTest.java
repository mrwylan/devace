package info.wylan.devace.service;

import java.util.List;

import info.wylan.devace.TestcontainersConfiguration;
import info.wylan.devace.service.EinladungService.EinladungsAngaben;
import info.wylan.devace.usecase.UseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.convention.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * UC-001, Service-Ebene: BR-002, BR-003 und BR-009, die die Oberfläche nicht erreicht (sie bietet nur eigene MAGs und
 * freigegebene Rollen an und sperrt die Route). Die übrigen Regeln prüfen die Tests der Ansicht, die diese Schicht
 * mit durchlaufen. Das E-Mail-System ist durch {@link EmailSystemClientFake} ersetzt.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@WithMockUser(username = "uc001-teamleitung", roles = "TEAMLEITUNG")
class UC001EinladungErstellenUndVersendenServiceTest {

	private static final long EIGENE_MAG = 9201L;

	private static final long FREMDE_MAG = 9202L;

	private static final long ROLLE_MIT_FREIGABE = 9201L;

	private static final long ROLLE_OHNE_FREIGABE = 9202L;

	private static final EmailSystemClientFake MAIL = new EmailSystemClientFake();

	@TestBean
	EmailSystemClient emailSystemClient;

	@Autowired
	private EinladungService service;

	@Autowired
	private JdbcClient jdbc;

	static EmailSystemClient emailSystemClient() {
		return MAIL;
	}

	@BeforeEach
	void zuruecksetzen() {
		MAIL.reset();
	}

	@Test
	@UseCase(id = "UC-001", scenario = "Steps 5-6", businessRules = { "BR-002" })
	void mag_einer_anderen_teamleitung_wird_abgewiesen() {
		EinladungsAngaben angaben = angaben(FREMDE_MAG, ROLLE_MIT_FREIGABE);

		assertThatThrownBy(() -> this.service.pruefen(angaben)).isInstanceOf(MagNichtZugeordnetException.class);
		assertThatThrownBy(() -> this.service.erstellenUndVersenden(angaben))
			.isInstanceOf(MagNichtZugeordnetException.class);
		nichtsErstelltOderVersendet();
	}

	@Test
	@UseCase(id = "UC-001", scenario = "Steps 5-6", businessRules = { "BR-003" })
	void rolle_ohne_freigegebenen_fragenkatalog_wird_abgewiesen() {
		EinladungsAngaben angaben = angaben(EIGENE_MAG, ROLLE_OHNE_FREIGABE);

		assertThatThrownBy(() -> this.service.pruefen(angaben)).isInstanceOf(RolleOhneFreigabeException.class);
		assertThatThrownBy(() -> this.service.erstellenUndVersenden(angaben))
			.isInstanceOf(RolleOhneFreigabeException.class);
		nichtsErstelltOderVersendet();
	}

	@Test
	@WithMockUser(username = "uc001-teamleitung", roles = "ADMINISTRATOR")
	@UseCase(id = "UC-001", businessRules = { "BR-009" }, requirements = { "NFR-005" })
	void aufruf_ohne_rolle_teamleitung_wird_abgewiesen() {
		EinladungsAngaben angaben = angaben(EIGENE_MAG, ROLLE_MIT_FREIGABE);

		assertThatThrownBy(() -> this.service.auswahl()).isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> this.service.pruefen(angaben)).isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> this.service.erstellenUndVersenden(angaben)).isInstanceOf(AccessDeniedException.class);
		nichtsErstelltOderVersendet();
	}

	private static EinladungsAngaben angaben(long magId, long rolleId) {
		return new EinladungsAngaben(magId, rolleId, List.of("anna.beispiel@example.com"), 30);
	}

	private void nichtsErstelltOderVersendet() {
		assertThat(MAIL.gesendet()).isEmpty();
		assertThat(this.jdbc.sql("select count(*) from einladung where erstellt_von_id in (9201, 9202)")
			.query(Long.class)
			.single()).isZero();
	}

}
