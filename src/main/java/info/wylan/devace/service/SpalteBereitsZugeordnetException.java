package info.wylan.devace.service;

import info.wylan.devace.persistence.Handlungsfeld;

/**
 * UC-010 A2: Die Spalte ist bereits einem Handlungsfeld zugeordnet (OQ-094).
 */
public class SpalteBereitsZugeordnetException extends BusinessRuleException {

	public SpalteBereitsZugeordnetException(Handlungsfeld handlungsfeld) {
		super("Diese Spalte ist bereits dem Handlungsfeld " + handlungsfeld.nummer() + " " + handlungsfeld.name()
				+ " zugeordnet.");
	}

}
