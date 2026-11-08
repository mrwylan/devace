package info.wylan.devace.persistence;

import java.util.List;

/**
 * UC-008 step 2: eine Rolle der Übersicht mit ihren Fragenkatalogen, neueste Version zuerst.
 */
public record RolleListItem(long id, String name, List<KatalogUebersicht> fragenkataloge) {
}
