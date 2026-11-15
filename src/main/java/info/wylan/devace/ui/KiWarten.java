package info.wylan.devace.ui;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * ADR-005: Ein Aufruf des KI-Modells läuft im Hintergrund, die Ansicht zeigt bis dahin den Wartehinweis (NFR-012) und
 * erhält Ergebnis oder Fehler per Server-Push. Der Aufruf behält die Anmeldung des Benutzers, damit die Berechtigungen
 * des Service gelten. Ist die Ansicht beim Ende des Aufrufs geschlossen, wird das Ergebnis verworfen.
 */
@Component
public class KiWarten {

	private final TaskExecutor executor;

	public KiWarten(@Qualifier("applicationTaskExecutor") TaskExecutor executor) {
		this.executor = executor;
	}

	/**
	 * Führt den Aufruf im Hintergrund aus. {@code ergebnis} und {@code fehler} laufen danach in der Sitzung der Ansicht.
	 * Ein Fehler, den {@code fehler} nicht behandelt, wird dort erneut geworfen und erreicht die Fehlerbehandlung von
	 * Vaadin wie bei einem synchronen Aufruf.
	 */
	public <T> void ausfuehren(Supplier<T> aufruf, Consumer<T> ergebnis, Consumer<RuntimeException> fehler) {
		UI ui = UI.getCurrentOrThrow();
		SecurityContext anmeldung = SecurityContextHolder.getContext();
		this.executor.execute(() -> {
			SecurityContextHolder.setContext(anmeldung);
			try {
				T wert = aufruf.get();
				ui.accessLater(() -> ergebnis.accept(wert), null).run();
			}
			catch (RuntimeException ex) {
				ui.accessLater(() -> fehler.accept(ex), null).run();
			}
			finally {
				SecurityContextHolder.clearContext();
			}
		});
	}

	/** NFR-012: der Wartehinweis, der nennt, worauf gewartet wird; Screenreader lesen ihn vor (NFR-010). */
	public static Div wartehinweis(String text, String id) {
		Span kreis = new Span();
		kreis.addClassName("devace-warte-kreis");
		kreis.getElement().setAttribute("aria-hidden", "true");
		Div hinweis = new Div(kreis, new Span(text));
		hinweis.addClassName("devace-wartehinweis");
		hinweis.getElement().setAttribute("role", "status");
		hinweis.getElement().setAttribute("aria-live", "polite");
		hinweis.setId(id);
		return hinweis;
	}

}
