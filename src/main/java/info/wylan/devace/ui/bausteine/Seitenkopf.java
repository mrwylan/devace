package info.wylan.devace.ui.bausteine;

import java.util.Optional;

import com.vaadin.flow.component.Component;

/**
 * ADR-006: Der Titel einer Ansicht steht wie im Mockup in der Kopfzeile des Seitenrahmens, nicht im Inhalt.
 */
public final class Seitenkopf {

	private Seitenkopf() {
	}

	/**
	 * Setzt den Titel der Ansicht in der Kopfzeile des Seitenrahmens, sobald die Ansicht darin hängt.
	 * @param ansicht die Ansicht oder eine Komponente darin
	 * @param titel der Titel
	 */
	public static void titel(Component ansicht, String titel) {
		Optional<Seitenrahmen> rahmen = rahmen(ansicht);
		if (rahmen.isPresent()) {
			rahmen.get().setzeTitel(titel);
		}
		else {
			ansicht.addAttachListener((event) -> {
				rahmen(ansicht).ifPresent((r) -> r.setzeTitel(titel));
				event.unregisterListener();
			});
		}
	}

	private static Optional<Seitenrahmen> rahmen(Component komponente) {
		Optional<Component> aktuell = Optional.of(komponente);
		while (aktuell.isPresent()) {
			if (aktuell.get() instanceof Seitenrahmen rahmen) {
				return Optional.of(rahmen);
			}
			aktuell = aktuell.get().getParent();
		}
		return Optional.empty();
	}

	/** Der Seitenrahmen, der den Titel der Ansicht zeigt. */
	public interface Seitenrahmen {

		/**
		 * @param titel der Titel der aktuellen Ansicht
		 */
		void setzeTitel(String titel);

	}

}
