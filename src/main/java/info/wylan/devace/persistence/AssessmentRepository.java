package info.wylan.devace.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

import static info.wylan.devace.persistence.db.Tables.ANTWORT;
import static info.wylan.devace.persistence.db.Tables.ASSESSMENT;
import static info.wylan.devace.persistence.db.Tables.BEWERTUNG;
import static info.wylan.devace.persistence.db.Tables.FRAGE;
import static info.wylan.devace.persistence.db.Tables.HANDLUNGSFELD;

/**
 * Assessments mit ihren Antworten und Stufen (UC-002). Keine Tabelle trägt eine E-Mail-Adresse oder einen Namen.
 */
@Repository
public class AssessmentRepository {

	public static final String LAUFEND = "Laufend";

	public static final String ABGESCHLOSSEN = "Abgeschlossen";

	private final DSLContext ctx;

	public AssessmentRepository(DSLContext ctx) {
		this.ctx = ctx;
	}

	/**
	 * UC-002 A2: das laufende Assessment zur Einladung, falls es eines gibt, dessen letzte Eingabe nicht vor der
	 * Grenze liegt.
	 */
	public Optional<LaufendesAssessment> findLaufend(long einladungId, LocalDateTime grenze) {
		return this.ctx.select(ASSESSMENT.ID, ASSESSMENT.MAG_ID, ASSESSMENT.ROLLE_ID, ASSESSMENT.FRAGENKATALOG_ID)
			.from(ASSESSMENT)
			.where(ASSESSMENT.EINLADUNG_ID.eq(einladungId))
			.and(ASSESSMENT.STATUS.eq(LAUFEND))
			// UC-002 BR-009: Nach 7 Tagen ohne Eingabe läuft das Assessment nicht mehr.
			.and(ASSESSMENT.LETZTE_EINGABE_AM.ge(grenze))
			.fetchOptional(Records.mapping(LaufendesAssessment::new));
	}

	/**
	 * UC-002 step 4: legt das Assessment mit dem bestätigten Starthinweis an und liefert die neue id. Eine zweite
	 * Anlage zur selben Einladung führt zu einer {@code DuplicateKeyException}.
	 */
	public long insert(long einladungId, long magId, long rolleId, long katalogId, long starthinweisId,
			LocalDateTime jetzt) {
		return this.ctx.insertInto(ASSESSMENT)
			.set(ASSESSMENT.EINLADUNG_ID, einladungId)
			.set(ASSESSMENT.MAG_ID, magId)
			.set(ASSESSMENT.ROLLE_ID, rolleId)
			.set(ASSESSMENT.FRAGENKATALOG_ID, katalogId)
			.set(ASSESSMENT.STARTHINWEIS_ID, starthinweisId)
			.set(ASSESSMENT.STATUS, LAUFEND)
			.set(ASSESSMENT.GESTARTET_AM, jetzt)
			.set(ASSESSMENT.LETZTE_EINGABE_AM, jetzt)
			.returning(ASSESSMENT.ID)
			.fetchSingle()
			.getId();
	}

	/**
	 * UC-002 step 7: die Ids der bereits beantworteten Fragen.
	 */
	public List<Long> beantworteteFragen(long assessmentId) {
		return this.ctx.select(ANTWORT.FRAGE_ID).from(ANTWORT).where(ANTWORT.ASSESSMENT_ID.eq(assessmentId)).fetch(ANTWORT.FRAGE_ID);
	}

	/**
	 * UC-002 step 7: Frage und Antwort der bereits beantworteten Fragen in der Reihenfolge des Fragenkatalogs, ohne
	 * Stufe.
	 */
	public List<Verlauf> verlauf(long assessmentId) {
		return this.ctx.select(FRAGE.POSITION, FRAGE.TEXT, ANTWORT.TEXT)
			.from(ANTWORT)
			.join(FRAGE)
			.on(FRAGE.ID.eq(ANTWORT.FRAGE_ID))
			.where(ANTWORT.ASSESSMENT_ID.eq(assessmentId))
			.orderBy(FRAGE.POSITION)
			.fetch(Records.mapping(Verlauf::new));
	}

