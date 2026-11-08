package info.wylan.devace.service;

import info.wylan.devace.persistence.BenutzerRepository;
import info.wylan.devace.persistence.Berechtigung;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Legt beim Start den ersten Administrator an (OQ-031), wenn Benutzername und Passwort konfiguriert sind und es
 * noch keinen Administrator gibt. Die Werte kommen aus den Umgebungsvariablen DEVACE_ADMIN_USERNAME und
 * DEVACE_ADMIN_PASSWORD und werden nie im Repository abgelegt.
 */
@Component
public class AdministratorStartkonto implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdministratorStartkonto.class);

	private final BenutzerRepository benutzerRepository;

	private final PasswordEncoder passwordEncoder;

	private final String benutzername;

	private final String passwort;

	public AdministratorStartkonto(BenutzerRepository benutzerRepository, PasswordEncoder passwordEncoder,
			@Value("${devace.admin.username:}") String benutzername,
			@Value("${devace.admin.password:}") String passwort) {
		this.benutzerRepository = benutzerRepository;
		this.passwordEncoder = passwordEncoder;
		this.benutzername = benutzername;
		this.passwort = passwort;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (this.benutzername.isBlank() || this.passwort.isBlank()) {
			return;
		}
		if (this.benutzerRepository.existsByBerechtigung(Berechtigung.ADMINISTRATOR)) {
			return;
		}
		this.benutzerRepository.insert(this.benutzername, this.passwordEncoder.encode(this.passwort),
				Berechtigung.ADMINISTRATOR);
		log.info("Administrator '{}' angelegt.", this.benutzername);
	}

}
