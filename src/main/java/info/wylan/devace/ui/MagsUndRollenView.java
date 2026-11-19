package info.wylan.devace.ui;

import java.util.stream.Collectors;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import info.wylan.devace.persistence.MagListItem;
import info.wylan.devace.persistence.RolleListItem;
import info.wylan.devace.service.MagService;
import info.wylan.devace.service.RolleService;
import info.wylan.devace.ui.bausteine.DevaceListe;
import info.wylan.devace.ui.bausteine.DevaceListe.Aktion;
import info.wylan.devace.ui.bausteine.Karte;
import jakarta.annotation.security.RolesAllowed;

/**
 * UC-008 MAGs und Rollen verwalten.
 */
@Route(value = "mags-und-rollen", layout = MainLayout.class)
@PageTitle("MAGs und Rollen verwalten")
// UC-008 BR-007: Nur der angemeldete Administrator verwaltet MAGs und Rollen.
@RolesAllowed("ADMINISTRATOR")
public class MagsUndRollenView extends VerticalLayout {

	private final MagService magService;

	private final RolleService rolleService;

	private final DevaceListe<MagListItem> magListe = new DevaceListe<>("mag-grid", "Noch keine MAG angelegt.");

	private final DevaceListe<RolleListItem> rolleListe = new DevaceListe<>("rolle-grid", "Noch keine Rolle angelegt.");

	public MagsUndRollenView(MagService magService, RolleService rolleService) {
		this.magService = magService;
		this.rolleService = rolleService;
		setWidthFull();
		addClassName("devace-ansicht-fuellend");

		Karte mags = magKarte();
		Karte rollen = rolleKarte();
		HorizontalLayout bereiche = new HorizontalLayout(mags, rollen);
		bereiche.addClassName("devace-spalten");
		bereiche.setFlexGrow(3, mags);
		bereiche.setFlexGrow(2, rollen);
		add(bereiche);
		setFlexGrow(1, bereiche);
		aktualisieren();
	}

	private Karte magKarte() {
		this.magListe.spalte("Scrum Team", MagListItem::scrumTeam).setAutoWidth(true);
		this.magListe.spalte("Linienteam", MagListItem::linienteam).setAutoWidth(true);
		this.magListe.spalte("Teamleitung", (mag) -> (mag.teamleitung() != null) ? mag.teamleitung() : "–")
			.setAutoWidth(true);
		this.magListe.aktionen(Aktion.von(VaadinIcon.EDIT, "Ändern",
				(MagListItem mag) -> "MAG " + mag.scrumTeam() + " " + mag.linienteam() + " ändern", this::magDialog));

		Button anlegen = new Button("MAG anlegen", VaadinIcon.PLUS.create(), (event) -> magDialog(null));
		anlegen.addThemeVariants(ButtonVariant.PRIMARY);
		anlegen.setId("mag-anlegen");
		// UC-008 BR-004: Eine MAG ohne Teamleitung ist zulässig; für sie kann niemand Einladungen erstellen.
		return new Karte("MAGs", "Mitarbeitergruppen aus Scrum Team und Linienteam").aktion(anlegen)
			.fuellen(this.magListe)
			.fussnote("Eine MAG ohne Teamleitung ist zulässig; für sie kann niemand Einladungen erstellen.");
	}

	private Karte rolleKarte() {
		this.rolleListe.spalte("Name", RolleListItem::name).setAutoWidth(true);
		this.rolleListe.spalte("Fragenkataloge", MagsUndRollenView::fragenkataloge).setAutoWidth(true);
		this.rolleListe.aktionen(Aktion.von(VaadinIcon.EDIT, "Ändern",
				(RolleListItem rolle) -> "Rolle " + rolle.name() + " ändern", this::rolleDialog));

		Button anlegen = new Button("Rolle anlegen", VaadinIcon.PLUS.create(), (event) -> rolleDialog(null));
		anlegen.addThemeVariants(ButtonVariant.PRIMARY);
		anlegen.setId("rolle-anlegen");
		// UC-008 BR-005: Der Use Case legt MAGs und Rollen an und ändert sie, er löscht sie nicht.
		return new Karte("Rollen", "Die Bewertung erfolgt getrennt je Rolle").aktion(anlegen)
			.fuellen(this.rolleListe)
			.fussnote("MAGs und Rollen werden angelegt und geändert, aber nicht gelöscht.");
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
		this.magListe.setItems(this.magService.list());
		this.rolleListe.setItems(this.rolleService.list());
	}

}
