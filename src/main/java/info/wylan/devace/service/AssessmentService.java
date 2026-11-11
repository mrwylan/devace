package info.wylan.devace.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import info.wylan.devace.persistence.AssessmentRepository;
import info.wylan.devace.persistence.BeantwortbareFrage;
import info.wylan.devace.persistence.EinladungRepository;
import info.wylan.devace.persistence.EinladungZustand;
import info.wylan.devace.persistence.Fragenkatalog;
import info.wylan.devace.persistence.FragenkatalogRepository;
import info.wylan.devace.persistence.LaufendesAssessment;
import info.wylan.devace.persistence.RolleRepository;
import info.wylan.devace.persistence.Starthinweis;
import info.wylan.devace.persistence.StarthinweisRepository;
import info.wylan.devace.persistence.Verlauf;
import info.wylan.devace.service.KiModellClient.BewertungAnfrage;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Assessment durchführen (UC-002). Der Teilnehmer meldet sich nicht an: Die zufällige Referenz seiner Einladung ist
 * sein Zugang; die Einladung ist die einzige Verbindung zu seiner MAG und Rolle.
 */
@Service
@Transactional(readOnly = true)
public class AssessmentService {

	static final int HOECHSTZAHL_ZEICHEN = 500;

	static final Duration AUFBEWAHRUNG = Duration.ofDays(7);

	private final EinladungRepository einladungRepository;

	private final AssessmentRepository assessmentRepository;

	private final FragenkatalogRepository fragenkatalogRepository;

	private final StarthinweisRepository starthinweisRepository;

	private final RolleRepository rolleRepository;

	private final KiModellClient kiModell;

	private final TransactionTemplate transaktion;

	private final Clock clock;

	public AssessmentService(EinladungRepository einladungRepository, AssessmentRepository assessmentRepository,
			FragenkatalogRepository fragenkatalogRepository, StarthinweisRepository starthinweisRepository,
			RolleRepository rolleRepository, KiModellClient kiModell, TransactionTemplate transaktion, Clock clock) {
		this.einladungRepository = einladungRepository;
		this.assessmentRepository = assessmentRepository;
		this.fragenkatalogRepository = fragenkatalogRepository;
		this.starthinweisRepository = starthinweisRepository;
		this.rolleRepository = rolleRepository;
		this.kiModell = kiModell;
		this.transaktion = transaktion;
		this.clock = clock;
	}

	/**
	 * UC-002 steps 1–2: was der Teilnehmer beim Öffnen der Einladung sieht; A1 bei einer nicht nutzbaren Einladung,
	 * A2 bei einem laufenden Assessment.
	 */
	public Einstieg einstieg(String referenz) {
		Optional<EinladungZustand> einladung = this.einladungRepository.findByReferenz(referenz);
		if (einladung.isEmpty()) {
			return new Einstieg.NichtNutzbar();
		}
		Optional<LaufendesAssessment> laufend = this.assessmentRepository.findLaufend(einladung.get().id(), grenze());
		if (laufend.isPresent()) {
			return new Einstieg.Fortsetzen(dialog(laufend.get()));
		}
		if (!nutzbar(einladung.get())) {
			return new Einstieg.NichtNutzbar();
		}
		return new Einstieg.StarthinweisNoetig(geltenderStarthinweis(), rollenName(einladung.get().rolleId()));
	}

	/**
	 * UC-002 steps 3–4: startet das Assessment mit dem bestätigten Starthinweis und stellt die erste Frage; A3
	 * ohne Bestätigung.
	 */
	@Transactional
	public Dialog starten(String referenz, boolean bestaetigt) {
		// UC-002 BR-002: Das Assessment beginnt erst, nachdem der Teilnehmer den Starthinweis bestätigt hat.
		if (!bestaetigt) {
			throw new StarthinweisNichtBestaetigtException();
		}
		EinladungZustand einladung = this.einladungRepository.findByReferenz(referenz)
			.orElseThrow(EinladungNichtNutzbarException::new);
		Optional<LaufendesAssessment> laufend = this.assessmentRepository.findLaufend(einladung.id(), grenze());
		if (laufend.isPresent()) {
			return dialog(laufend.get());
		}
		if (!nutzbar(einladung)) {
			throw new EinladungNichtNutzbarException();
		}
		Starthinweis hinweis = geltenderStarthinweis();
		Fragenkatalog katalog = this.fragenkatalogRepository.findFreigegeben(einladung.rolleId())
			// UC-002 BR-003: Der Teilnehmer erhält die Fragen des freigegebenen Fragenkatalogs seiner Rolle.
			.orElseThrow(() -> new IllegalStateException("Kein freigegebener Fragenkatalog für die Rolle"));
		long id = this.assessmentRepository.insert(einladung.id(), einladung.magId(), einladung.rolleId(),
				katalog.id(), hinweis.id(), LocalDateTime.now(this.clock));
		// UC-002 BR-001: Mit dem Start gilt die Einladung als eingelöst.
		this.einladungRepository.markiereEingeloest(einladung.id());
		return dialog(new LaufendesAssessment(id, einladung.rolleId(), katalog.id()));
	}

