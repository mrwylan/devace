package info.wylan.devace.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Records;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import static info.wylan.devace.persistence.db.Tables.BENUTZER;
import static info.wylan.devace.persistence.db.Tables.MAG;
import static info.wylan.devace.persistence.db.Tables.ROLLE;
import static info.wylan.devace.persistence.db.Tables.SCORE;

/**
 * Scores je Handlungsfeld, ohne Verweis auf Einladung, Assessment oder Person (UC-003 BR-002).
 */
@Repository
public class ScoreRepository {

	private final DSLContext ctx;

	public ScoreRepository(DSLContext ctx) {
		this.ctx = ctx;
	}

	/**
	 * UC-003 step 4: speichert einen Score nur mit MAG, Rolle, Handlungsfeld, Fragenkatalog, Wert und Datum.
	 */
	public void insert(long magId, long rolleId, long handlungsfeldId, long fragenkatalogId, BigDecimal wert,
			LocalDate erfasstAm) {
		// UC-003 BR-002: keine Einladung, kein Assessment, keine Antwort, keine Person
		this.ctx.insertInto(SCORE)
			.set(SCORE.MAG_ID, magId)
			.set(SCORE.ROLLE_ID, rolleId)
			.set(SCORE.HANDLUNGSFELD_ID, handlungsfeldId)
			.set(SCORE.FRAGENKATALOG_ID, fragenkatalogId)
			.set(SCORE.WERT, wert)
			.set(SCORE.ERFASST_AM, erfasstAm)
			.execute();
	}

	/**
	 * UC-003 BR-002: löscht Scores, die vor dem Stichtag erfasst wurden, und liefert ihre Zahl.
	 */
	public int loescheErfasstVor(LocalDate stichtag) {
		// UC-003 BR-002: Die Scores werden fünf Jahre aufbewahrt.
		return this.ctx.deleteFrom(SCORE).where(SCORE.ERFASST_AM.lt(stichtag)).execute();
	}

	/**
	 * UC-004 step 2: die MAGs der Teamleitung, zu denen mindestens ein Score vorliegt.
	 */
	public List<MagListItem> magsMitScores(long teamleitungId) {
		return this.ctx.select(MAG.ID, MAG.SCRUM_TEAM, MAG.LINIENTEAM, MAG.TEAMLEITUNG_ID, BENUTZER.BENUTZERNAME)
			.from(MAG)
			.leftJoin(BENUTZER)
			.on(BENUTZER.ID.eq(MAG.TEAMLEITUNG_ID))
			// UC-004 BR-002: Nur MAGs, die der Teamleitung zugeordnet sind.
			.where(MAG.TEAMLEITUNG_ID.eq(teamleitungId))
			// UC-004 BR-004: Nur MAGs mit mindestens einem Score.
			.andExists(DSL.selectOne().from(SCORE).where(SCORE.MAG_ID.eq(MAG.ID)))
			.orderBy(DSL.lower(MAG.SCRUM_TEAM), DSL.lower(MAG.LINIENTEAM))
			.fetch(Records.mapping(MagListItem::new));
	}

	/**
	 * UC-004 step 2: die Rollen, zu denen die MAG mindestens einen Score hat.
	 */
	public List<RolleOption> rollenMitScores(long magId) {
		return this.ctx.select(ROLLE.ID, ROLLE.NAME)
			.from(ROLLE)
			// UC-004 BR-004: Nur Rollen, zu denen die gewählte MAG einen Score hat.
			.whereExists(DSL.selectOne().from(SCORE).where(SCORE.ROLLE_ID.eq(ROLLE.ID)).and(SCORE.MAG_ID.eq(magId)))
			.orderBy(DSL.lower(ROLLE.NAME))
			.fetch(Records.mapping(RolleOption::new));
	}

	/**
	 * UC-004 step 4: der Mittelwert der Scores je Handlungsfeld von MAG und Rolle, nach der Id des Handlungsfelds.
	 * Ein Handlungsfeld ohne Score fehlt in der Abbildung (BR-006).
	 */
	public Map<Long, BigDecimal> mittelwerte(long magId, long rolleId) {
		// UC-004 BR-001: Mittelwert aller Scores, kaufmännisch auf eine Nachkommastelle gerundet,
		// unabhängig vom Fragenkatalog
		Field<BigDecimal> mittelwert = DSL.round(DSL.avg(SCORE.WERT), 1);
		return this.ctx.select(SCORE.HANDLUNGSFELD_ID, mittelwert)
			.from(SCORE)
			.where(SCORE.MAG_ID.eq(magId))
			.and(SCORE.ROLLE_ID.eq(rolleId))
			.groupBy(SCORE.HANDLUNGSFELD_ID)
			.fetchMap(SCORE.HANDLUNGSFELD_ID, mittelwert);
	}

	/**
	 * UC-005 step 2: alle Rollen, zu denen mindestens ein Score vorliegt.
	 */
	public List<RolleOption> rollenMitScores() {
		return this.ctx.select(ROLLE.ID, ROLLE.NAME)
			.from(ROLLE)
			// UC-005 BR-004: Zur Auswahl stehen nur Rollen mit mindestens einem Score.
			.whereExists(DSL.selectOne().from(SCORE).where(SCORE.ROLLE_ID.eq(ROLLE.ID)))
			.orderBy(DSL.lower(ROLLE.NAME))
			.fetch(Records.mapping(RolleOption::new));
	}

	/**
	 * UC-005 step 4: alle MAGs, zu denen Scores der Rolle vorliegen, unabhängig von ihrer Teamleitung.
	 */
	public List<MagListItem> magsMitScoresDerRolle(long rolleId) {
		return this.ctx.select(MAG.ID, MAG.SCRUM_TEAM, MAG.LINIENTEAM, MAG.TEAMLEITUNG_ID, BENUTZER.BENUTZERNAME)
			.from(MAG)
			.leftJoin(BENUTZER)
			.on(BENUTZER.ID.eq(MAG.TEAMLEITUNG_ID))
			// UC-005 BR-004: Nur MAGs, zu denen Scores der gewählten Rolle vorliegen.
			.whereExists(DSL.selectOne().from(SCORE).where(SCORE.MAG_ID.eq(MAG.ID)).and(SCORE.ROLLE_ID.eq(rolleId)))
			.orderBy(DSL.lower(MAG.SCRUM_TEAM), DSL.lower(MAG.LINIENTEAM))
			.fetch(Records.mapping(MagListItem::new));
	}

}
