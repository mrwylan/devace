package info.wylan.devace.client.kimodell;

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
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Das KI-Modell über Spring AI und OpenRouter (UC-006 step 6, UC-002 step 6).
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

	private final Resource bewertungVorlage = new ClassPathResource("prompts/bewertung.st");

	/**
	 * UC-002 step 6 — Anfrage nur mit Frage, Stufenvorgaben und Antworttext (BR-007).
	 */
	@Override
	public OptionalInt antwortBewerten(BewertungAnfrage anfrage) {
		String modell = this.properties.modelle().bewertung();
		if (isBlank(modell) || isBlank(this.properties.apiKey())) {
			// UC-002 A5: ohne eingerichtetes Modell oder Schlüssel gibt es keine Zuordnung
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
			StringBuilder stufen = new StringBuilder();
			for (int stufe = 0; stufe < anfrage.vorgaben().size(); stufe++) {
				stufen.append("- Stufe ").append(stufe).append(": ").append(anfrage.vorgaben().get(stufe)).append('\n');
			}
			String prompt = new PromptTemplate(this.bewertungVorlage)
				.render(Map.of("frage", anfrage.frage(), "stufen", stufen.toString(), "antwort", anfrage.antwort()));
			BeanOutputConverter<KiBewertungAntwort> konverter = new BeanOutputConverter<>(KiBewertungAntwort.class);
			String text = chatModel.call(new Prompt(prompt + "\n\n" + konverter.getFormat()))
				.getResult()
				.getOutput()
				.getText();
			KiBewertungAntwort antwort = konverter.convert(text);
			// UC-002 BR-006: Nur eine Stufe von 0 bis 5 zählt als Zuordnung.
			if (antwort == null || antwort.stufe() == null || antwort.stufe() < 0 || antwort.stufe() > 5) {
				return OptionalInt.empty();
			}
			return OptionalInt.of(antwort.stufe());
		}
		catch (RuntimeException ex) {
			// UC-002 A5: nicht erreichbar oder nicht lesbar; der Text des Anbieters wird nicht weitergereicht
			log.warn("KI-Modell ordnete keine Stufe zu: {}", ex.getClass().getSimpleName());
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
