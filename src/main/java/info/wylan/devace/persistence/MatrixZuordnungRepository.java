package info.wylan.devace.persistence;

import static info.wylan.devace.persistence.db.Tables.HANDLUNGSFELD;
import static info.wylan.devace.persistence.db.Tables.MATRIX_ZUORDNUNG;

import java.util.List;
import java.util.Optional;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Records;
import org.jooq.impl.DSL;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

@Repository
public class MatrixZuordnungRepository {

	private final DSLContext ctx;

	public MatrixZuordnungRepository(DSLContext ctx) {
		this.ctx = ctx;
	}

	/**
	 * UC-010 step 2: alle Zuordnungen, nach Handlungsfeld und Name der Spalte geordnet.
	 */
	public List<MatrixZuordnungListItem> findAll() {
		return this.ctx
			.select(MATRIX_ZUORDNUNG.ID, HANDLUNGSFELD.ID, HANDLUNGSFELD.NUMMER, HANDLUNGSFELD.NAME,
					MATRIX_ZUORDNUNG.MATRIX_SPALTE, MATRIX_ZUORDNUNG.BEGRUENDUNG)
			.from(MATRIX_ZUORDNUNG)
			.join(HANDLUNGSFELD)
			.on(HANDLUNGSFELD.ID.eq(MATRIX_ZUORDNUNG.HANDLUNGSFELD_ID))
			.orderBy(HANDLUNGSFELD.NUMMER, DSL.lower(MATRIX_ZUORDNUNG.MATRIX_SPALTE))
			.fetch(Records.mapping(MatrixZuordnungListItem::new));
	}

	public boolean existsById(long id) {
		return this.ctx.fetchExists(MATRIX_ZUORDNUNG, MATRIX_ZUORDNUNG.ID.eq(id));
	}

	/**
	 * UC-010 A2: das Handlungsfeld, dem die Spalte bereits zugeordnet ist. Beim Ändern wird die eigene Zuordnung
	 * über {@code ausserId} ausgenommen.
	 */
	public Optional<Handlungsfeld> findHandlungsfeldDerSpalte(String matrixSpalte, @Nullable Long ausserId) {
		// UC-010 BR-002: Dieselbe Spalte ohne Groß-/Kleinschreibung und umgebende Leerzeichen.
		Condition condition = DSL.lower(DSL.trim(MATRIX_ZUORDNUNG.MATRIX_SPALTE))
			.eq(DSL.lower(DSL.trim(DSL.val(matrixSpalte))));
		if (ausserId != null) {
			condition = condition.and(MATRIX_ZUORDNUNG.ID.ne(ausserId));
		}
		return this.ctx.select(HANDLUNGSFELD.ID, HANDLUNGSFELD.NUMMER, HANDLUNGSFELD.NAME, HANDLUNGSFELD.FOKUS)
			.from(MATRIX_ZUORDNUNG)
			.join(HANDLUNGSFELD)
			.on(HANDLUNGSFELD.ID.eq(MATRIX_ZUORDNUNG.HANDLUNGSFELD_ID))
			.where(condition)
			.fetchOptional(Records.mapping(Handlungsfeld::new));
	}

	/**
	 * Legt die Zuordnung an und liefert die neue id. Eine bereits zugeordnete Spalte führt zu einer
	 * {@code DuplicateKeyException} (matrix_zuordnung_matrix_spalte_uk).
	 */
	public long insert(MatrixZuordnung zuordnung) {
		return this.ctx.insertInto(MATRIX_ZUORDNUNG)
			.set(MATRIX_ZUORDNUNG.HANDLUNGSFELD_ID, zuordnung.handlungsfeldId())
			.set(MATRIX_ZUORDNUNG.MATRIX_SPALTE, zuordnung.matrixSpalte())
			.set(MATRIX_ZUORDNUNG.BEGRUENDUNG, zuordnung.begruendung())
			.returning(MATRIX_ZUORDNUNG.ID)
			.fetchSingle()
			.getId();
	}

	/**
	 * Ändert die Zuordnung; liefert {@code false}, wenn es sie nicht gibt.
	 */
	public boolean update(MatrixZuordnung zuordnung) {
		return this.ctx.update(MATRIX_ZUORDNUNG)
			.set(MATRIX_ZUORDNUNG.HANDLUNGSFELD_ID, zuordnung.handlungsfeldId())
			.set(MATRIX_ZUORDNUNG.MATRIX_SPALTE, zuordnung.matrixSpalte())
			.set(MATRIX_ZUORDNUNG.BEGRUENDUNG, zuordnung.begruendung())
			.where(MATRIX_ZUORDNUNG.ID.eq(zuordnung.id()))
			.execute() == 1;
	}

	/**
	 * Entfernt die Zuordnung; liefert {@code false}, wenn es sie nicht gibt.
	 */
	public boolean delete(long id) {
		return this.ctx.deleteFrom(MATRIX_ZUORDNUNG).where(MATRIX_ZUORDNUNG.ID.eq(id)).execute() == 1;
	}

}
