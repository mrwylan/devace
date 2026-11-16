package info.wylan.devace.persistence;

import static info.wylan.devace.persistence.db.Tables.FRAGE;
import static info.wylan.devace.persistence.db.Tables.FRAGENKATALOG;
import static info.wylan.devace.persistence.db.Tables.ROLLE;
import static info.wylan.devace.persistence.db.Tables.STUFENVORGABE;
import static info.wylan.devace.persistence.db.Tables.TESTFALL;
import static info.wylan.devace.persistence.db.Tables.TESTLAUF;
import static org.jooq.impl.DSL.multiset;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.selectCount;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.jooq.DSLContext;
import org.jooq.Records;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

/**
 * UC-009: die Testfälle zu den Fragen eines prüfbaren Fragenkatalogs.
 */
@Repository
public class TestfallRepository {

	private final DSLContext ctx;

	public TestfallRepository(DSLContext ctx) {
		this.ctx = ctx;
	}

	/**
	 * UC-009 step 2: die prüfbaren Fragenkataloge mit der Zahl ihrer Testfälle und dem jüngsten Testlauf.
	 */
	public List<PruefbarerKatalog> pruefbareKataloge() {
		var letzterLauf = select(TESTLAUF.TREFFERQUOTE).from(TESTLAUF)
			.where(TESTLAUF.FRAGENKATALOG_ID.eq(FRAGENKATALOG.ID))
			.orderBy(TESTLAUF.AUSGEFUEHRT_AM.desc(), TESTLAUF.ID.desc())
			.limit(1);
		var letzterZeitpunkt = select(TESTLAUF.AUSGEFUEHRT_AM).from(TESTLAUF)
			.where(TESTLAUF.FRAGENKATALOG_ID.eq(FRAGENKATALOG.ID))
			.orderBy(TESTLAUF.AUSGEFUEHRT_AM.desc(), TESTLAUF.ID.desc())
			.limit(1);
		return this.ctx
			.select(FRAGENKATALOG.ID, ROLLE.NAME, FRAGENKATALOG.VERSION, FRAGENKATALOG.STATUS,
					selectCount().from(TESTFALL)
						.join(FRAGE)
						.on(FRAGE.ID.eq(TESTFALL.FRAGE_ID))
						.where(FRAGE.FRAGENKATALOG_ID.eq(FRAGENKATALOG.ID))
						.<Integer>asField(),
					letzterLauf.<BigDecimal>asField(), letzterZeitpunkt.<LocalDateTime>asField())
			.from(FRAGENKATALOG)
			.join(ROLLE)
			.on(ROLLE.ID.eq(FRAGENKATALOG.ROLLE_ID))
			// UC-009 BR-002: Prüfbar sind Entwurf und freigegebene Version, nicht archivierte Kataloge.
			.where(FRAGENKATALOG.STATUS.in(FragenkatalogRepository.ENTWURF, FragenkatalogRepository.FREIGEGEBEN))
			.orderBy(ROLLE.NAME, FRAGENKATALOG.VERSION)
			.fetch(Records.mapping(PruefbarerKatalog::new));
	}

	/** UC-009 BR-002: ob der Fragenkatalog prüfbar ist. */
	public boolean istPruefbar(long katalogId) {
		// UC-009 BR-002: Prüfbar sind Entwurf und freigegebene Version, nicht archivierte Kataloge.
		return this.ctx.fetchExists(FRAGENKATALOG, FRAGENKATALOG.ID.eq(katalogId)
			.and(FRAGENKATALOG.STATUS.in(FragenkatalogRepository.ENTWURF, FragenkatalogRepository.FREIGEGEBEN)));
	}

	/**
	 * UC-009 step 4: die Fragen des Fragenkatalogs in ihrer Reihenfolge, je mit ihren Testfällen.
	 */
	public List<TestFrage> fragenMitTestfaellen(long katalogId) {
		return this.ctx
			.select(FRAGE.ID, FRAGE.POSITION, FRAGE.TEXT,
					multiset(select(TESTFALL.ID, TESTFALL.ANTWORTTEXT, TESTFALL.ERWARTETE_STUFE).from(TESTFALL)
						.where(TESTFALL.FRAGE_ID.eq(FRAGE.ID))
						.orderBy(TESTFALL.ID)).convertFrom(r -> r.map(Records.mapping(Testfall::new))))
			.from(FRAGE)
			.where(FRAGE.FRAGENKATALOG_ID.eq(katalogId))
			.orderBy(FRAGE.POSITION)
			.fetch(Records.mapping(TestFrage::new));
	}

