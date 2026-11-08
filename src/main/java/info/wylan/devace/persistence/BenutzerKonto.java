package info.wylan.devace.persistence;

/**
 * Benutzerkonto für die Anmeldung.
 */
public record BenutzerKonto(long id, String benutzername, String passwortHash, Berechtigung berechtigung) {
}
