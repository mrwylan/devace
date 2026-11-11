package info.wylan.devace.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * UC-002 BR-009: Laufende Assessments ohne Eingabe seit 7 Tagen werden mit ihren Antworten gelöscht (NFR-004);
 * UC-003 BR-002: Scores werden nach fünf Jahren gelöscht (C-003).
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
class AssessmentAufbewahrung {

	private static final Logger log = LoggerFactory.getLogger(AssessmentAufbewahrung.class);

	private final AssessmentService service;

	AssessmentAufbewahrung(AssessmentService service) {
		this.service = service;
	}

	// UC-002 BR-009: alle 5 Minuten; ein abgelaufenes Assessment ist schon vorher nicht mehr erreichbar.
	@Scheduled(cron = "0 */5 * * * *")
	void loescheLiegengebliebene() {
		int geloescht = this.service.loescheLiegengebliebene();
		if (geloescht > 0) {
			log.info("{} liegengebliebene Assessments gelöscht", geloescht);
		}
	}

	// UC-003 BR-002: einmal täglich; Scores bleiben fünf Jahre erhalten.
	@Scheduled(cron = "0 30 3 * * *")
	void loescheAlteScores() {
		int geloescht = this.service.loescheAlteScores();
		if (geloescht > 0) {
			log.info("{} Scores nach fünf Jahren gelöscht", geloescht);
		}
	}

}
