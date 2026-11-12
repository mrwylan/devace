package info.wylan.devace.persistence;

import static info.wylan.devace.persistence.db.Tables.FRAGE;
import static info.wylan.devace.persistence.db.Tables.FRAGENKATALOG;
import static info.wylan.devace.persistence.db.Tables.ROLLE;
import static info.wylan.devace.persistence.db.Tables.STUFENVORGABE;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.multiset;
import static org.jooq.impl.DSL.select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

@Repository
public class FragenkatalogRepository {

	/** Status des einzigen Katalogs einer Rolle, den UC-006 ändert. */
	public static final String ENTWURF = "Entwurf";

	public static final String FREIGEGEBEN = "Freigegeben";

	public static final String ARCHIVIERT = "Archiviert";

	private final DSLContext ctx;

	public FragenkatalogRepository(DSLContext ctx) {
		this.ctx = ctx;
	}

	/**
	 * UC-006 step 3: der Entwurf der Rolle, falls es einen gibt.
	 */
	public Optional<Fragenkatalog> findEntwurf(long rolleId) {
		return find(rolleId, ENTWURF);
	}

	/**
	 * UC-006 step 3: die freigegebene Version der Rolle, falls es eine gibt.
	 */
	public Optional<Fragenkatalog> findFreigegeben(long rolleId) {
		return find(rolleId, FREIGEGEBEN);
	}

	private Optional<Fragenkatalog> find(long rolleId, String status) {
		return this.ctx.select(FRAGENKATALOG.ID, FRAGENKATALOG.ROLLE_ID, FRAGENKATALOG.VERSION, FRAGENKATALOG.STATUS)
			.from(FRAGENKATALOG)
			.where(FRAGENKATALOG.ROLLE_ID.eq(rolleId))
			// UC-006 BR-005: Entwurf und freigegebene Version werden über den Status unterschieden.
			.and(FRAGENKATALOG.STATUS.eq(status))
			.fetchOptional(Records.mapping((id, rolle, version, st) -> new Fragenkatalog(id, rolle, version, st,
					fragen(id))));
	}

	private List<KatalogFrage> fragen(long katalogId) {
		return this.ctx
			.select(FRAGE.HANDLUNGSFELD_ID, FRAGE.TEXT,
					multiset(select(STUFENVORGABE.BESCHREIBUNG).from(STUFENVORGABE)
						.where(STUFENVORGABE.FRAGE_ID.eq(FRAGE.ID))
						.orderBy(STUFENVORGABE.STUFE)).convertFrom(r -> r.map(rec -> rec.value1())))
			.from(FRAGE)
			.where(FRAGE.FRAGENKATALOG_ID.eq(katalogId))
			.orderBy(FRAGE.POSITION)
			.fetch(Records.mapping(KatalogFrage::new));
	}

	/**
	 * UC-006 BR-004: höchste Versionsnummer der Rolle, 0 ohne Fragenkatalog.
	 */
	public int hoechsteVersion(long rolleId) {
		Integer max = this.ctx.select(org.jooq.impl.DSL.max(FRAGENKATALOG.VERSION))
			.from(FRAGENKATALOG)
			.where(FRAGENKATALOG.ROLLE_ID.eq(rolleId))
			.fetchOne(0, Integer.class);
		return (max != null) ? max : 0;
	}

	/**
	 * UC-006 step 11: legt einen Fragenkatalog mit dem Status Entwurf an und liefert seine id. Ein zweiter Entwurf
	 * der Rolle führt zu einer {@code DuplicateKeyException}.
	 */
	public long insertEntwurf(long rolleId, int version, LocalDateTime erstelltAm) {
		return this.ctx.insertInto(FRAGENKATALOG)
			.set(FRAGENKATALOG.ROLLE_ID, rolleId)
			.set(FRAGENKATALOG.VERSION, version)
			.set(FRAGENKATALOG.STATUS, ENTWURF)
			.set(FRAGENKATALOG.ERSTELLT_AM, erstelltAm)
			.returning(FRAGENKATALOG.ID)
			.fetchSingle()
			.getId();
	}

