package info.wylan.devace.ui;

import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import info.wylan.devace.persistence.MagListItem;
import info.wylan.devace.persistence.TeamleitungOption;
import info.wylan.devace.service.MagBereitsVorhandenException;
import info.wylan.devace.service.MagEingabe;
import info.wylan.devace.service.MagService;
import info.wylan.devace.service.NotFoundException;
import org.jspecify.annotations.Nullable;

/**
 * UC-008 steps 3–5: Angaben einer MAG erfassen, um sie anzulegen oder zu ändern.
 */
class MagDialog extends Dialog {

	private final TextField scrumTeam = new TextField("Scrum Team");

	private final TextField linienteam = new TextField("Linienteam");

	private final ComboBox<TeamleitungOption> teamleitung = new ComboBox<>("Teamleitung");

	private final Binder<Formular> binder = new Binder<>();

	private final Formular formular = new Formular();

	MagDialog(MagService service, List<TeamleitungOption> teamleitungen, @Nullable MagListItem vorhandene,
			Runnable nachSpeichern) {
		setHeaderTitle((vorhandene == null) ? "MAG anlegen" : "MAG ändern");
		setId("mag-dialog");

		this.scrumTeam.setId("mag-scrum-team");
		this.linienteam.setId("mag-linienteam");
		this.teamleitung.setId("mag-teamleitung");
		this.scrumTeam.setRequiredIndicatorVisible(true);
		this.linienteam.setRequiredIndicatorVisible(true);
		this.scrumTeam.setValueChangeMode(ValueChangeMode.EAGER);
		this.linienteam.setValueChangeMode(ValueChangeMode.EAGER);
		this.teamleitung.setItems(teamleitungen);
		this.teamleitung.setItemLabelGenerator(TeamleitungOption::benutzername);
		this.teamleitung.setClearButtonVisible(true);

		// UC-008 GR-001: Scrum Team ist Pflichtangabe mit höchstens 100 Zeichen.
		this.binder.forField(this.scrumTeam)
			.withValidator(wert -> wert != null && !wert.isBlank(), "Pflichtangabe fehlt.")
			.withValidator(wert -> wert.strip().length() <= 100, "Höchstens 100 Zeichen.")
			.bind(Formular::getScrumTeam, Formular::setScrumTeam);
		// UC-008 GR-001: Linienteam ist Pflichtangabe mit höchstens 100 Zeichen.
		this.binder.forField(this.linienteam)
			.withValidator(wert -> wert != null && !wert.isBlank(), "Pflichtangabe fehlt.")
			.withValidator(wert -> wert.strip().length() <= 100, "Höchstens 100 Zeichen.")
			.bind(Formular::getLinienteam, Formular::setLinienteam);
		this.binder.forField(this.teamleitung).bind(Formular::getTeamleitung, Formular::setTeamleitung);

		if (vorhandene != null) {
			this.formular.setScrumTeam(vorhandene.scrumTeam());
			this.formular.setLinienteam(vorhandene.linienteam());
			teamleitungen.stream()
				.filter((option) -> Long.valueOf(option.id()).equals(vorhandene.teamleitungId()))
				.findFirst()
				.ifPresent(this.formular::setTeamleitung);
		}
		this.binder.readBean(this.formular);

		FormLayout form = new FormLayout(this.scrumTeam, this.linienteam, this.teamleitung);
		form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		add(form);

		Button speichern = new Button("Speichern", event -> speichern(service, vorhandene, nachSpeichern));
		speichern.addThemeVariants(ButtonVariant.PRIMARY);
		speichern.setId("mag-speichern");
		Button abbrechen = new Button("Abbrechen", event -> close());
		abbrechen.setId("mag-abbrechen");
		getFooter().add(abbrechen, speichern);
	}

	private void speichern(MagService service, @Nullable MagListItem vorhandene, Runnable nachSpeichern) {
		if (!this.binder.writeBeanIfValid(this.formular)) {
			return;
		}
		TeamleitungOption gewaehlt = this.formular.getTeamleitung();
		MagEingabe eingabe = new MagEingabe(this.formular.getScrumTeam(), this.formular.getLinienteam(),
				(gewaehlt != null) ? gewaehlt.id() : null);
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
		catch (MagBereitsVorhandenException ex) {
			this.linienteam.setInvalid(true);
			this.linienteam.setErrorMessage(ex.getMessage());
		}
		catch (NotFoundException ex) {
			Notification notification = Notification.show(ex.getMessage());
			notification.addThemeVariants(NotificationVariant.ERROR);
			close();
			nachSpeichern.run();
		}
	}

	/** Formularwerte der MAG. */
	static class Formular {

		private @Nullable String scrumTeam = "";

		private @Nullable String linienteam = "";

		private @Nullable TeamleitungOption teamleitung;

		String getScrumTeam() {
			return (this.scrumTeam != null) ? this.scrumTeam : "";
		}

		void setScrumTeam(@Nullable String scrumTeam) {
			this.scrumTeam = scrumTeam;
		}

		String getLinienteam() {
			return (this.linienteam != null) ? this.linienteam : "";
		}

		void setLinienteam(@Nullable String linienteam) {
			this.linienteam = linienteam;
		}

		@Nullable TeamleitungOption getTeamleitung() {
			return this.teamleitung;
		}

		void setTeamleitung(@Nullable TeamleitungOption teamleitung) {
			this.teamleitung = teamleitung;
		}

	}

}
