package info.wylan.devace.persistence;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Eine Frage eines Fragenkatalogs mit ihrem Entscheidungsmodell. {@code vorgaben} enthält die Beschreibung je
 * Stufe; der Index ist die Stufe (0 bis 5). {@code frageId} ist die gespeicherte Frage, aus der sie stammt, oder leer
 * für eine neue Frage (UC-009 BR-010).
 */
public record KatalogFrage(long handlungsfeldId, String text, List<String> vorgaben, @Nullable Long frageId) {

	/** Eine neue Frage ohne gespeicherte Herkunft. */
	public KatalogFrage(long handlungsfeldId, String text, List<String> vorgaben) {
		this(handlungsfeldId, text, vorgaben, null);
	}

}
