package info.wylan.devace.persistence;

/**
 * Funktion eines Benutzerkontos (OQ-031). Gespeichert wird die Bezeichnung.
 */
public enum Berechtigung {

	TEAMLEITUNG("Teamleitung"), SCHULUNGSVERANTWORTLICHE("Schulungsverantwortliche"),
	ADMINISTRATOR("Administrator");

	private final String bezeichnung;

	Berechtigung(String bezeichnung) {
		this.bezeichnung = bezeichnung;
	}

	public String bezeichnung() {
		return this.bezeichnung;
	}

	public static Berechtigung ausBezeichnung(String bezeichnung) {
		for (Berechtigung berechtigung : values()) {
			if (berechtigung.bezeichnung.equals(bezeichnung)) {
				return berechtigung;
			}
		}
		throw new IllegalArgumentException("Unbekannte Berechtigung: " + bezeichnung);
	}

}
