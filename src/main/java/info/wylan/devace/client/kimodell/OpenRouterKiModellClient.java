package info.wylan.devace.client.kimodell;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import info.wylan.devace.service.KiModellClient;
import info.wylan.devace.service.KiModellNichtErreichbarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Das KI-Modell über OpenRouter: Spring AI für den Fragenkatalog (UC-006 step 6) und den Textreport (UC-003 step 3), Jev für die Bewertung (UC-002 step 6,
 * ADR-004).
 */
@Component
class OpenRouterKiModellClient implements KiModellClient {

	private static final Logger log = LoggerFactory.getLogger(OpenRouterKiModellClient.class);

	private final KiProperties properties;

	private final Resource promptVorlage;

	OpenRouterKiModellClient(KiProperties properties,
			@Value("classpath:prompts/fragenkatalog-entwurf.st") Resource promptVorlage) {
		this.properties = properties;
		this.promptVorlage = promptVorlage;
	}

	/**
	 * UC-006 step 6 — Anfrage nur mit Rolle, Handlungsfeldern, Schwerpunkten und Zahl der Fragen (BR-007).
	 */
	@Override
	public List<VorgeschlageneFrage> fragenEntwerfen(EntwurfAnfrage anfrage) {
		String modell = this.properties.modelle().fragenkatalog();
		if (isBlank(modell) || isBlank(this.properties.apiKey())) {
			// UC-006 A2: ohne eingerichtetes Modell oder Schlüssel gibt es keinen Entwurf
			throw new KiModellNichtErreichbarException(
					new IllegalStateException("KI-Modell oder OPENROUTER_API_KEY nicht eingerichtet"));
		}
		try {
			OpenAiChatModel chatModel = OpenAiChatModel.builder()
				.options(OpenAiChatOptions.builder()
					.baseUrl(this.properties.baseUrl())
					.apiKey(this.properties.apiKey())
					.model(modell)
					.build())
				.build();
			String prompt = new PromptTemplate(this.promptVorlage).render(Map.of("rolle", anfrage.rolle(),
					"handlungsfelder", handlungsfelder(anfrage), "schwerpunkte",
					(anfrage.schwerpunkte() != null) ? anfrage.schwerpunkte() : "keine"));
			BeanOutputConverter<KiEntwurfAntwort> konverter = new BeanOutputConverter<>(KiEntwurfAntwort.class);
			String text = chatModel.call(new Prompt(prompt + "\n\n" + konverter.getFormat()))
				.getResult()
				.getOutput()
				.getText();
			return abbilden(konverter.convert(text));
		}
		catch (RuntimeException ex) {
			// UC-006 A2: nicht erreichbar oder nicht lesbar; der Text des Anbieters wird nicht weitergereicht
			log.warn("KI-Modell lieferte keinen Entwurf: {}", ex.getClass().getSimpleName());
			throw new KiModellNichtErreichbarException(ex);
		}
	}

	private final Resource textreportVorlage = new ClassPathResource("prompts/textreport.st");

	/** UC-003 A1: Zeitlimit für den Textreport (NFR-006). */
	static final Duration ZEITLIMIT_TEXTREPORT = Duration.ofSeconds(10);

	/**
	 * UC-003 step 3 — Anfrage nur mit Rolle, Handlungsfeldern, Scores, Fragen, Antworten und Stufen (BR-004).
	 */
	@Override
	public Optional<Textreport> textreportErstellen(TextreportAnfrage anfrage) {
		String modell = this.properties.modelle().textreport();
		if (isBlank(modell) || isBlank(this.properties.apiKey())) {
			// UC-003 A1: ohne eingerichtetes Modell oder Schlüssel gibt es keinen Textreport
			throw new KiModellNichtErreichbarException(
					new IllegalStateException("KI-Modell oder OPENROUTER_API_KEY nicht eingerichtet"));
		}
		try {
			OpenAiChatModel chatModel = OpenAiChatModel.builder()
				.options(OpenAiChatOptions.builder()
					.baseUrl(this.properties.baseUrl())
					.apiKey(this.properties.apiKey())
					.model(modell)
					// UC-003 A1, NFR-006: ohne Textreport innerhalb von 10 s gilt A1, ohne Wiederholung
					.timeout(ZEITLIMIT_TEXTREPORT)
					.maxRetries(0)
					.build())
				.build();
			StringBuilder felder = new StringBuilder();
			for (HandlungsfeldAuswertung feld : anfrage.handlungsfelder()) {
				felder.append("- ").append(feld.name()).append(": Score ").append(feld.score()).append('\n');
				for (BewerteteFrage frage : feld.fragen()) {
					felder.append("  - Frage: ").append(frage.frage()).append(" | Antwort: ").append(frage.antwort())
						.append(" | Stufe: ").append(frage.stufe()).append('\n');
				}
			}
			String prompt = new PromptTemplate(this.textreportVorlage)
				.render(Map.of("rolle", anfrage.rolle(), "handlungsfelder", felder.toString()));
			BeanOutputConverter<KiTextreportAntwort> konverter = new BeanOutputConverter<>(KiTextreportAntwort.class);
			String text = chatModel.call(new Prompt(prompt + "\n\n" + konverter.getFormat()))
				.getResult()
				.getOutput()
				.getText();
			return textreport(konverter.convert(text));
		}
		catch (RuntimeException ex) {
			// UC-003 A1: nicht erreichbar oder nicht lesbar; der Text des Anbieters wird nicht weitergereicht
			log.warn("KI-Modell lieferte keinen Textreport: {}", ex.getClass().getSimpleName());
			throw new KiModellNichtErreichbarException(ex);
		}
	}

