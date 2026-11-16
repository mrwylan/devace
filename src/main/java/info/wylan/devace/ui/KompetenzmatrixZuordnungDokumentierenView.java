package info.wylan.devace.ui;

import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import info.wylan.devace.persistence.Handlungsfeld;
import info.wylan.devace.persistence.MatrixZuordnungListItem;
import info.wylan.devace.service.MatrixZuordnungService;
import info.wylan.devace.service.NotFoundException;
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

	public KompetenzmatrixZuordnungDokumentierenView(MatrixZuordnungService service) {
		this.service = service;
		this.handlungsfelder = service.handlungsfelder();
		setWidthFull();

		H2 titel = new H2("Kompetenzmatrix-Zuordnung");
		titel.addClassName("devace-seitentitel");
		Span unter = new Span("Welche Spalten der Kompetenzmatrix welchem Handlungsfeld zugeordnet sind");
		unter.addClassName("devace-untertitel");
		Button anlegen = new Button("Zuordnung anlegen", (event) -> dialog(null));
		anlegen.addThemeVariants(ButtonVariant.PRIMARY);
		anlegen.setId("matrix-anlegen");
		// UC-010 BR-003: Die Kompetenzmatrix bleibt die Quelle; devace hält nur die Zuordnung fest.
		Span fussnote = new Span(
				"Die Kompetenzmatrix bleibt die fachliche Quelle. devace speichert nur den Namen der Spalte und ihre Zuordnung.");
		fussnote.addClassName("devace-fussnote");

		this.felder.setPadding(false);
		this.felder.setWidthFull();
		add(titel, unter, anlegen, this.felder, fussnote);
		aktualisieren();
	}

	/** UC-010 steps 2 and 5: die Zuordnungen je Handlungsfeld. */
	private void aktualisieren() {
		// die Zuordnungen sind fachlich begrenzt (Spalten einer Matrix), daher ohne Paging
		List<MatrixZuordnungListItem> zuordnungen = this.service.list();
		this.felder.removeAll();
		for (Handlungsfeld feld : this.handlungsfelder) {
			this.felder.add(karte(feld,
					zuordnungen.stream().filter((zuordnung) -> zuordnung.handlungsfeldId() == feld.id()).toList()));
		}
	}

	private VerticalLayout karte(Handlungsfeld feld, List<MatrixZuordnungListItem> zuordnungen) {
		Grid<MatrixZuordnungListItem> grid = new Grid<>();
		grid.addColumn(MatrixZuordnungListItem::matrixSpalte).setHeader("Spalte der Kompetenzmatrix").setAutoWidth(true);
		grid.addColumn((zuordnung) -> (zuordnung.begruendung() != null) ? zuordnung.begruendung() : "–")
			.setHeader("Begründung")
			.setFlexGrow(1);
		grid.addComponentColumn(this::aktionen).setHeader("").setAutoWidth(true).setFlexGrow(0);
		grid.addThemeVariants(GridVariant.NO_BORDER);
		grid.setId("matrix-grid-" + feld.nummer());
		grid.setEmptyStateText("Noch keine Spalte zugeordnet.");
		grid.setAllRowsVisible(true);
		grid.setItems(zuordnungen);

		H3 ueberschrift = new H3(feld.nummer() + " " + feld.name());
		VerticalLayout karte = new VerticalLayout(new Div(ueberschrift), grid);
		karte.addClassName("devace-karte");
		karte.setPadding(false);
		karte.getStyle().set("padding", "1rem 1.25rem");
		return karte;
	}

	private HorizontalLayout aktionen(MatrixZuordnungListItem zuordnung) {
		Button aendern = new Button("Ändern", (event) -> dialog(zuordnung));
		aendern.setId("matrix-aendern-" + zuordnung.id());
		aendern.setAriaLabel("Zuordnung " + zuordnung.matrixSpalte() + " ändern");
		Button entfernen = new Button("Entfernen", (event) -> entfernen(zuordnung));
		entfernen.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.ERROR);
		entfernen.setId("matrix-entfernen-" + zuordnung.id());
		entfernen.setAriaLabel("Zuordnung " + zuordnung.matrixSpalte() + " entfernen");
		return new HorizontalLayout(aendern, entfernen);
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