	/**
	 * UC-002 step 7: speichert Antwort und Stufe zusammen und merkt den Zeitpunkt der letzten Eingabe vor. Eine
	 * zweite Antwort zur selben Frage führt zu einer {@code DuplicateKeyException}.
	 */
	public void insertAntwort(long assessmentId, long frageId, String text, int stufe, LocalDateTime jetzt) {
		// UC-002 BR-005: Eine Antwort je Frage; die Eindeutigkeit sichert die Tabelle.
		long antwortId = this.ctx.insertInto(ANTWORT)
			.set(ANTWORT.ASSESSMENT_ID, assessmentId)
			.set(ANTWORT.FRAGE_ID, frageId)
			.set(ANTWORT.TEXT, text)
			.set(ANTWORT.BEANTWORTET_AM, jetzt)
			.returning(ANTWORT.ID)
			.fetchSingle()
			.getId();
		this.ctx.insertInto(BEWERTUNG)
			.set(BEWERTUNG.ANTWORT_ID, antwortId)
			.set(BEWERTUNG.STUFE, stufe)
			.set(BEWERTUNG.BEWERTET_AM, jetzt)
			.execute();
		this.ctx.update(ASSESSMENT)
			.set(ASSESSMENT.LETZTE_EINGABE_AM, jetzt)
			.where(ASSESSMENT.ID.eq(assessmentId))
			.execute();
	}

	/**
	 * UC-002 BR-009: löscht laufende Assessments mit ihren Antworten und Stufen, deren letzte Eingabe vor der Grenze
	 * liegt, und liefert ihre Zahl.
	 */
	public int loescheLaufendeSeit(LocalDateTime grenze) {
		return this.ctx.deleteFrom(ASSESSMENT)
			.where(ASSESSMENT.STATUS.eq(LAUFEND))
			// UC-002 BR-009: Nach 7 Tagen ohne Eingabe wird das Assessment mit seinen Antworten gelöscht.
			.and(ASSESSMENT.LETZTE_EINGABE_AM.lt(grenze))
			.execute();
	}

	/**
	 * UC-003 step 2: die Antworten mit ihrer Stufe und dem Handlungsfeld ihrer Frage, in der Reihenfolge des
	 * Fragenkatalogs.
	 */
	public List<BewerteteAntwort> bewerteteAntworten(long assessmentId) {
		return this.ctx.select(FRAGE.HANDLUNGSFELD_ID, FRAGE.TEXT, ANTWORT.TEXT, BEWERTUNG.STUFE)
			.from(ANTWORT)
			.join(FRAGE)
			.on(FRAGE.ID.eq(ANTWORT.FRAGE_ID))
			.join(BEWERTUNG)
			.on(BEWERTUNG.ANTWORT_ID.eq(ANTWORT.ID))
			.join(HANDLUNGSFELD)
			.on(HANDLUNGSFELD.ID.eq(FRAGE.HANDLUNGSFELD_ID))
			.where(ANTWORT.ASSESSMENT_ID.eq(assessmentId))
			.orderBy(FRAGE.POSITION)
			.fetch(Records.mapping(BewerteteAntwort::new));
	}

	/**
	 * UC-003 step 4: setzt das Assessment auf abgeschlossen und löscht seine Antworten mit ihren Stufen.
	 */
	public void abschliessen(long assessmentId) {
		this.ctx.update(ASSESSMENT).set(ASSESSMENT.STATUS, ABGESCHLOSSEN).where(ASSESSMENT.ID.eq(assessmentId)).execute();
		// UC-003 BR-007: Die Antworten werden gelöscht, die Scores bleiben erhalten.
		this.ctx.deleteFrom(ANTWORT).where(ANTWORT.ASSESSMENT_ID.eq(assessmentId)).execute();
	}

}
