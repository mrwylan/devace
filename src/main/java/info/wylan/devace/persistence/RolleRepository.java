package info.wylan.devace.persistence;

import static info.wylan.devace.persistence.db.Tables.FRAGENKATALOG;
import static info.wylan.devace.persistence.db.Tables.ROLLE;
import static org.jooq.impl.DSL.multiset;
import static org.jooq.impl.DSL.select;

import java.util.List;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Records;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

@Repository
public class RolleRepository {

	private final DSLContext ctx;

	public RolleRepository(DSLContext ctx) {
		this.ctx = ctx;
	}

	/**
	 * UC-008 step 2: alle Rollen mit Version und Status ihrer Fragenkataloge.
	 */
	public List<RolleListItem> findAll() {
		return this.ctx
			.select(ROLLE.ID, ROLLE.NAME,
					multiset(select(FRAGENKATALOG.VERSION, FRAGENKATALOG.STATUS).from(FRAGENKATALOG)
						.where(FRAGENKATALOG.ROLLE_ID.eq(ROLLE.ID))
						.orderBy(FRAGENKATALOG.VERSION.desc()))
						.convertFrom(r -> r.map(Records.mapping(KatalogUebersicht::new))))
			.from(ROLLE)
			.orderBy(DSL.lower(ROLLE.NAME))
			.fetch(Records.mapping(RolleListItem::new));
	}

	public boolean existsById(long id) {
		return this.ctx.fetchExists(ROLLE, ROLLE.ID.eq(id));
	}

	/**
	 * UC-001 step 2: die Rollen mit freigegebenem Fragenkatalog.
	 */
	public List<RolleOption> findMitFreigegebenemFragenkatalog() {
		return this.ctx.select(ROLLE.ID, ROLLE.NAME)
			.from(ROLLE)
			// UC-001 BR-003: Nur Rollen, für die ein freigegebener Fragenkatalog besteht.
			.where(DSL.exists(DSL.selectOne()
				.from(FRAGENKATALOG)
				.where(FRAGENKATALOG.ROLLE_ID.eq(ROLLE.ID).and(FRAGENKATALOG.STATUS.eq("Freigegeben")))))
			.orderBy(DSL.lower(ROLLE.NAME))
			.fetch(Records.mapping(RolleOption::new));
	}

	/**
	 * UC-002 step 2: der Name der Rolle, falls es sie gibt.
	 */
	public java.util.Optional<String> findName(long rolleId) {
		return this.ctx.select(ROLLE.NAME).from(ROLLE).where(ROLLE.ID.eq(rolleId)).fetchOptional(ROLLE.NAME);
	}

	/**
	 * UC-001 BR-003: hat die Rolle einen freigegebenen Fragenkatalog?
	 */
	public boolean hatFreigegebenenFragenkatalog(long rolleId) {
		// UC-001 BR-003: Nur Rollen, für die ein freigegebener Fragenkatalog besteht.
		return this.ctx.fetchExists(FRAGENKATALOG,
				FRAGENKATALOG.ROLLE_ID.eq(rolleId).and(FRAGENKATALOG.STATUS.eq("Freigegeben")));
	}

	/**
	 * Prüft, ob der Name einer anderen Rolle gehört. Beim Ändern wird die eigene Rolle über
	 * {@code ausserId} ausgenommen.
	 */
	public boolean existsByName(String name, Long ausserId) {
		// UC-008 BR-003: Der Name gehört zu höchstens einer Rolle (ohne Schreibweise).
		Condition condition = DSL.lower(ROLLE.NAME).eq(DSL.lower(DSL.val(name)));
		if (ausserId != null) {
			condition = condition.and(ROLLE.ID.ne(ausserId));
		}
		return this.ctx.fetchExists(ROLLE, condition);
	}

	/**
	 * Legt die Rolle an und liefert die neue id. Ein vorhandener Name führt zu einer
	 * {@code DuplicateKeyException}.
	 */
	public long insert(Rolle rolle) {
		return this.ctx.insertInto(ROLLE).set(ROLLE.NAME, rolle.name()).returning(ROLLE.ID).fetchSingle().getId();
	}

	/**
	 * Ändert die Rolle; liefert {@code false}, wenn es sie nicht gibt.
	 */
	public boolean update(Rolle rolle) {
		return this.ctx.update(ROLLE).set(ROLLE.NAME, rolle.name()).where(ROLLE.ID.eq(rolle.id())).execute() == 1;
	}

}
