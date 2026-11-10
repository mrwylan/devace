package info.wylan.devace.persistence;

import java.time.LocalDateTime;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static info.wylan.devace.persistence.db.Tables.EINLADUNG;

/**
 * Einladungen (UC-001). Die E-Mail-Adresse einer Person hat in keiner Tabelle eine Spalte.
 */
@Repository
public class EinladungRepository {

	public static final String ERSTELLT = "Erstellt";

	public static final String VERSENDET = "Versendet";

	private final DSLContext ctx;

	public EinladungRepository(DSLContext ctx) {
		this.ctx = ctx;
	}

	/**
	 * UC-001 step 8: legt die Einladung mit dem Status Erstellt an und liefert die neue id. Eine bereits
	 * vergebene Referenz führt zu einer {@code DuplicateKeyException}.
	 */
	public long insert(String referenz, long magId, long rolleId, long erstelltVonId, LocalDateTime erstelltAm,
			int gueltigkeitTage) {
		return this.ctx.insertInto(EINLADUNG)
			.set(EINLADUNG.REFERENZ, referenz)
			.set(EINLADUNG.MAG_ID, magId)
			.set(EINLADUNG.ROLLE_ID, rolleId)
			.set(EINLADUNG.ERSTELLT_VON_ID, erstelltVonId)
			.set(EINLADUNG.STATUS, ERSTELLT)
			.set(EINLADUNG.ERSTELLT_AM, erstelltAm)
			.set(EINLADUNG.GUELTIGKEIT_TAGE, gueltigkeitTage)
			.returning(EINLADUNG.ID)
			.fetchSingle()
			.getId();
	}

	/**
	 * UC-001 step 9: setzt den Status auf Versendet, nachdem der Versand gelang.
	 */
	public void markiereVersendet(long id) {
		this.ctx.update(EINLADUNG).set(EINLADUNG.STATUS, VERSENDET).where(EINLADUNG.ID.eq(id)).execute();
	}

	/**
	 * UC-001 A4: verwirft die Einladung einer Adresse, deren Versand scheiterte.
	 */
	public void loesche(long id) {
		this.ctx.deleteFrom(EINLADUNG).where(EINLADUNG.ID.eq(id)).execute();
	}

}
