package info.wylan.devace.ui.bausteine;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import org.jspecify.annotations.Nullable;

/**
 * ADR-006: Hinweisfeld mit Titel und Text, optional mit Thema „fehler“, „erfolg“ oder „warnung“.
 */
public final class Hinweis {

	private Hinweis() {
	}

	/**
	 * @param titel der fett gesetzte Titel
	 * @param text der Text
	 * @param thema „fehler“, „erfolg“, „warnung“ oder {@code null} für neutral
	 * @param id die Id des Feldes oder {@code null}
	 * @return das Hinweisfeld
	 */
	public static Div meldung(String titel, String text, @Nullable String thema, @Nullable String id) {
		Span kopf = new Span(titel);
		kopf.addClassName("devace-hinweis-titel");
		Div box = new Div(kopf, new Span(text));
		box.addClassName("devace-hinweis");
		if (thema != null) {
			box.getElement().setAttribute("theme", thema);
		}
		if (id != null) {
			box.setId(id);
		}
		return box;
	}

}
