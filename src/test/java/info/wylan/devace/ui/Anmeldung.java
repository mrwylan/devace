package info.wylan.devace.ui;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.AriaRole;
import org.vaadin.addons.dramafinder.element.ButtonElement;
import org.vaadin.addons.dramafinder.element.PasswordFieldElement;
import org.vaadin.addons.dramafinder.element.TextFieldElement;

/**
 * Anmeldung der Browser-Tests über die Anmeldeseite.
 */
final class Anmeldung {

	/**
	 * Der erste Aufruf einer frisch gestarteten Anwendung dauert auf dem CI-Runner länger als die 15 s, die Playwright
	 * sonst wartet; die Anmeldeseite bekommt deshalb mehr Zeit.
	 */
	private static final double ERSTER_AUFRUF_MS = 60_000;

	private Anmeldung() {
	}

	/** Meldet sich auf der Anmeldeseite an und wartet, bis sie verlassen ist. */
	static void anmelden(Page page, String benutzername, String passwort) {
		PlaywrightAssertions.assertThat(page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Benutzername")))
			.isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(ERSTER_AUFRUF_MS));
		TextFieldElement.getByLabel(page, "Benutzername").setValue(benutzername);
		PasswordFieldElement.getByLabel(page, "Passwort").setValue(passwort);
		ButtonElement.getByText(page, "Anmelden").click();
		page.waitForURL((url) -> !url.contains("login"));
	}

}
