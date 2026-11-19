package info.wylan.devace.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.security.AuthenticationContext;
import info.wylan.devace.ui.bausteine.Seitenkopf;
import jakarta.annotation.security.PermitAll;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Rahmen der Anwendung wie im Figma-Mockup (ADR-006): Marke und Navigation links, Profil mit Abmelden unten in der
 * Navigation, Titel der Ansicht oben, Fußzeile unten.
 */
@PermitAll
public class MainLayout extends AppLayout implements Seitenkopf.Seitenrahmen, AfterNavigationObserver {

	private static final String ROLLE_ADMINISTRATOR = "ROLE_ADMINISTRATOR";

	private static final String ROLLE_TEAMLEITUNG = "ROLE_TEAMLEITUNG";

	private static final String ROLLE_SCHULUNGSVERANTWORTLICHE = "ROLE_SCHULUNGSVERANTWORTLICHE";

	private static final Map<String, String> BERECHTIGUNGEN = Map.of(ROLLE_ADMINISTRATOR, "Administrator",
			ROLLE_TEAMLEITUNG, "Teamleitung", ROLLE_SCHULUNGSVERANTWORTLICHE, "Schulungsverantwortliche");

	private final H1 titel = new H1();

	private final Div inhalt = new Div();

	private final List<SideNavItem> eintraege = new ArrayList<>();

	private @Nullable Component ansicht;

