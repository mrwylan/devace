package info.wylan.devace.service;

import java.util.List;

import info.wylan.devace.persistence.Rolle;
import info.wylan.devace.persistence.RolleListItem;
import info.wylan.devace.persistence.RolleRepository;
import jakarta.validation.Valid;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Verwaltung der Rollen (UC-008).
 */
// UC-008 GR-007: Nur der angemeldete Administrator verwaltet Rollen.
@PreAuthorize("hasRole('ADMINISTRATOR')")
@Service
@Validated
@Transactional(readOnly = true)
// UC-008 GR-005: Rollen werden nur angelegt und geändert, nie gelöscht; der Service bietet kein Löschen.
public class RolleService {

	private final RolleRepository rolleRepository;

	public RolleService(RolleRepository rolleRepository) {
		this.rolleRepository = rolleRepository;
	}

	/**
	 * UC-008 step 2: die vorhandenen Rollen mit Version und Status ihrer Fragenkataloge.
	 */
	public List<RolleListItem> list() {
		return this.rolleRepository.findAll();
	}

	/**
	 * UC-008 steps 4–7: legt eine Rolle an; A1 bei ungültigen Angaben, A2 wenn es die Rolle schon gibt.
	 */
	@Transactional
	public long anlegen(@Valid RolleEingabe eingabe) {
		pruefe(eingabe, null);
		try {
			return this.rolleRepository.insert(new Rolle(null, eingabe.name()));
		}
		catch (DuplicateKeyException ex) {
			// ein gleichzeitiges Anlegen derselben Rolle ist an der Prüfung vorbeigekommen
			throw new RolleBereitsVorhandenException();
		}
	}

	/**
	 * UC-008 steps 4–7: ändert den Namen einer Rolle; bestehende Einladungen, Fragenkataloge und Scores bleiben
	 * ihr zugeordnet.
	 */
	@Transactional
	// UC-008 GR-006: Die Änderung betrifft nur den Namen der Rolle; ihre id und damit ihre Zuordnungen bleiben.
	public void aendern(long id, @Valid RolleEingabe eingabe) {
		if (!this.rolleRepository.existsById(id)) {
			throw new RolleNichtGefundenException();
		}
		pruefe(eingabe, id);
		try {
			this.rolleRepository.update(new Rolle(id, eingabe.name()));
		}
		catch (DuplicateKeyException ex) {
			throw new RolleBereitsVorhandenException();
		}
	}

	private void pruefe(RolleEingabe eingabe, Long ausserId) {
		// UC-008 GR-003: Der Name gehört zu höchstens einer Rolle.
		if (this.rolleRepository.existsByName(eingabe.name(), ausserId)) {
			throw new RolleBereitsVorhandenException();
		}
	}

}
