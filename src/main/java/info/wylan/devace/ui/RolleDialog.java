package info.wylan.devace.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import info.wylan.devace.persistence.RolleListItem;
import info.wylan.devace.service.NotFoundException;
import info.wylan.devace.service.RolleBereitsVorhandenException;
import info.wylan.devace.service.RolleEingabe;
import info.wylan.devace.service.RolleService;
import org.jspecify.annotations.Nullable;

/**
 * UC-008 steps 3–5: Name einer Rolle erfassen, um sie anzulegen oder zu ändern.
 */
class RolleDialog extends Dialog {

	private final TextField name = new TextField("Name der Rolle");

	private final Binder<Formular> binder = new Binder<>();

	private final Formular formular = new Formular();

	RolleDialog(RolleService service, @Nullable RolleListItem vorhandene, Runnable nachSpeichern) {
		setHeaderTitle((vorhandene == null) ? "Rolle anlegen" : "Rolle ändern");
		setId("rolle-dialog");
		setWidth("34rem");

		this.name.setId("rolle-name");
		this.name.setRequiredIndicatorVisible(true);
		this.name.setHelperText("Pflichtangabe, höchstens 50 Zeichen");
		this.name.setValueChangeMode(ValueChangeMode.EAGER);

		// UC-008 BR-001: Der Name einer Rolle ist Pflichtangabe mit höchstens 50 Zeichen.
		this.binder.forField(this.name)
			.withValidator(wert -> wert != null && !wert.isBlank(), "Pflichtangabe fehlt.")
			.withValidator(wert -> wert.strip().length() <= 50, "Höchstens 50 Zeichen.")
			.bind(Formular::getName, Formular::setName);

		if (vorhandene != null) {
			this.formular.setName(vorhandene.name());
		}
		this.binder.readBean(this.formular);

		FormLayout form = new FormLayout(this.name);
		form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		add(form);
		if (vorhandene != null) {
			// UC-008 BR-006: Die Änderung des Namens wirkt rückwirkend auf Einladungen, Fragenkataloge und Scores.
			Div hinweis = new Div(hinweisTitel("Änderung wirkt rückwirkend"),
					new Span("Bestehende Einladungen, Fragenkataloge und Scores dieser Rolle erscheinen mit dem neuen Namen."));
			hinweis.addClassName("devace-hinweis");
			add(hinweis);
		}

		Button speichern = new Button("Speichern", event -> speichern(service, vorhandene, nachSpeichern));
		speichern.addThemeVariants(ButtonVariant.PRIMARY);
		speichern.setId("rolle-speichern");
		Button abbrechen = new Button("Abbrechen", event -> close());
		abbrechen.setId("rolle-abbrechen");
		getFooter().add(abbrechen, speichern);
	}

	private static Span hinweisTitel(String text) {
		Span titel = new Span(text);
		titel.addClassName("devace-hinweis-titel");
		return titel;
	}

	private void speichern(RolleService service, @Nullable RolleListItem vorhandene, Runnable nachSpeichern) {
		if (!this.binder.writeBeanIfValid(this.formular)) {
			return;
		}
		RolleEingabe eingabe = new RolleEingabe(this.formular.getName());
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
		catch (RolleBereitsVorhandenException ex) {
			this.name.setInvalid(true);
			this.name.setErrorMessage(ex.getMessage());
		}
		catch (NotFoundException ex) {
			Notification notification = Notification.show(ex.getMessage());
			notification.addThemeVariants(NotificationVariant.ERROR);
			close();
			nachSpeichern.run();
		}
	}

	/** Formularwerte der Rolle. */
	static class Formular {

		private @Nullable String name = "";

		String getName() {
			return (this.name != null) ? this.name : "";
		}

		void setName(@Nullable String name) {
			this.name = name;
		}

	}

}