	/**
	 * UC-006 step 11: ersetzt die Fragen des Katalogs durch die übergebenen; die Position ergibt sich aus der
	 * Reihenfolge, die Stufenvorgaben der alten Fragen entfallen mit ihnen.
	 */
	public void ersetzeFragen(long katalogId, List<KatalogFrage> fragen) {
		this.ctx.deleteFrom(FRAGE).where(FRAGE.FRAGENKATALOG_ID.eq(katalogId)).execute();
		int position = 1;
		for (KatalogFrage frage : fragen) {
			long frageId = this.ctx.insertInto(FRAGE)
				.set(FRAGE.FRAGENKATALOG_ID, katalogId)
				.set(FRAGE.HANDLUNGSFELD_ID, frage.handlungsfeldId())
				.set(FRAGE.TEXT, frage.text())
				.set(FRAGE.POSITION, position++)
				.returning(FRAGE.ID)
				.fetchSingle()
				.getId();
			for (int stufe = 0; stufe < frage.vorgaben().size(); stufe++) {
				this.ctx.insertInto(STUFENVORGABE)
					.set(STUFENVORGABE.FRAGE_ID, frageId)
					.set(STUFENVORGABE.STUFE, stufe)
					.set(STUFENVORGABE.BESCHREIBUNG, frage.vorgaben().get(stufe))
					.execute();
			}
		}
	}

	/**
	 * UC-007 step 2: die Rollen mit einem Entwurf, nach Name geordnet, mit der bisher freigegebenen Version.
	 */
	public List<FreigabeKandidat> findRollenMitEntwurf() {
		info.wylan.devace.persistence.db.tables.Fragenkatalog freigegeben = FRAGENKATALOG.as("freigegeben");
		return this.ctx
			.select(ROLLE.ID, ROLLE.NAME, FRAGENKATALOG.VERSION,
					field(select(freigegeben.VERSION).from(freigegeben)
						.where(freigegeben.ROLLE_ID.eq(ROLLE.ID))
						.and(freigegeben.STATUS.eq(FREIGEGEBEN))))
			.from(FRAGENKATALOG)
			.join(ROLLE)
			.on(ROLLE.ID.eq(FRAGENKATALOG.ROLLE_ID))
			// UC-007 BR-003: Nur ein Entwurf wird freigegeben.
			.where(FRAGENKATALOG.STATUS.eq(ENTWURF))
			.orderBy(ROLLE.NAME)
			.fetch(Records.mapping(FreigabeKandidat::new));
	}

	/**
	 * UC-007 step 8: archiviert die freigegebene Version der Rolle, falls es eine gibt. Freigabezeitpunkt und
	 * Administrator der archivierten Version bleiben erhalten.
	 */
	public void archiviereFreigegebene(long rolleId) {
		// UC-007 BR-006: Nur der Status ändert sich; Assessments behalten ihren Fragenkatalog.
		this.ctx.update(FRAGENKATALOG)
			.set(FRAGENKATALOG.STATUS, ARCHIVIERT)
			.where(FRAGENKATALOG.ROLLE_ID.eq(rolleId))
			// UC-007 BR-002: Mit der Freigabe wird die bisher freigegebene Version archiviert.
			.and(FRAGENKATALOG.STATUS.eq(FREIGEGEBEN))
			.execute();
	}

	/**
	 * UC-007 step 8: gibt den Entwurf mit der id frei und hält Zeitpunkt und Administrator fest; {@code false},
	 * wenn der Katalog kein Entwurf (mehr) ist. Eine zweite freigegebene Version der Rolle führt zu einer
	 * {@code DuplicateKeyException}.
	 */
	public boolean freigeben(long katalogId, LocalDateTime freigegebenAm, long freigegebenVonId) {
		return this.ctx.update(FRAGENKATALOG)
			.set(FRAGENKATALOG.STATUS, FREIGEGEBEN)
			// UC-007 BR-005: Zeitpunkt und Administrator der Freigabe werden festgehalten.
			.set(FRAGENKATALOG.FREIGEGEBEN_AM, freigegebenAm)
			.set(FRAGENKATALOG.FREIGEGEBEN_VON_ID, freigegebenVonId)
			.where(FRAGENKATALOG.ID.eq(katalogId))
			// UC-007 BR-003: Freigegebene und archivierte Fragenkataloge bleiben unverändert.
			.and(FRAGENKATALOG.STATUS.eq(ENTWURF))
			.execute() == 1;
	}

	/**
	 * UC-002 BR-003: die Fragen des Fragenkatalogs in seiner Reihenfolge, mit ihren Stufenvorgaben.
	 */
	public List<BeantwortbareFrage> findBeantwortbareFragen(long katalogId) {
		return this.ctx
			.select(FRAGE.ID, FRAGE.POSITION, FRAGE.TEXT,
					multiset(select(STUFENVORGABE.BESCHREIBUNG).from(STUFENVORGABE)
						.where(STUFENVORGABE.FRAGE_ID.eq(FRAGE.ID))
						.orderBy(STUFENVORGABE.STUFE)).convertFrom(r -> r.map(rec -> rec.value1())))
			.from(FRAGE)
			.where(FRAGE.FRAGENKATALOG_ID.eq(katalogId))
			.orderBy(FRAGE.POSITION)
			.fetch(Records.mapping(BeantwortbareFrage::new));
	}

}
