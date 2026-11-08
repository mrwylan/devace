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

/**
 * Rahmen der Anwendung mit Navigation und Abmelden.
 */
@PermitAll
public class MainLayout extends AppLayout {

	public MainLayout(AuthenticationContext authenticationContext) {
		H1 title = new H1("devace");
		title.getStyle().set("font-size", "var(--aura-font-size-l, 1.25rem)").set("margin", "0");

		Button abmelden = new Button("Abmelden", event -> authenticationContext.logout());
		abmelden.setId("abmelden");

		SideNav navigation = new SideNav();
		navigation.addItem(new SideNavItem("MAGs und Rollen", MagsUndRollenView.class, VaadinIcon.GROUP.create()));

		navigation.addItem(new SideNavItem("Fragenkatalog erstellen", FragenkatalogUndEntscheidungsmodellErstellenView.class,
				VaadinIcon.LIST_OL.create()));

		addToNavbar(new DrawerToggle(), title, abmelden);
		addToDrawer(navigation);
	}

}
