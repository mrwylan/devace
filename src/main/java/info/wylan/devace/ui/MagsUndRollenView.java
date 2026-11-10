package info.wylan.devace.ui;

import java.util.stream.Collectors;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import info.wylan.devace.persistence.MagListItem;
import info.wylan.devace.persistence.RolleListItem;
import info.wylan.devace.service.MagService;
import info.wylan.devace.service.RolleService;
import jakarta.annotation.security.RolesAllowed;

/**
 * UC-008 MAGs und Rollen verwalten.
 */
@Route(value = "mags-und-rollen", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PageTitle("MAGs und Rollen verwalten")
// UC-008 GR-007: Nur der angemeldete Administrator verwaltet MAGs und Rollen.
@RolesAllowed("ADMINISTRATOR")
public class MagsUndRollenView extends VerticalLayout {

	private final MagService magService;

	private final RolleService rolleService;

	private final Grid<MagListItem> magGrid = new Grid<>();

	private final Grid<RolleListItem> rolleGrid = new Grid<>();

	public MagsUndRollenView(MagService magService, RolleService rolleService) {
		this.magService = magService;
		this.rolleService = rolleService;
		setWidthFull();

		H2 titel = new H2("MAGs und Rollen");
		titel.addClassName("devace-seitentitel");
		HorizontalLayout bereiche = new HorizontalLayout(magBereich(), rolleBereich());
		bereiche.setWidthFull();
		bereiche.setAlignItems(Alignment.START);
		add(titel, bereiche);
		aktualisieren();
	}

	private VerticalLayout magBereich() {
		this.magGrid.addColumn(MagListItem::scrumTeam).setHeader("Scrum Team").setAutoWidth(true);
		this.magGrid.addColumn(MagListItem::linienteam).setHeader("Linienteam").setAutoWidth(true);
		this.magGrid.addColumn((mag) -> (mag.teamleitung() != null) ? mag.teamleitung() : "–")
			.setHeader("Teamleitung")
			.setAutoWidth(true);
		this.magGrid.addComponentColumn((mag) -> {
			Button aendern = new Button("Ändern", (event) -> magDialog(mag));
			aendern.setAriaLabel("MAG " + mag.scrumTeam() + " " + mag.linienteam() + " ändern");
			return aendern;
		}).setHeader("").setAutoWidth(true).setFlexGrow(0);
		this.magGrid.addThemeVariants(GridVariant.NO_BORDER);
		this.magGrid.setId("mag-grid");
		this.magGrid.setEmptyStateText("Noch keine MAG angelegt.");
		this.magGrid.setAllRowsVisible(true);

		Button anlegen = new Button("MAG anlegen", (event) -> magDialog(null));
		anlegen.addThemeVariants(ButtonVariant.PRIMARY);
		anlegen.setId("mag-anlegen");
		// UC-008 GR-004: Eine MAG ohne Teamleitung ist zulässig; für sie kann niemand Einladungen erstellen.
		return karte("MAGs", "Mitarbeitergruppen aus Scrum Team und Linienteam", this.magGrid,
				"Eine MAG ohne Teamleitung ist zulässig; für sie kann niemand Einladungen erstellen.", anlegen, 3);
	}

	private VerticalLayout rolleBereich() {
		this.rolleGrid.addColumn(RolleListItem::name).setHeader("Name").setAutoWidth(true);
		this.rolleGrid.addColumn((rolle) -> fragenkataloge(rolle)).setHeader("Fragenkataloge").setAutoWidth(true);
		this.rolleGrid.addComponentColumn((rolle) -> {
			Button aendern = new Button("Ändern", (event) -> rolleDialog(rolle));
			aendern.setAriaLabel("Rolle " + rolle.name() + " ändern");
			return aendern;
		}).setHeader("").setAutoWidth(true).setFlexGrow(0);
		this.rolleGrid.addThemeVariants(GridVariant.NO_BORDER);
		this.rolleGrid.setId("rolle-grid");
		this.rolleGrid.setEmptyStateText("Noch keine Rolle angelegt.");
		this.rolleGrid.setAllRowsVisible(true);

		Button anlegen = new Button("Rolle anlegen", (event) -> rolleDialog(null));
		anlegen.addThemeVariants(ButtonVariant.PRIMARY);
		anlegen.setId("rolle-anlegen");
		// UC-008 GR-005: Der Use Case legt MAGs und Rollen an und ändert sie, er löscht sie nicht.
		return karte("Rollen", "Die Bewertung erfolgt getrennt je Rolle", this.rolleGrid,
				"MAGs und Rollen werden angelegt und geändert, aber nicht gelöscht.", anlegen, 2);
	}

	private static VerticalLayout karte(String titel, String untertitel, Grid<?> grid, String fussnote, Button anlegen,
			int breite) {
		H3 ueberschrift = new H3(titel);
		Span unter = new Span(untertitel);
		unter.addClassName("devace-untertitel");
		Span note = new Span(fussnote);
		note.addClassName("devace-fussnote");
		VerticalLayout karte = new VerticalLayout(new Div(ueberschrift, unter), grid, note, anlegen);
		karte.addClassName("devace-karte");
		karte.setPadding(false);
		karte.getStyle().set("flex", String.valueOf(breite));
		karte.getStyle().set("padding", "1rem 1.25rem");
		return karte;
	}

	private static String fragenkataloge(RolleListItem rolle) {
		if (rolle.fragenkataloge().isEmpty()) {
			return "–";
		}
		return rolle.fragenkataloge()
			.stream()
			.map((katalog) -> "Version " + katalog.version() + " (" + katalog.status() + ")")
			.collect(Collectors.joining(", "));
	}

	private void magDialog(MagListItem vorhandene) {
		new MagDialog(this.magService, this.magService.teamleitungen(), vorhandene, this::aktualisieren).open();
	}

	private void rolleDialog(RolleListItem vorhandene) {
		new RolleDialog(this.rolleService, vorhandene, this::aktualisieren).open();
	}

	/** UC-008 step 8: zeigt die aktualisierte Übersicht an. */
	private void aktualisieren() {
		// die Listen sind fachlich klein und begrenzt (Organisation der Teams), daher ohne Paging
		this.magGrid.setItems(this.magService.list());
		this.rolleGrid.setItems(this.rolleService.list());
	}

}
