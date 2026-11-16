package info.wylan.devace.persistence;

import org.jspecify.annotations.Nullable;

/**
 * UC-010 BR-001: Zuordnung einer Spalte der Kompetenzmatrix zu einem Handlungsfeld, wie sie gespeichert wird.
 */
public record MatrixZuordnung(@Nullable Long id, long handlungsfeldId, String matrixSpalte,
		@Nullable String begruendung) {
}