	public MainLayout(AuthenticationContext authenticationContext) {
		List<String> rechte = authenticationContext.getAuthenticatedUser(UserDetails.class)
			.map((benutzer) -> benutzer.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
			.orElse(List.of());
		boolean administrator = rechte.contains(ROLLE_ADMINISTRATOR);
		boolean teamleitung = rechte.contains(ROLLE_TEAMLEITUNG);
		boolean schulungsverantwortliche = rechte.contains(ROLLE_SCHULUNGSVERANTWORTLICHE);

		// Die Einträge sind für alle sichtbar; ohne Berechtigung sind sie deaktiviert.
		SideNavItem auswertung = eintrag("Radar-Chart", RadarChartEinerMagAuswertenView.class, VaadinIcon.PIE_CHART);
		// UC-004 BR-007: Einträge ohne passende Berechtigung erscheinen deaktiviert.
		auswertung.setEnabled(teamleitung);
		SideNavItem schulungsplanung = eintrag("Schulungsplanung", RadarChartsFuerDieSchulungsplanungAuswertenView.class,
				VaadinIcon.BAR_CHART);
		// UC-005 BR-007: Einträge ohne passende Berechtigung erscheinen deaktiviert.
		schulungsplanung.setEnabled(schulungsverantwortliche);
		SideNavItem einladung = eintrag("Einladungen", EinladungErstellenUndVersendenView.class, VaadinIcon.PAPERPLANE);
		// UC-001 BR-009: Einträge ohne passende Berechtigung erscheinen deaktiviert.
		einladung.setEnabled(teamleitung);
		SideNavItem katalog = eintrag("Fragenkatalog", FragenkatalogUndEntscheidungsmodellErstellenView.class,
				VaadinIcon.LIST_OL);
		SideNavItem freigabe = eintrag("Freigabe", FragenkatalogFreigebenView.class, VaadinIcon.CHECK_SQUARE_O);
		SideNavItem absicherung = eintrag("KI-Absicherung", KiBewertungMitTestfaellenAbsichernView.class,
				VaadinIcon.FLASK);
		SideNavItem matrix = eintrag("Kompetenzmatrix", KompetenzmatrixZuordnungDokumentierenView.class,
				VaadinIcon.TABLE);
		SideNavItem mags = eintrag("MAGs und Rollen", MagsUndRollenView.class, VaadinIcon.USER);
		// UC-006 BR-009, UC-007 BR-007, UC-008 BR-007: Einträge ohne Berechtigung erscheinen deaktiviert.
		// UC-009 BR-011: Einträge ohne passende Berechtigung erscheinen deaktiviert.
		absicherung.setEnabled(administrator);
		// UC-010 BR-004: Einträge ohne passende Berechtigung erscheinen deaktiviert.
		matrix.setEnabled(administrator);
		mags.setEnabled(administrator);
		katalog.setEnabled(administrator);
		freigabe.setEnabled(administrator);

		SideNav gruppeAuswertung = gruppe("Auswertung", auswertung, schulungsplanung);
		SideNav gruppeEinladungen = gruppe("Einladungen", einladung);
		SideNav gruppeAdministration = gruppe("Administration", katalog, freigabe, absicherung, matrix, mags);
		Div navigation = new Div(gruppeAuswertung, gruppeEinladungen, gruppeAdministration);
		navigation.addClassName("devace-navigation");

		Span zeichen = new Span(">_");
		zeichen.addClassName("devace-marke-zeichen");
		Div marke = new Div(zeichen, new Span("devace"));
		marke.addClassName("devace-marke");

		String benutzername = authenticationContext.getPrincipalName().orElse("");
		String berechtigung = rechte.stream().map(BERECHTIGUNGEN::get).filter((b) -> b != null).findFirst().orElse("");
		Div profil = profil(benutzername, berechtigung, authenticationContext);

		Div drawer = new Div(marke, navigation, profil);
		drawer.addClassName("devace-drawer");
		addToDrawer(drawer);

		this.titel.addClassName("devace-navbar-titel");
		addToNavbar(new DrawerToggle(), this.titel);
		setPrimarySection(Section.DRAWER);

		this.inhalt.addClassName("devace-inhalt");
		Div seite = new Div(this.inhalt, fusszeile());
		seite.addClassName("devace-seite");
		setContent(seite);
	}

	@Override
	public void showRouterLayoutContent(HasElement content) {
		this.ansicht = content.getElement().getComponent().orElse(null);
		this.inhalt.removeAll();
		if (this.ansicht != null) {
			this.inhalt.add(this.ansicht);
		}
	}

	@Override
	public void removeRouterLayoutContent(@Nullable HasElement oldContent) {
		this.inhalt.removeAll();
		this.ansicht = null;
	}

	@Override
	public void afterNavigation(AfterNavigationEvent event) {
		String pfad = event.getLocation().getPath();
		String text = this.eintraege.stream()
			.filter((eintrag) -> pfad.equals(eintrag.getPath()))
			.map(SideNavItem::getLabel)
			.findFirst()
			.orElseGet(this::seitentitel);
		setzeTitel(text);
	}

	@Override
	public void setzeTitel(String text) {
		this.titel.setText(text);
	}

	private String seitentitel() {
		PageTitle titel = (this.ansicht != null) ? this.ansicht.getClass().getAnnotation(PageTitle.class) : null;
		return (titel != null) ? titel.value() : "devace";
	}

	private SideNavItem eintrag(String text, Class<? extends Component> ansicht, VaadinIcon symbol) {
		SideNavItem eintrag = new SideNavItem(text, ansicht, symbol.create());
		this.eintraege.add(eintrag);
		return eintrag;
	}

	private static SideNav gruppe(String text, SideNavItem... eintraege) {
		SideNav gruppe = new SideNav(text);
		gruppe.addClassName("devace-navgruppe");
		gruppe.addItem(eintraege);
		return gruppe;
	}

	/** Profil unten in der Navigation; das Menü enthält nur Abmelden (Profil und Einstellungen sind nicht spezifiziert). */
	private static Div profil(String benutzername, String berechtigung, AuthenticationContext authenticationContext) {
		Avatar avatar = new Avatar(benutzername);
		Span name = new Span(benutzername);
		name.addClassName("devace-profil-name");
		Span rolle = new Span(berechtigung);
		rolle.addClassName("devace-profil-rolle");
		Div text = new Div(name, rolle);
		text.addClassName("devace-profil-text");

		Button menue = new Button(VaadinIcon.CHEVRON_UP_SMALL.create());
		menue.addThemeVariants(ButtonVariant.TERTIARY);
		menue.setAriaLabel("Profilmenü öffnen");
		menue.setTooltipText("Profilmenü");
		ContextMenu auswahl = new ContextMenu(menue);
		auswahl.setOpenOnClick(true);
		MenuItem abmelden = auswahl.addItem("Abmelden", (event) -> authenticationContext.logout());
		abmelden.setId("abmelden");

		Div profil = new Div(avatar, text, menue);
		profil.addClassName("devace-profil");
		return profil;
	}

	private static Footer fusszeile() {
		Span produkt = new Span("devace · Kompetenz-Assessment · Zielbild 2030");
		produkt.addClassName("devace-fusszeile-text");
		RouterLink datenschutz = new RouterLink("Datenschutz", DatenschutzView.class);
		RouterLink kiHinweise = new RouterLink("Hinweise zur KI-Nutzung", KiHinweiseView.class);
		Div links = new Div(datenschutz, kiHinweise);
		links.addClassName("devace-fusszeile-links");
		Footer fusszeile = new Footer(produkt, links);
		fusszeile.addClassName("devace-fusszeile");
		return fusszeile;
	}

}
