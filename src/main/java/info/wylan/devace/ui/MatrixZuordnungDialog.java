package info.wylan.devace.ui;

import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import info.wylan.devace.persistence.Handlungsfeld;
import info.wylan.devace.persistence.MatrixZuordnungListItem;
import info.wylan.devace.service.MatrixZuordnungEingabe;
import info.wylan.devace.service.MatrixZuordnungService;
import info.wylan.devace.service.NotFoundException;
import info.wylan.devace.service.SpalteBereitsZugeordnetException;
import org.jspecify.annotations.Nullable;

/**
 * UC-010 steps 3–5: Spalte der Kompetenzmatrix, Handlungsfeld und Begründung einer Zuordnung erfassen.
 */
class MatrixZuordnungDialog extends Dialog {

	private final ComboBox<Handlungsfeld> handlungsfeld = new ComboBox<>("Handlungsfeld");

	private final TextField spalte = new TextField("Spalte der Kompetenzmatrix");

	private final TextArea begruendung = new TextArea("Begründung (optional)");

	private final Binder<Formular> binder = new Binder<>();

	private final Formular formular = new Formular();

	MatrixZuordnungDialog(MatrixZuordnungService service, List<Handlungsfeld> handlungsfelder,
			@Nullable MatrixZuordnungListItem vorhandene, Runnable nachSpeichern) {
		setHeaderTitle((vorhandene == null) ? "Zuordnung anlegen" : "Zuordnung ändern");
		setId("matrix-dialog");
		setWidth("36rem");

		this.handlungsfeld.setId("matrix-handlungsfeld");
		this.handlungsfeld.setItems(handlungsfelder);
		this.handlungsfeld.setItemLabelGenerator((feld) -> feld.nummer() + " " + feld.name());
		this.handlungsfeld.setRequiredIndicatorVisible(true);
		this.spalte.setId("matrix-spalte");
		this.spalte.setRequiredIndicatorVisible(true);
		this.spalte.setHelperText("Pflichtangabe, höchstens 200 Zeichen");
		this.spalte.setValueChangeMode(ValueChangeMode.EAGER);
		this.begruendung.setId("matrix-begruendung");
		this.begruendung.setHelperText("Höchstens 500 Zeichen");
		this.begruendung.setValueChangeMode(ValueChangeMode.EAGER);

		// UC-010 BR-001: Die Spalte gehört zu genau einem Handlungsfeld (OQ-096: Pflichtangabe).
		this.binder.forField(this.handlungsfeld)
			.asRequired("Pflichtangabe fehlt.")
			.bind(Formular::getHandlungsfeld, Formular::setHandlungsfeld);
		// UC-010 BR-001: Der Name der Spalte ist Pflichtangabe mit höchstens 200 Zeichen.
		this.binder.forField(this.spalte)
			.withValidator((wert) -> wert != null && !wert.isBlank(), "Pflichtangabe fehlt.")
			.withValidator((wert) -> wert.strip().length() <= 200, "Höchstens 200 Zeichen.")
			.bind(Formular::getSpalte, Formular::setSpalte);
		// UC-010 BR-001: Die Begründung ist optional mit höchstens 500 Zeichen.
		this.binder.forField(this.begruendung)
			.withValidator((wert) -> wert == null || wert.strip().length() <= 500, "Höchstens 500 Zeichen.")
			.bind(Formular::getBegruendung, Formular::setBegruendung);

		if (vorhandene != null) {
			handlungsfelder.stream()
				.filter((feld) -> feld.id() == vorhandene.handlungsfeldId())
				.findFirst()
				.ifPresent(this.formular::setHandlungsfeld);
			this.formular.setSpalte(vorhandene.matrixSpalte());
			this.formular.setBegruendung(vorhandene.begruendung());
		}
		this.binder.readBean(this.formular);

		FormLayout form = new FormLayout(this.spalte, this.handlungsfeld, this.begruendung);
		form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		add(form);

		Button speichern = new Button("Speichern", (event) -> speichern(service, vorhandene, nachSpeichern));
		speichern.addThemeVariants(ButtonVariant.PRIMARY);
		speichern.setId("matrix-speichern");
		// UC-010 A3: Abbrechen verwirft die Angaben.
		Button abbrechen = new Button("Abbrechen", (event) -> close());
		abbrechen.setId("matrix-abbrechen");
		getFooter().add(abbrechen, speichern);
	}

	private void speichern(MatrixZuordnungService service, @Nullable MatrixZuordnungListItem vorhandene,
			Runnable nachSpeichern) {
		if (!this.binder.writeBeanIfValid(this.formular)) {
			return;
		}
		Handlungsfeld feld = this.formular.getHandlungsfeld();
		MatrixZuordnungEingabe eingabe = new MatrixZuordnungEingabe((feld != null) ? feld.id() : null,
				this.formular.getSpalte(), this.formular.getBegruendung());
		try {
			if (vorhandene == null) {
				service.anlegen(eingabe);
			}
			else {
				service.aendern(vorhandene.id(), eingabe);
			}
			close();
			nachSpeichern.run();
		}
		catch (SpalteBereitsZugeordnetException ex) {
			// UC-010 A2: Hinweis auf die bestehende Zuordnung und ihr Handlungsfeld am Namen der Spalte.
			this.spalte.setInvalid(true);
			this.spalte.setErrorMessage(ex.getMessage());
		}
		catch (NotFoundException ex) {
			Notification notification = Notification.show(ex.getMessage());
			notification.addThemeVariants(NotificationVariant.ERROR);
			close();
			nachSpeichern.run();
		}
	}

	/** Formularwerte der Zuordnung. */
	static class Formular {

		private @Nullable Handlungsfeld handlungsfeld;

		private @Nullable String spalte = "";

		private @Nullable String begruendung = "";

		@Nullable Handlungsfeld getHandlungsfeld() {
			return this.handlungsfeld;
		}

		void setHandlungsfeld(@Nullable Handlungsfeld handlungsfeld) {
			this.handlungsfeld = handlungsfeld;
		}

		String getSpalte() {
			return (this.spalte != null) ? this.spalte : "";
		}

		void setSpalte(@Nullable String spalte) {
			this.spalte = spalte;
		}

		String getBegruendung() {
			return (this.begruendung != null) ? this.begruendung : "";
		}

		void setBegruendung(@Nullable String begruendung) {
			this.begruendung = begruendung;
		}

	}

}
