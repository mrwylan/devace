package info.wylan.devace.service;

import info.wylan.devace.persistence.BenutzerKonto;
import info.wylan.devace.persistence.BenutzerRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stellt die Benutzerkonten für die Anmeldung bereit (OQ-023).
 */
@Service
@Transactional(readOnly = true)
public class BenutzerKontoService implements UserDetailsService {

	private final BenutzerRepository benutzerRepository;

	public BenutzerKontoService(BenutzerRepository benutzerRepository) {
		this.benutzerRepository = benutzerRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String benutzername) throws UsernameNotFoundException {
		BenutzerKonto konto = this.benutzerRepository.findByBenutzername(benutzername)
			.orElseThrow(() -> new UsernameNotFoundException(benutzername));
		return User.withUsername(konto.benutzername())
			.password(konto.passwortHash())
			.roles(konto.berechtigung().name())
			.build();
	}

}
