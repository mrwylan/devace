package info.wylan.devace;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Die Uhr der Anwendung, damit Regeln, die vom heutigen Tag abhängen, mit festem Datum prüfbar sind.
 */
@Configuration(proxyBeanMethods = false)
class ZeitConfiguration {

	@Bean
	Clock clock() {
		return Clock.systemDefaultZone();
	}

}
