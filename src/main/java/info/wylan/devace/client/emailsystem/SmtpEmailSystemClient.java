package info.wylan.devace.client.emailsystem;

import info.wylan.devace.service.EmailSystemClient;
import info.wylan.devace.service.EmailVersandFehlgeschlagenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Das E-Mail-System über SMTP (UC-001 step 9). Der Adapter schreibt weder Adresse noch Text in ein Log.
 */
@Component
class SmtpEmailSystemClient implements EmailSystemClient {

	private static final Logger log = LoggerFactory.getLogger(SmtpEmailSystemClient.class);

	private final ObjectProvider<JavaMailSender> sender;

	private final MailProperties properties;

	SmtpEmailSystemClient(ObjectProvider<JavaMailSender> sender, MailProperties properties) {
		this.sender = sender;
		this.properties = properties;
	}

	/**
	 * UC-001 step 9 — E-Mail mit Betreff, Text und Link; A4 wenn der Versand scheitert.
	 */
	@Override
	public void einladungVersenden(Einladungsmail mail) {
		JavaMailSender mailSender = this.sender.getIfAvailable();
		if (mailSender == null || this.properties.absender() == null || this.properties.absender().isBlank()) {
			// UC-001 A4: ohne eingerichteten Server oder Absender ist das E-Mail-System nicht erreichbar
			throw new EmailVersandFehlgeschlagenException(
					new IllegalStateException("E-Mail-Server oder Absender nicht eingerichtet"));
		}
		try {
			SimpleMailMessage nachricht = new SimpleMailMessage();
			nachricht.setFrom(this.properties.absender());
			nachricht.setTo(mail.adresse());
			nachricht.setSubject(mail.betreff());
			nachricht.setText(mail.text());
			mailSender.send(nachricht);
		}
		catch (RuntimeException ex) {
			// UC-001 A4: abgelehnt oder nicht erreichbar; nur der Klassenname, nie Adresse oder Text des Systems
			log.warn("E-Mail-Versand gescheitert: {}", ex.getClass().getSimpleName());
			throw new EmailVersandFehlgeschlagenException(ex);
		}
	}

}
