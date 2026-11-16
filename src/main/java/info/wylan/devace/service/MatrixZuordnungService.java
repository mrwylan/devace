package info.wylan.devace.service;

import java.util.List;

import info.wylan.devace.persistence.Handlungsfeld;
import info.wylan.devace.persistence.HandlungsfeldRepository;
import info.wylan.devace.persistence.MatrixZuordnung;
import info.wylan.devace.persistence.MatrixZuordnungListItem;
import info.wylan.devace.persistence.MatrixZuordnungRepository;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;

/**
 * Dokumentation der Zuordnung von Spalten der Kompetenzmatrix zu Handlungsfeldern (UC-010).
 */
// UC-010 BR-004: Nur der angemeldete Administrator dokumentiert die Zuordnung.
@PreAuthorize("hasRole('ADMINISTRATOR')")
@Service
@Validated
@Transactional(readOnly = true)
// UC-010 BR-003: Gespeichert werden nur Name der Spalte, Handlungsfeld und Begründung, keine Werte der Matrix.
public class MatrixZuordnungService {

	private final MatrixZuordnungRepository zuordnungRepository;

	private final HandlungsfeldRepository handlungsfeldRepository;

	private final TransactionTemplate savepoint;

	public MatrixZuordnungService(MatrixZuordnungRepository zuordnungRepository,
			HandlungsfeldRepository handlungsfeldRepository, TransactionTemplate transaktion) {
		this.zuordnungRepository = zuordnungRepository;
		this.handlungsfeldRepository = handlungsfeldRepository;
		this.savepoint = new TransactionTemplate(transaktion.getTransactionManager());
		this.savepoint.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
	}

	/**
	 * UC-010 step 2: die vorhandenen Zuordnungen, nach Handlungsfeld geordnet.
	 */
	public List<MatrixZuordnungListItem> list() {
		return this.zuordnungRepository.findAll();
	}

	/**
	 * UC-010 step 3: die sechs Handlungsfelder zur Auswahl.
	 */
	public List<Handlungsfeld> handlungsfelder() {
		return this.handlungsfeldRepository.findAll();
	}

	/**
	 * UC-010 steps 3–5: legt eine Zuordnung an; A1 bei ungültigen Angaben, A2 wenn die Spalte schon zugeordnet ist.
	 */
	@Transactional
	public long anlegen(@Valid MatrixZuordnungEingabe eingabe) {
		pruefe(eingabe, null);
		MatrixZuordnung zuordnung = zuordnung(null, eingabe);
		return schreibe(eingabe, null, () -> this.zuordnungRepository.insert(zuordnung));
	}

	/**
	 * UC-010 steps 3–5: ändert Spalte, Handlungsfeld oder Begründung einer Zuordnung; A1 und A2 wie beim Anlegen.
	 */
	@Transactional
	public void aendern(long id, @Valid MatrixZuordnungEingabe eingabe) {
		if (!this.zuordnungRepository.existsById(id)) {
			throw new MatrixZuordnungNichtGefundenException();
		}
		pruefe(eingabe, id);
		MatrixZuordnung zuordnung = zuordnung(id, eingabe);
		schreibe(eingabe, id, () -> this.zuordnungRepository.update(zuordnung) ? 1L : 0L);
	}

	/**
	 * UC-010 steps 3–5: entfernt eine Zuordnung, nachdem der Administrator es bestätigt hat (OQ-095).
	 */
	@Transactional
	public void entfernen(long id) {
		if (!this.zuordnungRepository.delete(id)) {
			throw new MatrixZuordnungNichtGefundenException();
		}
	}

	private void pruefe(MatrixZuordnungEingabe eingabe, @Nullable Long ausserId) {
		// UC-010 BR-002: Eine Spalte ist höchstens einem Handlungsfeld zugeordnet (ohne Schreibweise).
		this.zuordnungRepository.findHandlungsfeldDerSpalte(spalte(eingabe), ausserId).ifPresent((feld) -> {
			throw new SpalteBereitsZugeordnetException(feld);
		});
	}

	/**
	 * Schreibt hinter einem Savepoint: Kommt eine gleichzeitige Zuordnung derselben Spalte an der Prüfung vorbei,
	 * bleibt die Transaktion nutzbar, und A2 nennt das Handlungsfeld der anderen Zuordnung.
	 */
	private long schreibe(MatrixZuordnungEingabe eingabe, @Nullable Long ausserId, Schreiben schreiben) {
		try {
			Long ergebnis = this.savepoint.execute((status) -> schreiben.ausfuehren());
			return (ergebnis != null) ? ergebnis : 0L;
		}
		catch (DuplicateKeyException ex) {
			// UC-010 BR-002: Die Datenbank weist die zweite Zuordnung derselben Spalte ab.
			pruefe(eingabe, ausserId);
			throw ex;
		}
	}

	private static MatrixZuordnung zuordnung(@Nullable Long id, MatrixZuordnungEingabe eingabe) {
		Long handlungsfeldId = eingabe.handlungsfeldId();
		if (handlungsfeldId == null) {
			// tritt nicht ein: @Valid hat die Pflichtangabe vor dem Methodenrumpf geprüft
			throw new IllegalArgumentException("Handlungsfeld fehlt");
		}
		return new MatrixZuordnung(id, handlungsfeldId, spalte(eingabe), eingabe.begruendung());
	}

	private static String spalte(MatrixZuordnungEingabe eingabe) {
		String spalte = eingabe.matrixSpalte();
		return (spalte != null) ? spalte : "";
	}

	@FunctionalInterface
	private interface Schreiben {

		long ausfuehren();

	}

}
