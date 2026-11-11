package info.wylan.devace.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

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

}
