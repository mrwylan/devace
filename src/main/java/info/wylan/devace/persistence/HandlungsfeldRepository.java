package info.wylan.devace.persistence;

import static info.wylan.devace.persistence.db.Tables.HANDLUNGSFELD;

import java.util.List;

import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

@Repository
public class HandlungsfeldRepository {

	private final DSLContext ctx;

	public HandlungsfeldRepository(DSLContext ctx) {
		this.ctx = ctx;
	}

	/**
	 * UC-006 steps 4 and 7: die sechs Handlungsfelder in der Reihenfolge ihrer Nummer.
	 */
	public List<Handlungsfeld> findAll() {
		return this.ctx.select(HANDLUNGSFELD.ID, HANDLUNGSFELD.NUMMER, HANDLUNGSFELD.NAME, HANDLUNGSFELD.FOKUS)
			.from(HANDLUNGSFELD)
			.orderBy(HANDLUNGSFELD.NUMMER)
			.fetch(Records.mapping(Handlungsfeld::new));
	}

}