	private static Optional<Textreport> textreport(KiTextreportAntwort antwort) {
		if (antwort == null || isBlank(antwort.ueberblick()) || antwort.abschnitte() == null) {
			return Optional.empty();
		}
		List<Abschnitt> abschnitte = new ArrayList<>();
		for (KiTextreportAntwort.Abschnitt abschnitt : antwort.abschnitte()) {
			if (abschnitt != null && !isBlank(abschnitt.handlungsfeld()) && !isBlank(abschnitt.wuerdigung())) {
				abschnitte.add(new Abschnitt(abschnitt.handlungsfeld(), abschnitt.wuerdigung(),
						(abschnitt.naechsterSchritt() != null) ? abschnitt.naechsterSchritt() : ""));
			}
		}
		return abschnitte.isEmpty() ? Optional.empty() : Optional.of(new Textreport(antwort.ueberblick(), abschnitte));
	}

	/** Schlüssel der einen Frage an Jev; er geht nicht ans Modell. */
	static final String FRAGE = "stufe";

	/** Wartezeiten zwischen den Versuchen bei 429 und 529 (ADR-004): höchstens drei Versuche. */
	private static final long[] WARTEZEIT_MS = { 500, 1500 };

	/** Zeitlimit je Versuch (NFR-006); eine Zeitüberschreitung ist UC-002 A5 und wird nicht wiederholt. */
	static final Duration ZEITLIMIT = Duration.ofSeconds(3);

	/** Gesamtbudget aller Versuche und Wartezeiten einer Bewertung (NFR-006, ADR-004). */
	static final Duration GESAMTBUDGET = Duration.ofSeconds(10);

	/**
	 * UC-002 step 6 — Jev über die System-One-Route von OpenRouter (ADR-004); die Anfrage enthält nur Frage,
	 * Stufenvorgaben und Antworttext (BR-007).
	 */
	@Override
	public OptionalInt antwortBewerten(BewertungAnfrage anfrage) {
		String modell = this.properties.modelle().bewertung();
		if (isBlank(modell) || isBlank(this.properties.apiKey())) {
			// UC-002 A5: ohne eingerichtetes Modell oder Schlüssel gibt es keine Zuordnung
			throw new KiModellNichtErreichbarException(
					new IllegalStateException("KI-Modell oder OPENROUTER_API_KEY nicht eingerichtet"));
		}
		List<String> stufen = new ArrayList<>();
		for (int stufe = 0; stufe < anfrage.vorgaben().size(); stufe++) {
			stufen.add("Stufe " + stufe + ": " + anfrage.vorgaben().get(stufe));
		}
		JevAnfrage jev = new JevAnfrage(anfrage.antwort(), modell,
				Map.of(FRAGE, new JevAnfrage.Frage("score", "Frage aus einem Kompetenz-Assessment: " + anfrage.frage()
						+ " Ordne die Antwort der Stufe zu, deren Beschreibung am besten passt.", stufen)));
		long frist = System.nanoTime() + GESAMTBUDGET.toNanos();
		for (int versuch = 0;; versuch++) {
			// NFR-006: jeder Versuch höchstens 3 s, alle Versuche zusammen höchstens 10 s
			Duration zeitlimit = kleinere(ZEITLIMIT, rest(frist));
			try {
				JevAntwort antwort = client(zeitlimit).post()
					.uri("/systemone")
					.contentType(MediaType.APPLICATION_JSON)
					.body(jev)
					.retrieve()
					.body(JevAntwort.class);
				// ADR-004: das verwendete Modell protokollieren, nie den Antworttext
				log.info("Jev bewertete mit Modell {}", (antwort != null) ? antwort.model() : "unbekannt");
				return stufe(antwort);
			}
			catch (HttpStatusCodeException ex) {
				int status = ex.getStatusCode().value();
				// ADR-004: bei 429 und 529 mit wachsender Wartezeit wiederholen, höchstens dreimal und nur,
				// solange nach der Wartezeit noch Zeit für einen Versuch im Gesamtbudget bleibt
				if ((status == 429 || status == 529) && versuch < WARTEZEIT_MS.length
						&& rest(frist).toMillis() > WARTEZEIT_MS[versuch]) {
					warte(WARTEZEIT_MS[versuch]);
					continue;
				}
				log.warn("Jev ordnete keine Stufe zu: HTTP {}", status);
				throw new KiModellNichtErreichbarException(ex);
			}
			catch (RuntimeException ex) {
				// UC-002 A5: nicht erreichbar oder nicht lesbar; der Text des Anbieters wird nicht weitergereicht
				log.warn("Jev ordnete keine Stufe zu: {}", ex.getClass().getSimpleName());
				throw new KiModellNichtErreichbarException(ex);
			}
		}
	}

