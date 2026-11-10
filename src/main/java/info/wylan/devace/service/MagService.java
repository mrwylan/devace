package info.wylan.devace.service;

import java.util.List;

import info.wylan.devace.persistence.BenutzerRepository;
import info.wylan.devace.persistence.Mag;
import info.wylan.devace.persistence.MagListItem;
import info.wylan.devace.persistence.MagRepository;
import info.wylan.devace.persistence.TeamleitungOption;
import jakarta.validation.Valid;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Verwaltung der MAGs (UC-008).
 */
// UC-008 BR-007: Nur der angemeldete Administrator verwaltet MAGs.
@PreAuthorize("hasRole('ADMINISTRATOR')")
@Service
@Validated
@Transactional(readOnly = true)
// UC-008 BR-005: MAGs werden nur angelegt und geändert, nie gelöscht; der Service bietet kein Löschen.
public class MagService {

	private final MagRepository magRepository;

	private final BenutzerRepository benutzerRepository;

	public MagService(MagRepository magRepository, BenutzerRepository benutzerRepository) {
		this.magRepository = magRepository;
		this.benutzerRepository = benutzerRepository;
	}

	/**
	 * UC-008 step 2: die vorhandenen MAGs.
	 */
	public List<MagListItem> list() {
		return this.magRepository.findAll();
	}

	/**
	 * UC-008 step 2: die vorhandenen Teamleitungen zur Zuordnung.
	 */
	public List<TeamleitungOption> teamleitungen() {
		return this.benutzerRepository.findTeamleitungen();
	}

	/**
	 * UC-008 steps 4–7: legt eine MAG an; A1 bei ungültigen Angaben, A2 wenn es die MAG schon gibt.
	 */
	@Transactional
	public long anlegen(@Valid MagEingabe eingabe) {
		pruefe(eingabe, null);
		try {
			return this.magRepository.insert(new Mag(null, eingabe.scrumTeam(), eingabe.linienteam(),
					eingabe.teamleitungId()));
		}
		catch (DuplicateKeyException ex) {
			// ein gleichzeitiges Anlegen derselben MAG ist an der Prüfung vorbeigekommen
			throw new MagBereitsVorhandenException();
		}
	}

	/**
	 * UC-008 steps 4–7: ändert eine MAG; bestehende Einladungen, Fragenkataloge und Scores bleiben ihr
	 * zugeordnet.
	 */
	@Transactional
	// UC-008 BR-006: Die Änderung betrifft nur die Angaben der MAG; ihre id und damit ihre Zuordnungen bleiben.
	public void aendern(long id, @Valid MagEingabe eingabe) {
		if (!this.magRepository.existsById(id)) {
			throw new MagNichtGefundenException();
		}
		pruefe(eingabe, id);
		try {
			this.magRepository.update(new Mag(id, eingabe.scrumTeam(), eingabe.linienteam(), eingabe.teamleitungId()));
		}
		catch (DuplicateKeyException ex) {
			throw new MagBereitsVorhandenException();
		}
	}

	private void pruefe(MagEingabe eingabe, Long ausserId) {
		// UC-008 BR-002: Scrum Team und Linienteam gehören zu höchstens einer MAG.
		if (this.magRepository.existsByScrumTeamAndLinienteam(eingabe.scrumTeam(), eingabe.linienteam(), ausserId)) {
			throw new MagBereitsVorhandenException();
		}
		// UC-008 BR-004: Die Teamleitung ist eine vorhandene Teamleitung oder fehlt.
		if (eingabe.teamleitungId() != null && !this.benutzerRepository.istTeamleitung(eingabe.teamleitungId())) {
			throw new TeamleitungNichtVorhandenException();
		}
	}

}
