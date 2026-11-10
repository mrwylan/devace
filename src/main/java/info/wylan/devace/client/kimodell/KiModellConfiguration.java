package info.wylan.devace.client.kimodell;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(KiProperties.class)
class KiModellConfiguration {

}
