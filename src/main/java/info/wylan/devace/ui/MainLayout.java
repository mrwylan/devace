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

	private static final String ROLLE_TEAMLEITUNG = "ROLE_TEAMLEITUNG";

	private static final String ROLLE_SCHULUNGSVERANTWORTLICHE = "ROLE_SCHULUNGSVERANTWORTLICHE";

	public MainLayout(AuthenticationContext authenticationContext) {
		H1 title = new H1("devace");
		title.getStyle().set("font-size", "var(--aura-font-size-l, 1.25rem)").set("margin", "0");

		Button abmelden = new Button("Abmelden", event -> authenticationContext.logout());
		abmelden.setId("abmelden");

		boolean administrator = authenticationContext.getAuthenticatedUser(UserDetails.class)
			.map((benutzer) -> benutzer.getAuthorities().stream().anyMatch((recht) -> ROLLE_ADMINISTRATOR.equals(recht.getAuthority())))
			.orElse(false);
		boolean teamleitung = authenticationContext.getAuthenticatedUser(UserDetails.class)
			.map((benutzer) -> benutzer.getAuthorities().stream().anyMatch((recht) -> ROLLE_TEAMLEITUNG.equals(recht.getAuthority())))
			.orElse(false);

		boolean schulungsverantwortliche = authenticationContext.getAuthenticatedUser(UserDetails.class)
			.map((benutzer) -> benutzer.getAuthorities()
				.stream()
				.anyMatch((recht) -> ROLLE_SCHULUNGSVERANTWORTLICHE.equals(recht.getAuthority())))
			.orElse(false);

		// Die Einträge sind für alle sichtbar; ohne Berechtigung sind sie deaktiviert.
		SideNav einladungen = new SideNav("Einladungen");
		SideNavItem einladung = new SideNavItem("Einladungen", EinladungErstellenUndVersendenView.class,
				VaadinIcon.PAPERPLANE.create());
		// UC-001 BR-009: Einträge ohne passende Berechtigung erscheinen deaktiviert.
		einladung.setEnabled(teamleitung);
		SideNavItem auswertung = new SideNavItem("Radar-Chart", RadarChartEinerMagAuswertenView.class,
				VaadinIcon.PIE_CHART.create());
		// UC-004 BR-007: Einträge ohne passende Berechtigung erscheinen deaktiviert.
		auswertung.setEnabled(teamleitung);
		einladungen.addItem(einladung, auswertung);
		SideNav schulung = new SideNav("Schulung");
		SideNavItem schulungsplanung = new SideNavItem("Schulungsplanung",
				RadarChartsFuerDieSchulungsplanungAuswertenView.class, VaadinIcon.BAR_CHART.create());
		// UC-005 BR-007: Einträge ohne passende Berechtigung erscheinen deaktiviert.
		schulungsplanung.setEnabled(schulungsverantwortliche);
		schulung.addItem(schulungsplanung);
		// Die Einträge der Administration folgen demselben Muster.
		SideNav administration = new SideNav("Administration");
		SideNavItem mags = new SideNavItem("MAGs und Rollen", MagsUndRollenView.class, VaadinIcon.USER.create());
		SideNavItem katalog = new SideNavItem("Fragenkatalog",
				FragenkatalogUndEntscheidungsmodellErstellenView.class, VaadinIcon.LIST_OL.create());
		SideNavItem freigabe = new SideNavItem("Freigabe", FragenkatalogFreigebenView.class,
				VaadinIcon.CHECK_SQUARE_O.create());
		SideNavItem absicherung = new SideNavItem("KI-Absicherung", KiBewertungMitTestfaellenAbsichernView.class,
				VaadinIcon.FLASK.create());
		SideNavItem matrix = new SideNavItem("Kompetenzmatrix", KompetenzmatrixZuordnungDokumentierenView.class,
				VaadinIcon.TABLE.create());
		// UC-006 BR-009, UC-007 BR-007, UC-008 BR-007: Einträge ohne Berechtigung erscheinen deaktiviert.
		// UC-009 BR-011: Einträge ohne passende Berechtigung erscheinen deaktiviert.
		absicherung.setEnabled(administrator);
		// UC-010 BR-004: Einträge ohne passende Berechtigung erscheinen deaktiviert.
		matrix.setEnabled(administrator);
		mags.setEnabled(administrator);
		katalog.setEnabled(administrator);
		freigabe.setEnabled(administrator);
		administration.addItem(katalog, freigabe, absicherung, matrix, mags);

		addToNavbar(new DrawerToggle(), title, abmelden);
		addToDrawer(einladungen, schulung, administration);
	}

}
