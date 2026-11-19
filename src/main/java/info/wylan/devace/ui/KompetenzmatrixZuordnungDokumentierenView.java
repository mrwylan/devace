package info.wylan.devace.ui;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import info.wylan.devace.persistence.Handlungsfeld;
import info.wylan.devace.persistence.MatrixZuordnungListItem;
import info.wylan.devace.service.MatrixZuordnungService;
import info.wylan.devace.service.NotFoundException;
import info.wylan.devace.ui.bausteine.DevaceListe;
import info.wylan.devace.ui.bausteine.DevaceListe.Aktion;
import info.wylan.devace.ui.bausteine.Karte;
import jakarta.annotation.security.RolesAllowed;
import org.jspecify.annotations.Nullable;

/**
 * UC-010 Kompetenzmatrix-Zuordnung dokumentieren.
 */
@Route(value = "kompetenzmatrix", layout = MainLayout.class)
@PageTitle("Kompetenzmatrix-Zuordnung dokumentieren")
// UC-010 BR-004: Nur der angemeldete Administrator dokumentiert die Zuordnung.
@RolesAllowed("ADMINISTRATOR")
public class KompetenzmatrixZuordnungDokumentierenView extends VerticalLayout {

	private final MatrixZuordnungService service;

	private final List<Handlungsfeld> handlungsfelder;

	private final VerticalLayout felder = new VerticalLayout();

	private final List<DevaceListe<MatrixZuordnungListItem>> listen = new ArrayList<>();

	private final TextField suche = new TextField();

	public KompetenzmatrixZuordnungDokumentierenView(MatrixZuordnungService service) {
		this.service = service;
		this.handlungsfelder = service.handlungsfelder();
		setWidthFull();

		Span unter = new Span("Welche Spalten der Kompetenzmatrix welchem Handlungsfeld zugeordnet sind");
		unter.addClassName("devace-untertitel");
		// ADR-006: ein Suchfeld für alle sechs Handlungsfelder, die Hauptaktion rechts oben
		this.suche.setId("matrix-suche");
		this.suche.setPlaceholder("Suchen …");
		this.suche.setAriaLabel("Zuordnungen durchsuchen");
		this.suche.setPrefixComponent(VaadinIcon.SEARCH.create());
		this.suche.setClearButtonVisible(true);
		this.suche.setValueChangeMode(ValueChangeMode.LAZY);
		this.suche.addClassName("devace-liste-suche");
		this.suche.addValueChangeListener((event) -> this.listen.forEach((liste) -> liste.suchen(event.getValue())));
		Button anlegen = new Button("Zuordnung anlegen", VaadinIcon.PLUS.create(), (event) -> dialog(null));
		anlegen.addThemeVariants(ButtonVariant.PRIMARY);
		anlegen.setId("matrix-anlegen");
		HorizontalLayout kopf = new HorizontalLayout(this.suche, anlegen);
		kopf.setWidthFull();
		kopf.setAlignItems(Alignment.CENTER);
		kopf.setJustifyContentMode(JustifyContentMode.BETWEEN);
		// UC-010 BR-003: Die Kompetenzmatrix bleibt die Quelle; devace hält nur die Zuordnung fest.
		Span fussnote = new Span(
				"Die Kompetenzmatrix bleibt die fachliche Quelle. devace speichert nur den Namen der Spalte und ihre Zuordnung.");
		fussnote.addClassName("devace-fussnote");

		this.felder.setPadding(false);
		this.felder.setWidthFull();
		add(unter, kopf, this.felder, fussnote);
		aktualisieren();
	}

	/** UC-010 steps 2 and 5: die Zuordnungen je Handlungsfeld. */
	private void aktualisieren() {
		// die Zuordnungen sind fachlich begrenzt (Spalten einer Matrix), daher ohne Paging
		List<MatrixZuordnungListItem> zuordnungen = this.service.list();
		this.felder.removeAll();
		this.listen.clear();
		for (Handlungsfeld feld : this.handlungsfelder) {
			this.felder.add(karte(feld,
					zuordnungen.stream().filter((zuordnung) -> zuordnung.handlungsfeldId() == feld.id()).toList()));
		}
	}

	private Karte karte(Handlungsfeld feld, List<MatrixZuordnungListItem> zuordnungen) {
		DevaceListe<MatrixZuordnungListItem> liste = new DevaceListe<>("matrix-grid-" + feld.nummer(),
				"Noch keine Spalte zugeordnet.");
		// ADR-006: feste Spaltenbreiten, damit die sechs Tabellen untereinander bündig sind
		liste.spalte("Spalte der Kompetenzmatrix", MatrixZuordnungListItem::matrixSpalte).setWidth("22rem").setFlexGrow(0);
		liste.spalte("Begründung", (zuordnung) -> (zuordnung.begruendung() != null) ? zuordnung.begruendung() : "–")
			.setFlexGrow(1);
		liste.aktionen(
				Aktion.von(VaadinIcon.EDIT, "Ändern",
						(MatrixZuordnungListItem zuordnung) -> "Zuordnung " + zuordnung.matrixSpalte() + " ändern",
						this::dialog)
					.mitId((zuordnung) -> "matrix-aendern-" + zuordnung.id()),
				Aktion.entfernen(
						(MatrixZuordnungListItem zuordnung) -> "Zuordnung " + zuordnung.matrixSpalte() + " entfernen",
						this::entfernen)
					.mitId((zuordnung) -> "matrix-entfernen-" + zuordnung.id()))
			.setWidth("6rem");
		liste.alleZeilen().ohneSuche();
		liste.setItems(zuordnungen);
		liste.suchen(this.suche.getValue());
		this.listen.add(liste);

		Karte karte = new Karte(feld.nummer() + " " + feld.name(), "");
		karte.add(liste);
		return karte;
	}

	private void dialog(@Nullable MatrixZuordnungListItem vorhandene) {
		new MatrixZuordnungDialog(this.service, this.handlungsfelder, vorhandene, this::aktualisieren).open();
	}

	/** UC-010 steps 3–5 (OQ-095): das Entfernen wird bestätigt; Abbrechen ist A3 und behält die Zuordnung. */
	private void entfernen(MatrixZuordnungListItem zuordnung) {
		ConfirmDialog bestaetigung = new ConfirmDialog();
		bestaetigung.setId("matrix-entfernen-dialog");
		bestaetigung.setHeader("Zuordnung entfernen");
		bestaetigung.setText("Die Zuordnung der Spalte „" + zuordnung.matrixSpalte() + "“ wird entfernt.");
		bestaetigung.setCancelable(true);
		bestaetigung.setCancelText("Abbrechen");
		bestaetigung.setConfirmText("Entfernen");
		bestaetigung.setConfirmButtonTheme("error primary");
		bestaetigung.addConfirmListener((event) -> {
			try {
				this.service.entfernen(zuordnung.id());
			}
			catch (NotFoundException ex) {
				Notification notification = Notification.show(ex.getMessage());
				notification.addThemeVariants(NotificationVariant.ERROR);
			}
			aktualisieren();
		});
		bestaetigung.open();
	}

}
