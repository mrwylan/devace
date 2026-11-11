package info.wylan.devace.persistence;

import java.time.LocalDate;
import java.util.Optional;

import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

import static info.wylan.devace.persistence.db.Tables.STARTHINWEIS;

/**
 * Starthinweise (UC-002 step 2, UC-011).
 */
@Repository
public class StarthinweisRepository {

	private final DSLContext ctx;

	public StarthinweisRepository(DSLContext ctx) {
		this.ctx = ctx;
	}

	/**
	 * UC-002 step 2: der geltende Starthinweis, die Version mit dem jüngsten Beginn der Gültigkeit, der nicht in
	 * der Zukunft liegt.
	 */
	public Optional<Starthinweis> findGeltend(LocalDate heute) {
		return this.ctx.select(STARTHINWEIS.ID, STARTHINWEIS.VERSION, STARTHINWEIS.TEXT, STARTHINWEIS.KI_MODELLE)
			.from(STARTHINWEIS)
			// UC-011 BR-004: Es gilt die Version mit dem jüngsten Beginn, der nicht in der Zukunft liegt.
			.where(STARTHINWEIS.GUELTIG_AB.le(heute))
			.orderBy(STARTHINWEIS.GUELTIG_AB.desc(), STARTHINWEIS.VERSION.desc())
			.limit(1)
			.fetchOptional(Records.mapping(Starthinweis::new));
	}

}
