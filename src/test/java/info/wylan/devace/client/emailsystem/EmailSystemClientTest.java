package info.wylan.devace.client.emailsystem;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import info.wylan.devace.service.EmailSystemClient.Einladungsmail;
import info.wylan.devace.service.EmailVersandFehlgeschlagenException;
import info.wylan.devace.usecase.UseCase;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Adapter des E-Mail-Systems gegen einen SMTP-Stub (GreenMail): kein Versand an einen echten Server.
 */
class EmailSystemClientTest {

	private static final String ABSENDER = "devace@example.test";

	private static final String ADRESSE = "anna.beispiel@example.com";

	private static final String BETREFF = "Einladung zum devace-Assessment";

	private static final String TEXT = "Sie sind zu einem devace-Assessment eingeladen. "
			+ "Über den folgenden Link starten Sie es:\n\nhttp://localhost:8080/assessment/abc";

	@RegisterExtension
	static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP.dynamicPort());

	private SmtpEmailSystemClient adapter(String absender) {
		JavaMailSenderImpl sender = new JavaMailSenderImpl();
		sender.setHost("127.0.0.1");
		sender.setPort(greenMail.getSmtp().getPort());
		StaticListableBeanFactory beans = new StaticListableBeanFactory();
		beans.addBean("mailSender", sender);
		return new SmtpEmailSystemClient(beans.getBeanProvider(JavaMailSender.class), new MailProperties(absender));
	}

	@Test
	@UseCase(id = "UC-001", scenario = "Steps 8-9: Einladung versenden", businessRules = { "BR-007" })
	void versendet_die_einladung_mit_betreff_text_und_link_an_die_adresse() throws Exception {
		adapter(ABSENDER).einladungVersenden(new Einladungsmail(ADRESSE, BETREFF, TEXT));

		MimeMessage[] empfangen = greenMail.getReceivedMessages();
		assertThat(empfangen).hasSize(1);
		MimeMessage nachricht = empfangen[0];
		assertThat(nachricht.getSubject()).isEqualTo(BETREFF);
		assertThat(nachricht.getAllRecipients()).extracting(Object::toString).containsExactly(ADRESSE);
		assertThat(nachricht.getFrom()).extracting(Object::toString).containsExactly(ABSENDER);
		// BR-007: nur der Text und der Link, sonst nichts
		assertThat(((String) nachricht.getContent()).replace("\r\n", "\n").strip()).isEqualTo(TEXT);
	}

	@Test
	@UseCase(id = "UC-001", scenario = "A4: Versand scheitert")
	void meldet_fehlgeschlagen_wenn_der_server_nicht_erreichbar_ist_ohne_die_adresse() {
		SmtpEmailSystemClient adapter = adapter(ABSENDER);
		greenMail.stop();

		assertThatThrownBy(() -> adapter.einladungVersenden(new Einladungsmail(ADRESSE, BETREFF, TEXT)))
			.isInstanceOf(EmailVersandFehlgeschlagenException.class)
			.hasMessage("Der Versand der Einladung ist gescheitert.");
	}

	@Test
	@UseCase(id = "UC-001", scenario = "A4: Versand scheitert")
	void meldet_fehlgeschlagen_ohne_absender_und_sendet_nichts() {
		assertThatThrownBy(() -> adapter(" ").einladungVersenden(new Einladungsmail(ADRESSE, BETREFF, TEXT)))
			.isInstanceOf(EmailVersandFehlgeschlagenException.class)
			.hasMessage("Der Versand der Einladung ist gescheitert.");

		assertThat(greenMail.getReceivedMessages()).isEmpty();
	}

	@Test
	@UseCase(id = "UC-001", scenario = "A4: Versand scheitert")
	void meldet_fehlgeschlagen_ohne_eingerichteten_mailserver() {
		ObjectProvider<JavaMailSender> keiner = new StaticListableBeanFactory().getBeanProvider(JavaMailSender.class);
		SmtpEmailSystemClient adapter = new SmtpEmailSystemClient(keiner, new MailProperties(ABSENDER));

		assertThatThrownBy(() -> adapter.einladungVersenden(new Einladungsmail(ADRESSE, BETREFF, TEXT)))
			.isInstanceOf(EmailVersandFehlgeschlagenException.class)
			.hasMessage("Der Versand der Einladung ist gescheitert.");
	}

}
