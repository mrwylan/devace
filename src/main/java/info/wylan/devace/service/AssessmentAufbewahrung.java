package info.wylan.devace.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * UC-002 BR-009: Laufende Assessments ohne Eingabe seit 7 Tagen werden mit ihren Antworten gelöscht (NFR-004).
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
class AssessmentAufbewahrung {

	private static final Logger log = LoggerFactory.getLogger(AssessmentAufbewahrung.class);

	private final AssessmentService service;

	AssessmentAufbewahrung(AssessmentService service) {
		this.service = service;
	}

	@Scheduled(cron = "0 15 3 * * *")
	void loescheLiegengebliebene() {
		int geloescht = this.service.loescheLiegengebliebene();
		if (geloescht > 0) {
			log.info("{} liegengebliebene Assessments gelöscht", geloescht);
		}
	}

}
