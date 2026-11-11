package info.wylan.devace.ui;

import java.util.Locale;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.stereotype.Component;

/**
 * NFR-010: Alle Oberflächen sind deutsch; die Seite meldet ihre Sprache als {@code de-CH} (WCAG 3.1.1), und jede
 * Oberfläche formatiert Zahlen und Daten nach {@code de-CH}.
 */
@Component
class Sprache implements VaadinServiceInitListener {

	static final Locale LOCALE = Locale.forLanguageTag("de-CH");

	@Override
	public void serviceInit(ServiceInitEvent event) {
		event.addIndexHtmlRequestListener(
				(antwort) -> antwort.getDocument().select("html").attr("lang", LOCALE.toLanguageTag()));
		event.getSource().addUIInitListener((ui) -> ui.getUI().setLocale(LOCALE));
	}

}
