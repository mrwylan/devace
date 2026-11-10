package info.wylan.devace.client.emailsystem;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Absender der Einladungen. Zugang und Server stehen unter {@code spring.mail}; alle Werte kommen aus Umgebungsvariablen
 * (DEVACE_MAIL_*), nichts davon im Repository.
 */
@ConfigurationProperties("devace.mail")
record MailProperties(String absender) {
}
