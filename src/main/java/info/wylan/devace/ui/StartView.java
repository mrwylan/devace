package info.wylan.devace.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

/**
 * Startseite nach der Anmeldung: leitet je nach Berechtigung weiter (OQ-073). Der Administrator landet bei MAGs und
 * Rollen (UC-008), die Teamleitung bei den Einladungen (UC-001). Für andere Berechtigungen gibt es noch keine Ansicht.
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("devace")
@PermitAll
public class StartView extends Div implements BeforeEnterObserver {

	private final AuthenticationContext authenticationContext;

	public StartView(AuthenticationContext authenticationContext) {
		this.authenticationContext = authenticationContext;
		add(new Paragraph("Für Ihre Berechtigung gibt es noch keine Ansicht."));
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		if (this.authenticationContext.hasRole("ADMINISTRATOR")) {
			event.forwardTo(MagsUndRollenView.class);
		}
		else if (this.authenticationContext.hasRole("TEAMLEITUNG")) {
			event.forwardTo(EinladungErstellenUndVersendenView.class);
		}
	}

}
