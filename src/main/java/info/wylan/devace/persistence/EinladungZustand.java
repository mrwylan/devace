package info.wylan.devace.persistence;

import java.time.LocalDate;

/**
 * Stand einer Einladung, wie UC-002 ihn zum Start prüft.
 */
public record EinladungZustand(long id, long magId, long rolleId, String status, LocalDate laeuftAbAm) {
}