	/**
	 * UC-002 steps 5–7: speichert die Antwort mit der vom KI-Modell zugeordneten Stufe und stellt die nächste Frage;
	 * A4 bei einer leeren oder zu langen Antwort, A5 wenn keine Stufe zugeordnet wird. Der Aufruf des KI-Modells
	 * hält keine Datenbanktransaktion offen.
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public Dialog antworten(String referenz, String text) {
		EinladungZustand einladung = this.einladungRepository.findByReferenz(referenz)
			.orElseThrow(EinladungNichtNutzbarException::new);
		LaufendesAssessment assessment = this.assessmentRepository.findLaufend(einladung.id(), grenze())
			.orElseThrow(EinladungNichtNutzbarException::new);
		String antwort = (text != null) ? text.strip() : "";
		// UC-002 BR-004: Eine Antwort ist nicht leer und hat höchstens 500 Zeichen.
		if (antwort.isEmpty()) {
			throw new AntwortUngueltigException("Bitte geben Sie eine Antwort ein.");
		}
		if (antwort.length() > HOECHSTZAHL_ZEICHEN) {
			throw new AntwortUngueltigException("Die Antwort ist zu lang: " + antwort.length() + " von höchstens "
					+ HOECHSTZAHL_ZEICHEN + " Zeichen.");
		}
		Dialog bisher = dialog(assessment);
		BeantwortbareFrage frage = naechsteFrage(assessment);
		if (frage == null) {
			return bisher;
		}
		int stufe = stufeZuordnen(frage, antwort);
		try {
			this.transaktion.executeWithoutResult((status) -> this.assessmentRepository.insertAntwort(assessment.id(),
					frage.id(), antwort, stufe, LocalDateTime.now(this.clock)));
		}
		catch (DuplicateKeyException ex) {
			// UC-002 BR-005: Jede Frage wird genau einmal beantwortet; eine zweite Antwort entsteht nicht.
		}
		return dialog(assessment);
	}

	/**
	 * UC-002 BR-009: löscht laufende Assessments, deren letzte Eingabe 7 Tage zurückliegt, mit ihren Antworten und
	 * Stufen; die Einladung bleibt eingelöst. Liefert die Zahl der gelöschten Assessments.
	 */
	@Transactional
	public int loescheLiegengebliebene() {
		return this.assessmentRepository.loescheLaufendeSeit(grenze());
	}

	/** UC-002 BR-009: Vor dieser Zeit liegt die letzte Eingabe eines Assessments, das nicht mehr läuft. */
	private LocalDateTime grenze() {
		return LocalDateTime.now(this.clock).minus(AUFBEWAHRUNG);
	}

	private int stufeZuordnen(BeantwortbareFrage frage, String antwort) {
		try {
			// UC-002 BR-007: Das KI-Modell erhält nur die Frage, ihre Stufenvorgaben und den Text der Antwort.
			return this.kiModell.antwortBewerten(new BewertungAnfrage(frage.text(), frage.vorgaben(), antwort))
				.orElse(0);
		}
		catch (KiModellNichtErreichbarException ex) {
			// UC-002 A5, BR-006: Ohne Zuordnung gilt die Stufe 0.
			return 0;
		}
	}

	private @Nullable BeantwortbareFrage naechsteFrage(LaufendesAssessment assessment) {
		Set<Long> beantwortet = Set.copyOf(this.assessmentRepository.beantworteteFragen(assessment.id()));
		// UC-002 BR-003: in der Reihenfolge des Fragenkatalogs; A2: die erste Frage ohne Antwort
		return this.fragenkatalogRepository.findBeantwortbareFragen(assessment.fragenkatalogId())
			.stream()
			.filter((frage) -> !beantwortet.contains(frage.id()))
			.findFirst()
			.orElse(null);
	}

	private Dialog dialog(LaufendesAssessment assessment) {
		List<BeantwortbareFrage> fragen = this.fragenkatalogRepository
			.findBeantwortbareFragen(assessment.fragenkatalogId());
		// UC-002 BR-008: Der Verlauf enthält die Stufe nicht.
		List<Verlauf> verlauf = this.assessmentRepository.verlauf(assessment.id());
		BeantwortbareFrage naechste = naechsteFrage(assessment);
		return new Dialog(rollenName(assessment.rolleId()), fragen.size(), verlauf, (naechste != null) ? naechste.text() : null);
	}

	private Starthinweis geltenderStarthinweis() {
		return this.starthinweisRepository.findGeltend(LocalDate.now(this.clock))
			.orElseThrow(() -> new IllegalStateException("Kein Starthinweis hinterlegt"));
	}

	private String rollenName(long rolleId) {
		return this.rolleRepository.findName(rolleId).orElse("");
	}

	private boolean nutzbar(EinladungZustand einladung) {
		// UC-002 BR-001: Bis zum Ablaufdatum (24 Uhr des letzten Tages) und nur, solange sie nicht eingelöst ist.
		return EinladungRepository.VERSENDET.equals(einladung.status())
				&& !LocalDate.now(this.clock).isAfter(einladung.laeuftAbAm());
	}

	/** Was der Teilnehmer beim Öffnen der Einladung sieht. */
	public sealed interface Einstieg {

		/** A1: Die Einladung ist unbekannt, eingelöst oder abgelaufen. */
		record NichtNutzbar() implements Einstieg {
		}

		/** Step 2: Der Teilnehmer liest und bestätigt den Starthinweis. */
		record StarthinweisNoetig(Starthinweis hinweis, String rolle) implements Einstieg {
		}

		/** A2: Das laufende Assessment geht bei der ersten Frage ohne Antwort weiter. */
		record Fortsetzen(Dialog dialog) implements Einstieg {
		}

	}

	/**
	 * Der Dialog: bereits beantwortete Fragen mit Antwort, aber ohne Stufe (BR-008), und die nächste Frage;
	 * {@code naechsteFrage} ist leer, wenn alle Fragen beantwortet sind (step 8).
	 */
	public record Dialog(String rolle, int anzahl, List<Verlauf> verlauf, @Nullable String naechsteFrage) {

		public boolean alleBeantwortet() {
			return this.naechsteFrage == null;
		}

		/** Nummer der Frage, die als Nächste gestellt wird; bei allen beantworteten die letzte. */
		public int nummer() {
			return Math.min(this.verlauf.size() + 1, this.anzahl);
		}

	}

}
