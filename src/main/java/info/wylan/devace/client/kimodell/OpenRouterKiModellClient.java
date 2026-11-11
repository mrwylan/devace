package info.wylan.devace.client.kimodell;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Das KI-Modell über OpenRouter: Spring AI für den Fragenkatalog (UC-006 step 6), Jev für die Bewertung (UC-002 step 6,
 * ADR-003).
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

	/** Schlüssel der einen Frage an Jev; er geht nicht ans Modell. */
	static final String FRAGE = "stufe";

	/** Wartezeiten zwischen den Versuchen bei 429 und 529 (ADR-003): höchstens drei Versuche, unter 10 Sekunden. */
	private static final long[] WARTEZEIT_MS = { 500, 1500 };

	/** Zeitlimit je Versuch (NFR-006); eine Zeitüberschreitung ist UC-002 A5 und wird nicht wiederholt. */
	static final Duration ZEITLIMIT = Duration.ofSeconds(3);

	/**
	 * UC-002 step 6 — Jev über die System-One-Route von OpenRouter (ADR-003); die Anfrage enthält nur Frage,
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
		java.net.http.HttpClient http = java.net.http.HttpClient.newBuilder().connectTimeout(ZEITLIMIT).build();
		JdkClientHttpRequestFactory fabrik = new JdkClientHttpRequestFactory(http);
		fabrik.setReadTimeout(ZEITLIMIT);
		RestClient client = RestClient.builder()
			.requestFactory(fabrik)
			.baseUrl(this.properties.baseUrl())
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.properties.apiKey())
			.build();
		for (int versuch = 0;; versuch++) {
			try {
				JevAntwort antwort = client.post()
					.uri("/systemone")
					.contentType(MediaType.APPLICATION_JSON)
					.body(jev)
					.retrieve()
					.body(JevAntwort.class);
				// ADR-003: das verwendete Modell protokollieren, nie den Antworttext
				log.info("Jev bewertete mit Modell {}", (antwort != null) ? antwort.model() : "unbekannt");
				return stufe(antwort);
			}
			catch (HttpStatusCodeException ex) {
				int status = ex.getStatusCode().value();
				// ADR-003: bei 429 und 529 mit wachsender Wartezeit wiederholen, höchstens dreimal
				if ((status == 429 || status == 529) && versuch < WARTEZEIT_MS.length) {
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
