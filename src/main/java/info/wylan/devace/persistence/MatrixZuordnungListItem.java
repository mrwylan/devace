package info.wylan.devace.persistence;

import org.jspecify.annotations.Nullable;

/**
 * UC-010 step 2: eine vorhandene Zuordnung mit ihrem Handlungsfeld.
 */
public record MatrixZuordnungListItem(long id, long handlungsfeldId, int handlungsfeldNummer,
		String handlungsfeldName, String matrixSpalte, @Nullable String begruendung) {
}
