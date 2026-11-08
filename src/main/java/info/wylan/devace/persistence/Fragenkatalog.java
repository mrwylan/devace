package info.wylan.devace.persistence;

import java.util.List;

/**
 * Ein Fragenkatalog einer Rolle mit seinen Fragen in der Reihenfolge ihrer Position.
 */
public record Fragenkatalog(long id, long rolleId, int version, String status, List<KatalogFrage> fragen) {
}
