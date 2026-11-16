package info.wylan.devace.persistence;

import static info.wylan.devace.persistence.db.Tables.BENUTZER;
import static info.wylan.devace.persistence.db.Tables.TESTERGEBNIS;
import static info.wylan.devace.persistence.db.Tables.TESTLAUF;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.jooq.DSLContext;
import org.jooq.Records;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

/**
 * UC-009: die Testläufe eines Fragenkatalogs mit ihren Ergebnissen; sie werden nie gelöscht (BR-007).
 */
@Repository
public class TestlaufRepository {

	private final DSLContext ctx;

	public TestlaufRepository(DSLContext ctx) {
		this.ctx = ctx;
	}

	/**
	 * UC-009 step 8: speichert den Testlauf mit Modell, Zeitpunkt, Administrator, Trefferquote und je Testfall der
	 * ermittelten Stufe. Der Aufrufer hält die Transaktion, damit kein unvollständiger Testlauf entsteht.
	 */
	public long insert(long katalogId, long administratorId, LocalDateTime ausgefuehrtAm, String kiModell,
			BigDecimal trefferquote, List<Testergebnis> ergebnisse) {
		long laufId = this.ctx.insertInto(TESTLAUF)
			.set(TESTLAUF.FRAGENKATALOG_ID, katalogId)
			.set(TESTLAUF.AUSGEFUEHRT_VON_ID, administratorId)
			.set(TESTLAUF.AUSGEFUEHRT_AM, ausgefuehrtAm)
			.set(TESTLAUF.KI_MODELL, kiModell)
			.set(TESTLAUF.TREFFERQUOTE, trefferquote)
			.returning(TESTLAUF.ID)
			.fetchSingle()
			.getId();
		for (Testergebnis ergebnis : ergebnisse) {
			// UC-009 BR-007: das Ergebnis mit einer Kopie des Testfalls, damit es ihn überdauert
			this.ctx.insertInto(TESTERGEBNIS)
				.set(TESTERGEBNIS.TESTLAUF_ID, laufId)
				.set(TESTERGEBNIS.TESTFALL_ID, ergebnis.testfallId())
				.set(TESTERGEBNIS.FRAGE_TEXT, ergebnis.frage())
				.set(TESTERGEBNIS.ANTWORTTEXT, ergebnis.antworttext())
				.set(TESTERGEBNIS.ERWARTETE_STUFE, ergebnis.erwarteteStufe())
				.set(TESTERGEBNIS.ERMITTELTE_STUFE, ergebnis.ermittelteStufe())
				.execute();
		}
		return laufId;
	}

	/** UC-009 step 4 und BR-007: die Testläufe des Fragenkatalogs, der jüngste zuerst. */
	public List<Testlauf> laeufe(long katalogId) {
		return this.ctx
			.select(TESTLAUF.ID, TESTLAUF.AUSGEFUEHRT_AM, BENUTZER.BENUTZERNAME, TESTLAUF.KI_MODELL,
					TESTLAUF.TREFFERQUOTE)
			.from(TESTLAUF)
			.join(BENUTZER)
			.on(BENUTZER.ID.eq(TESTLAUF.AUSGEFUEHRT_VON_ID))
			.where(TESTLAUF.FRAGENKATALOG_ID.eq(katalogId))
			.orderBy(TESTLAUF.AUSGEFUEHRT_AM.desc(), TESTLAUF.ID.desc())
			.fetch(Records.mapping(Testlauf::new));
	}

	/** UC-009 step 9 und BR-007: die Ergebnisse eines Testlaufs in der Reihenfolge der Ausführung. */
	public List<Testergebnis> ergebnisse(long laufId) {
		return this.ctx
			.select(TESTERGEBNIS.TESTFALL_ID, TESTERGEBNIS.FRAGE_TEXT, TESTERGEBNIS.ANTWORTTEXT,
					TESTERGEBNIS.ERWARTETE_STUFE, TESTERGEBNIS.ERMITTELTE_STUFE)
			.from(TESTERGEBNIS)
			.where(TESTERGEBNIS.TESTLAUF_ID.eq(laufId))
			.orderBy(TESTERGEBNIS.ID)
			.fetch(Records.mapping(Testergebnis::new));
	}

	/** UC-009 step 4: ein Testlauf mit Zeitpunkt, Administrator, Modell und Trefferquote. */
	public record Testlauf(long id, LocalDateTime ausgefuehrtAm, String administrator, String kiModell,
			BigDecimal trefferquote) {
	}

	/** UC-009 step 8: das Ergebnis eines Testfalls; der Verweis auf den Testfall entfällt mit ihm (BR-007). */
	public record Testergebnis(@Nullable Long testfallId, String frage, String antworttext, int erwarteteStufe,
			int ermittelteStufe) {

		/** UC-009 step 9: Die ermittelte Stufe weicht von der erwarteten ab. */
		public boolean abweichung() {
			return this.erwarteteStufe != this.ermittelteStufe;
		}

	}

}
