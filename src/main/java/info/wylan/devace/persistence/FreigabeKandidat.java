package info.wylan.devace.persistence;

import org.jspecify.annotations.Nullable;

/**
 * UC-007 step 2: eine Rolle mit Entwurf, mit der Version des Entwurfs und der bisher freigegebenen Version.
 */
public record FreigabeKandidat(long rolleId, String rolle, int entwurfVersion, @Nullable Integer freigegebeneVersion) {
}
