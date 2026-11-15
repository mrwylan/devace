package info.wylan.devace.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import info.wylan.devace.persistence.BenutzerKonto;
import info.wylan.devace.persistence.BenutzerRepository;
import info.wylan.devace.persistence.HandlungsfeldRepository;
import info.wylan.devace.persistence.MagListItem;
import info.wylan.devace.persistence.RolleOption;
import info.wylan.devace.persistence.ScoreRepository;
import info.wylan.devace.service.AssessmentService.FeldScore;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-004 Radar-Chart einer MAG auswerten. Die Teamleitung sieht je Handlungsfeld nur den Mittelwert der Scores ihrer
 * MAG und Rolle (BR-005); der Service ändert keine Daten.
 */
@Service
// UC-004 BR-007: Nur die angemeldete Teamleitung wertet Radar-Charts aus.
@PreAuthorize("hasRole('TEAMLEITUNG')")
@Transactional(readOnly = true)
public class AuswertungService {

	private final ScoreRepository scoreRepository;

	private final HandlungsfeldRepository handlungsfeldRepository;

	private final BenutzerRepository benutzerRepository;

	public AuswertungService(ScoreRepository scoreRepository, HandlungsfeldRepository handlungsfeldRepository,
			BenutzerRepository benutzerRepository) {
		this.scoreRepository = scoreRepository;
		this.handlungsfeldRepository = handlungsfeldRepository;
		this.benutzerRepository = benutzerRepository;
	}

	/**
	 * UC-004 step 2: die MAGs der Teamleitung, zu denen Scores vorliegen; leer bei A1.
	 */
	// UC-004 BR-002, BR-004: nur eigene MAGs, und nur solche mit mindestens einem Score
	public List<MagListItem> mags() {
		return this.scoreRepository.magsMitScores(aktuelleTeamleitung().id());
	}

	/**
	 * UC-004 step 2: die Rollen, zu denen die gewählte MAG Scores hat.
	 */
	public List<RolleOption> rollen(long magId) {
		pruefeEigeneMag(magId);
		// UC-004 BR-004: nur Rollen, zu denen die MAG mindestens einen Score hat
		return this.scoreRepository.rollenMitScores(magId);
	}

	/**
	 * UC-004 step 4 und A2: das Radar-Chart der MAG und Rolle, je Handlungsfeld in dessen Reihenfolge.
	 */
	public List<FeldScore> radarChart(long magId, long rolleId) {
		pruefeEigeneMag(magId);
		// UC-004 BR-001, BR-003: Mittelwert je Handlungsfeld, ohne Mindestzahl an Scores
		Map<Long, BigDecimal> mittelwerte = this.scoreRepository.mittelwerte(magId, rolleId);
		// UC-004 BR-006: Ein Handlungsfeld ohne Score zeigt keinen Wert.
		return this.handlungsfeldRepository.findAll()
			.stream()
			.map((feld) -> new FeldScore(feld.id(), feld.name(), mittelwerte.get(feld.id())))
			.toList();
	}

	// UC-004 BR-002: Die Teamleitung wertet nur MAGs aus, die ihr zugeordnet sind.
	private void pruefeEigeneMag(long magId) {
		boolean eigene = mags().stream().anyMatch((mag) -> mag.id() == magId);
		if (!eigene) {
			throw new MagNichtZugeordnetException();
		}
	}

	private BenutzerKonto aktuelleTeamleitung() {
		String name = SecurityContextHolder.getContext().getAuthentication().getName();
		return this.benutzerRepository.findByBenutzername(name)
			.orElseThrow(() -> new IllegalStateException("Angemeldeter Benutzer ohne Konto"));
	}

}
