package info.wylan.devace.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import info.wylan.devace.persistence.Handlungsfeld;
import info.wylan.devace.persistence.HandlungsfeldRepository;
import info.wylan.devace.persistence.MagListItem;
import info.wylan.devace.persistence.RolleOption;
import info.wylan.devace.persistence.ScoreRepository;
import info.wylan.devace.service.AssessmentService.FeldScore;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-005 Radar-Charts für die Schulungsplanung auswerten. Die Schulungsverantwortliche sieht je Rolle die Radar-Charts
 * aller MAGs mit Scores, je Handlungsfeld nur den Mittelwert (BR-005); der Service ändert keine Daten.
 */
@Service
// UC-005 BR-007: Nur die angemeldete Schulungsverantwortliche wertet die Radar-Charts aller MAGs aus.
@PreAuthorize("hasRole('SCHULUNGSVERANTWORTLICHE')")
@Transactional(readOnly = true)
public class SchulungsplanungService {

	private final ScoreRepository scoreRepository;

	private final HandlungsfeldRepository handlungsfeldRepository;

	public SchulungsplanungService(ScoreRepository scoreRepository, HandlungsfeldRepository handlungsfeldRepository) {
		this.scoreRepository = scoreRepository;
		this.handlungsfeldRepository = handlungsfeldRepository;
	}

	/**
	 * UC-005 step 2: die Rollen, zu denen Scores vorliegen; leer bei A1.
	 */
	// UC-005 BR-004: Zur Auswahl stehen nur Rollen mit mindestens einem Score.
	public List<RolleOption> rollen() {
		return this.scoreRepository.rollenMitScores();
	}

	/**
	 * UC-005 step 4 und A2: die Radar-Charts aller MAGs mit Scores der Rolle, je Handlungsfeld in dessen Reihenfolge.
	 */
	public List<MagRadarChart> radarCharts(long rolleId) {
		List<Handlungsfeld> handlungsfelder = this.handlungsfeldRepository.findAll();
		// UC-005 BR-002: alle MAGs, nicht nur die einer zugeordneten MAG; nur Radar-Charts
		// UC-005 BR-004: nur MAGs, zu denen Scores der gewählten Rolle vorliegen
		return this.scoreRepository.magsMitScoresDerRolle(rolleId).stream().map((mag) -> {
			// UC-005 BR-001: Mittelwert der Scores je Handlungsfeld von MAG und Rolle, wie UC-004 BR-001.
			// UC-005 BR-003: Keine Mindestzahl an Scores, auch für kleine MAGs.
			Map<Long, BigDecimal> mittelwerte = this.scoreRepository.mittelwerte(mag.id(), rolleId);
			// UC-005 BR-006: Ein Handlungsfeld ohne Score zeigt keinen Wert.
			List<FeldScore> werte = handlungsfelder.stream()
				.map((feld) -> new FeldScore(feld.id(), feld.name(), mittelwerte.get(feld.id())))
				.toList();
			return new MagRadarChart(mag, werte);
		}).toList();
	}

	/** UC-005 step 4: das Radar-Chart einer MAG mit dem Mittelwert je Handlungsfeld. */
	public record MagRadarChart(MagListItem mag, List<FeldScore> werte) {
	}

}
