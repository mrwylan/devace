package info.wylan.devace.service;

/**
 * UC-009 BR-002: Den Fragenkatalog, die Frage oder den Testfall gibt es nicht, oder der Fragenkatalog ist nicht
 * prüfbar. Die Oberfläche bietet nur prüfbare Fragenkataloge an; der Text ist technisch.
 */
public class FragenkatalogNichtPruefbarException extends NotFoundException {

	public FragenkatalogNichtPruefbarException() {
		super("Dieser Fragenkatalog steht nicht zur Prüfung zur Verfügung.");
	}

}
