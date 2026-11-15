package info.wylan.devace.ui;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.task.TaskExecutor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Ersetzt in den Browserless-Tests den {@code applicationTaskExecutor} (ADR-005). Ohne {@link #anhalten()} führt er
 * jeden Aufruf sofort im Test-Thread aus. Das Ergebnis erreicht die Ansicht dann mit dem nächsten {@code roundTrip()},
 * der die Befehle aus {@code UI.access} ausführt.
 *
 * <p>
 * Nach {@link #anhalten()} sammelt er die Aufrufe, bis {@link #fortsetzen()} sie ausführt. So lässt sich prüfen, was
 * die Ansicht zeigt, solange das KI-Modell noch arbeitet (NFR-012). Wie ein eigener Thread lässt er die Anmeldung des
 * Test-Threads unverändert.
 */
public class HintergrundExecutorFake implements TaskExecutor {

	private final List<Runnable> wartend = new ArrayList<>();

	private boolean angehalten;

	@Override
	public synchronized void execute(Runnable aufgabe) {
		if (this.angehalten) {
			this.wartend.add(aufgabe);
			return;
		}
		ausfuehren(aufgabe);
	}

	/** Sammelt die folgenden Aufrufe, statt sie auszuführen. */
	public synchronized void anhalten() {
		this.angehalten = true;
	}

	/** Führt die gesammelten Aufrufe aus und arbeitet danach wieder sofort. */
	public synchronized void fortsetzen() {
		this.angehalten = false;
		List<Runnable> aufgaben = List.copyOf(this.wartend);
		this.wartend.clear();
		aufgaben.forEach(HintergrundExecutorFake::ausfuehren);
	}

	/** Für {@code @BeforeEach}: sofort ausführen, nichts gesammelt. */
	public synchronized void reset() {
		this.angehalten = false;
		this.wartend.clear();
	}

	private static void ausfuehren(Runnable aufgabe) {
		SecurityContext anmeldung = SecurityContextHolder.getContext();
		try {
			aufgabe.run();
		}
		finally {
			SecurityContextHolder.setContext(anmeldung);
		}
	}

}