	/** UC-009 BR-001: der Fragenkatalog, zu dem die Frage gehört. */
	public Optional<Long> katalogDerFrage(long frageId) {
		return this.ctx.select(FRAGE.FRAGENKATALOG_ID)
			.from(FRAGE)
			.where(FRAGE.ID.eq(frageId))
			.fetchOptional(FRAGE.FRAGENKATALOG_ID);
	}

	/** UC-009 step 5: der Fragenkatalog, zu dem der Testfall gehört. */
	public Optional<Long> katalogDesTestfalls(long testfallId) {
		return this.ctx.select(FRAGE.FRAGENKATALOG_ID)
			.from(TESTFALL)
			.join(FRAGE)
			.on(FRAGE.ID.eq(TESTFALL.FRAGE_ID))
			.where(TESTFALL.ID.eq(testfallId))
			.fetchOptional(FRAGE.FRAGENKATALOG_ID);
	}

	/** UC-009 step 5: legt einen Testfall zu einer Frage an. */
	public long insert(long frageId, String antworttext, int erwarteteStufe) {
		return this.ctx.insertInto(TESTFALL)
			.set(TESTFALL.FRAGE_ID, frageId)
			.set(TESTFALL.ANTWORTTEXT, antworttext)
			.set(TESTFALL.ERWARTETE_STUFE, erwarteteStufe)
			.returning(TESTFALL.ID)
			.fetchSingle()
			.getId();
	}

	/** UC-009 step 5: ändert Antwort und erwartete Stufe eines Testfalls. */
	public void update(long testfallId, String antworttext, int erwarteteStufe) {
		this.ctx.update(TESTFALL)
			.set(TESTFALL.ANTWORTTEXT, antworttext)
			.set(TESTFALL.ERWARTETE_STUFE, erwarteteStufe)
			.where(TESTFALL.ID.eq(testfallId))
			.execute();
	}

	/** UC-009 step 5: entfernt einen Testfall; seine Ergebnisse behalten ihre Kopie (BR-007). */
	public void delete(long testfallId) {
		this.ctx.deleteFrom(TESTFALL).where(TESTFALL.ID.eq(testfallId)).execute();
	}

	/**
	 * UC-009 step 7: alle Testfälle des Fragenkatalogs mit Frage und Entscheidungsmodell (BR-003, BR-008).
	 */
	public List<AuszufuehrenderTestfall> auszufuehrende(long katalogId) {
		return this.ctx
			.select(TESTFALL.ID, FRAGE.TEXT,
					multiset(select(STUFENVORGABE.BESCHREIBUNG).from(STUFENVORGABE)
						.where(STUFENVORGABE.FRAGE_ID.eq(FRAGE.ID))
						.orderBy(STUFENVORGABE.STUFE)).convertFrom(r -> r.map(rec -> rec.value1())),
					TESTFALL.ANTWORTTEXT, TESTFALL.ERWARTETE_STUFE)
			.from(TESTFALL)
			.join(FRAGE)
			.on(FRAGE.ID.eq(TESTFALL.FRAGE_ID))
			// UC-009 BR-003: Ein Testlauf führt alle Testfälle des gewählten Fragenkatalogs aus.
			.where(FRAGE.FRAGENKATALOG_ID.eq(katalogId))
			.orderBy(FRAGE.POSITION, TESTFALL.ID)
			.fetch(Records.mapping(AuszufuehrenderTestfall::new));
	}

	/** UC-009 step 2: ein prüfbarer Fragenkatalog mit Testfällen und dem jüngsten Testlauf, falls es einen gibt. */
	public record PruefbarerKatalog(long id, String rolle, int version, String status, int testfaelle,
			@Nullable BigDecimal letzteTrefferquote, @Nullable LocalDateTime letzterLauf) {
	}

	/** UC-009 step 4: eine Frage mit ihren Testfällen. */
	public record TestFrage(long frageId, int position, String text, List<Testfall> testfaelle) {
	}

	/** UC-009 BR-001: vorgegebene Antwort und erwartete Stufe zu einer Frage. */
	public record Testfall(long id, String antworttext, int erwarteteStufe) {
	}

	/** UC-009 step 7: was das KI-Modell zu einem Testfall erhält, und die erwartete Stufe. */
	public record AuszufuehrenderTestfall(long testfallId, String frage, List<String> vorgaben, String antworttext,
			int erwarteteStufe) {
	}

}
