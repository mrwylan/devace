package info.wylan.devace.persistence;

import static info.wylan.devace.persistence.db.Tables.BENUTZER;
import static info.wylan.devace.persistence.db.Tables.MAG;

import java.util.List;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Records;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

@Repository
public class MagRepository {

	private final DSLContext ctx;

	public MagRepository(DSLContext ctx) {
		this.ctx = ctx;
	}

	/**
	 * UC-008 step 2: alle MAGs, geordnet nach Scrum Team und Linienteam.
	 */
	public List<MagListItem> findAll() {
		return this.ctx
			.select(MAG.ID, MAG.SCRUM_TEAM, MAG.LINIENTEAM, MAG.TEAMLEITUNG_ID, BENUTZER.BENUTZERNAME)
			.from(MAG)
			.leftJoin(BENUTZER)
			.on(BENUTZER.ID.eq(MAG.TEAMLEITUNG_ID))
			.orderBy(DSL.lower(MAG.SCRUM_TEAM), DSL.lower(MAG.LINIENTEAM))
			.fetch(Records.mapping(MagListItem::new));
	}

	public boolean existsById(long id) {
		return this.ctx.fetchExists(MAG, MAG.ID.eq(id));
	}

	/**
	 * UC-001 step 2: die MAGs, die der Teamleitung zugeordnet sind.
	 */
	public List<MagListItem> findByTeamleitung(long teamleitungId) {
		return this.ctx.select(MAG.ID, MAG.SCRUM_TEAM, MAG.LINIENTEAM, MAG.TEAMLEITUNG_ID, BENUTZER.BENUTZERNAME)
			.from(MAG)
			.leftJoin(BENUTZER)
			.on(BENUTZER.ID.eq(MAG.TEAMLEITUNG_ID))
			// UC-001 BR-002: Nur MAGs, die der Teamleitung zugeordnet sind.
			.where(MAG.TEAMLEITUNG_ID.eq(teamleitungId))
			.orderBy(DSL.lower(MAG.SCRUM_TEAM), DSL.lower(MAG.LINIENTEAM))
			.fetch(Records.mapping(MagListItem::new));
	}

	/**
	 * UC-001 BR-002: gehört die MAG zur Teamleitung?
	 */
	public boolean gehoertZurTeamleitung(long magId, long teamleitungId) {
		// UC-001 BR-002: Nur MAGs, die der Teamleitung zugeordnet sind.
		return this.ctx.fetchExists(MAG, MAG.ID.eq(magId).and(MAG.TEAMLEITUNG_ID.eq(teamleitungId)));
	}

	/**
	 * Prüft, ob die Kombination aus Scrum Team und Linienteam einer anderen MAG gehört. Beim
	 * Ändern wird die eigene MAG über {@code ausserId} ausgenommen.
	 */
	public boolean existsByScrumTeamAndLinienteam(String scrumTeam, String linienteam, Long ausserId) {
		// UC-008 BR-002: Scrum Team und Linienteam gehören zu höchstens einer MAG (ohne Schreibweise).
		Condition condition = DSL.lower(MAG.SCRUM_TEAM)
			.eq(DSL.lower(DSL.val(scrumTeam)))
			.and(DSL.lower(MAG.LINIENTEAM).eq(DSL.lower(DSL.val(linienteam))));
		if (ausserId != null) {
			condition = condition.and(MAG.ID.ne(ausserId));
		}
		return this.ctx.fetchExists(MAG, condition);
	}

	/**
	 * Legt die MAG an und liefert die neue id. Eine bereits vorhandene Kombination führt zu einer
	 * {@code DuplicateKeyException}.
	 */
	public long insert(Mag mag) {
		return this.ctx.insertInto(MAG)
			.set(MAG.SCRUM_TEAM, mag.scrumTeam())
			.set(MAG.LINIENTEAM, mag.linienteam())
			.set(MAG.TEAMLEITUNG_ID, mag.teamleitungId())
			.returning(MAG.ID)
			.fetchSingle()
			.getId();
	}

	/**
	 * Ändert die MAG; liefert {@code false}, wenn es sie nicht gibt.
	 */
	public boolean update(Mag mag) {
		return this.ctx.update(MAG)
			.set(MAG.SCRUM_TEAM, mag.scrumTeam())
			.set(MAG.LINIENTEAM, mag.linienteam())
			.set(MAG.TEAMLEITUNG_ID, mag.teamleitungId())
			.where(MAG.ID.eq(mag.id()))
			.execute() == 1;
	}

}