	private RestClient client(Duration zeitlimit) {
		java.net.http.HttpClient http = java.net.http.HttpClient.newBuilder().connectTimeout(zeitlimit).build();
		JdkClientHttpRequestFactory fabrik = new JdkClientHttpRequestFactory(http);
		fabrik.setReadTimeout(zeitlimit);
		return RestClient.builder()
			.requestFactory(fabrik)
			.baseUrl(this.properties.baseUrl())
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.properties.apiKey())
			.build();
	}

	private static Duration rest(long frist) {
		long nanos = frist - System.nanoTime();
		return (nanos > 0) ? Duration.ofNanos(nanos) : Duration.ofMillis(1);
	}

	private static Duration kleinere(Duration a, Duration b) {
		return (a.compareTo(b) <= 0) ? a : b;
	}

	/** UC-002 BR-006: die Stufe mit der höchsten Wahrscheinlichkeit, gelesen über ihren Namen; leer ohne Zuordnung. */
	private static OptionalInt stufe(JevAntwort antwort) {
		if (antwort == null || antwort.answers() == null || antwort.answers().get(FRAGE) == null
				|| antwort.answers().get(FRAGE).probabilities() == null) {
			return OptionalInt.empty();
		}
		Map<String, Double> wahrscheinlichkeiten = antwort.answers().get(FRAGE).probabilities();
		int beste = -1;
		double hoechste = -1;
		for (int stufe = 0; stufe <= 5; stufe++) {
			Double wert = wahrscheinlichkeiten.get(Integer.toString(stufe));
			if (wert != null && wert > hoechste) {
				hoechste = wert;
				beste = stufe;
			}
		}
		return (beste < 0) ? OptionalInt.empty() : OptionalInt.of(beste);
	}

	private static void warte(long millisekunden) {
		try {
			Thread.sleep(millisekunden);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new KiModellNichtErreichbarException(ex);
		}
	}

	private static String handlungsfelder(EntwurfAnfrage anfrage) {
		return anfrage.handlungsfelder()
			.stream()
			.map(h -> "- Handlungsfeld " + h.nummer() + " (" + h.name()
					+ ((h.fokus() != null) ? ", Fokus: " + h.fokus() : "") + "): " + h.anzahl() + " Fragen")
			.collect(Collectors.joining("\n"));
	}

	private static List<VorgeschlageneFrage> abbilden(KiEntwurfAntwort antwort) {
		List<VorgeschlageneFrage> fragen = new ArrayList<>();
		if (antwort == null || antwort.fragen() == null) {
			return fragen;
		}
		for (KiEntwurfAntwort.Frage frage : antwort.fragen()) {
			if (frage == null || isBlank(frage.frage())) {
				continue;
			}
			List<String> vorgaben = new ArrayList<>();
			for (int stufe = 0; stufe < 6; stufe++) {
				String text = (frage.stufen() != null && stufe < frage.stufen().size()) ? frage.stufen().get(stufe)
						: null;
				vorgaben.add((text != null) ? text : "");
			}
			fragen.add(new VorgeschlageneFrage(frage.handlungsfeld(), frage.frage(), vorgaben));
		}
		return fragen;
	}

	private static boolean isBlank(String wert) {
		return wert == null || wert.isBlank();
	}

}
