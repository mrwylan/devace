package info.wylan.devace.persistence;

import static info.wylan.devace.persistence.db.Tables.BENUTZER;

import java.util.List;
import java.util.Optional;

import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

@Repository
public class BenutzerRepository {

	private final DSLContext ctx;

	public BenutzerRepository(DSLContext ctx) {
		this.ctx = ctx;
	}

	public Optional<BenutzerKonto> findByBenutzername(String benutzername) {
		return this.ctx
			.select(BENUTZER.ID, BENUTZER.BENUTZERNAME, BENUTZER.PASSWORT_HASH, BENUTZER.BERECHTIGUNG)
			.from(BENUTZER)
			.where(BENUTZER.BENUTZERNAME.eq(benutzername))
			.fetchOptional(Records.mapping((id, name, hash, berechtigung) -> new BenutzerKonto(id, name, hash,
					Berechtigung.ausBezeichnung(berechtigung))));
	}

	public boolean existsByBerechtigung(Berechtigung berechtigung) {
		return this.ctx.fetchExists(BENUTZER, BENUTZER.BERECHTIGUNG.eq(berechtigung.bezeichnung()));
	}

	public boolean istTeamleitung(long id) {
		return this.ctx.fetchExists(BENUTZER,
				BENUTZER.ID.eq(id).and(BENUTZER.BERECHTIGUNG.eq(Berechtigung.TEAMLEITUNG.bezeichnung())));
	}

	public List<TeamleitungOption> findTeamleitungen() {
		return this.ctx.select(BENUTZER.ID, BENUTZER.BENUTZERNAME)
			.from(BENUTZER)
			.where(BENUTZER.BERECHTIGUNG.eq(Berechtigung.TEAMLEITUNG.bezeichnung()))
			.orderBy(BENUTZER.BENUTZERNAME)
			.fetch(Records.mapping(TeamleitungOption::new));
	}

	/**
	 * Legt ein Benutzerkonto an und liefert die neue id. Ein vorhandener Benutzername führt zu
	 * einer {@code DuplicateKeyException}.
	 */
	public long insert(String benutzername, String passwortHash, Berechtigung berechtigung) {
		return this.ctx.insertInto(BENUTZER)
			.set(BENUTZER.BENUTZERNAME, benutzername)
			.set(BENUTZER.PASSWORT_HASH, passwortHash)
			.set(BENUTZER.BERECHTIGUNG, berechtigung.bezeichnung())
			.returning(BENUTZER.ID)
			.fetchSingle()
			.getId();
	}

}
