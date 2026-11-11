package info.wylan.devace.persistence;

/**
 * Ein laufendes Assessment zu einer Einladung (UC-002, UC-003).
 */
public record LaufendesAssessment(long id, long magId, long rolleId, long fragenkatalogId) {
}
