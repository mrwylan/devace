package info.wylan.devace.service;

/**
 * UC-010 step 3: Die zu ändernde oder zu entfernende Zuordnung gibt es nicht (mehr).
 */
public class MatrixZuordnungNichtGefundenException extends NotFoundException {

	public MatrixZuordnungNichtGefundenException() {
		super("Die Zuordnung ist nicht vorhanden.");
	}

}
