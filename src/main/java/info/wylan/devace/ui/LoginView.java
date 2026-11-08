package info.wylan.devace.ui;

import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Anmeldung mit Benutzername und Passwort (OQ-023).
 */
@Route("login")
@PageTitle("Anmeldung")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

	private final LoginForm login = new LoginForm();

	public LoginView() {
		setSizeFull();
		setAlignItems(FlexComponent.Alignment.CENTER);
		setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

		LoginI18n i18n = LoginI18n.createDefault();
		i18n.getForm().setTitle("devace");
		i18n.getForm().setUsername("Benutzername");
		i18n.getForm().setPassword("Passwort");
		i18n.getForm().setSubmit("Anmelden");
		i18n.getErrorMessage().setTitle("Anmeldung fehlgeschlagen");
		i18n.getErrorMessage().setMessage("Benutzername oder Passwort ist falsch.");
		this.login.setI18n(i18n);
		this.login.setAction("login");
		this.login.setForgotPasswordButtonVisible(false);
		this.login.setId("login-form");
		add(this.login);
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
			this.login.setError(true);
		}
	}

}
