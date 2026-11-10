package info.wylan.devace.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Rahmen der Anwendung mit Navigation und Abmelden.
 */
@PermitAll
public class MainLayout extends AppLayout {

	private static final String ROLLE_ADMINISTRATOR = "ROLE_ADMINISTRATOR";

	public MainLayout(AuthenticationContext authenticationContext) {
		H1 title = new H1("devace");
		title.getStyle().set("font-size", "var(--aura-font-size-l, 1.25rem)").set("margin", "0");

		Button abmelden = new Button("Abmelden", event -> authenticationContext.logout());
		abmelden.setId("abmelden");

		boolean administrator = authenticationContext.getAuthenticatedUser(UserDetails.class)
			.map((benutzer) -> benutzer.getAuthorities().stream().anyMatch((recht) -> ROLLE_ADMINISTRATOR.equals(recht.getAuthority())))
			.orElse(false);

		// Die Einträge der Administration sind für alle sichtbar; ohne Berechtigung sind sie deaktiviert.
		SideNav administration = new SideNav("Administration");
		SideNavItem mags = new SideNavItem("MAGs und Rollen", MagsUndRollenView.class, VaadinIcon.USER.create());
		SideNavItem katalog = new SideNavItem("Fragenkatalog",
				FragenkatalogUndEntscheidungsmodellErstellenView.class, VaadinIcon.LIST_OL.create());
		// UC-006 GR-009, UC-008 GR-007: Einträge ohne passende Berechtigung erscheinen deaktiviert.
		mags.setEnabled(administrator);
		katalog.setEnabled(administrator);
		administration.addItem(katalog, mags);

		addToNavbar(new DrawerToggle(), title, abmelden);
		addToDrawer(administration);
	}

}
