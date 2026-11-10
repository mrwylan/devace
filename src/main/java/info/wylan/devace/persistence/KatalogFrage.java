package info.wylan.devace.persistence;

import java.util.List;

/**
 * Eine Frage eines Fragenkatalogs mit ihrem Entscheidungsmodell. {@code vorgaben} enthält die Beschreibung je
 * Stufe; der Index ist die Stufe (0 bis 5).
 */
public record KatalogFrage(long handlungsfeldId, String text, List<String> vorgaben) {
}
