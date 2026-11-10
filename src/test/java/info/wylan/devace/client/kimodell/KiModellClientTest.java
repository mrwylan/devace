package info.wylan.devace.client.kimodell;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.net.httpserver.HttpServer;
import info.wylan.devace.service.KiModellClient.EntwurfAnfrage;
import info.wylan.devace.service.KiModellClient.HandlungsfeldAnfrage;
import info.wylan.devace.service.KiModellClient.VorgeschlageneFrage;
import info.wylan.devace.service.KiModellNichtErreichbarException;
import info.wylan.devace.usecase.UseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Adapter des KI-Modells gegen einen Stub der Chat-Schnittstelle: kein Aufruf eines echten Modells.
 */
class KiModellClientTest {

	private static final String MODELL = "test/modell";

	private static final String SCHLUESSEL = "test-api-key";

	private HttpServer stub;

	private final List<String> pfade = new CopyOnWriteArrayList<>();

	private final List<String> koepfe = new CopyOnWriteArrayList<>();

	private final List<String> koerper = new CopyOnWriteArrayList<>();

	private volatile int status = 200;

	private volatile String antwortKoerper = "";

	@BeforeEach
	void startStub() throws IOException {
		this.stub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		this.stub.createContext("/", (austausch) -> {
			this.pfade.add(austausch.getRequestURI().getPath());
			this.koepfe.add(String.valueOf(austausch.getRequestHeaders().getFirst("Authorization")));
			this.koerper.add(new String(austausch.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			byte[] antwort = this.antwortKoerper.getBytes(StandardCharsets.UTF_8);
			austausch.getResponseHeaders().add("Content-Type", "application/json");
			austausch.sendResponseHeaders(this.status, antwort.length);
			austausch.getResponseBody().write(antwort);
			austausch.close();
		});
		this.stub.start();
	}

	@AfterEach
	void stopStub() {
		this.stub.stop(0);
	}

	private OpenRouterKiModellClient adapter(String modell, String schluessel) {
		String basis = "http://127.0.0.1:" + this.stub.getAddress().getPort() + "/api/v1";
		return new OpenRouterKiModellClient(new KiProperties(basis, schluessel, new KiProperties.Modelle(modell)),
				new ClassPathResource("prompts/fragenkatalog-entwurf.st"));
	}

	private static EntwurfAnfrage anfrage() {
		return new EntwurfAnfrage("Entwicklung",
				List.of(new HandlungsfeldAnfrage(1, "Domänenwissen & Business Alignment", "Fachlichkeit", 3),
						new HandlungsfeldAnfrage(2, "Architektur & Engineering", null, 2)),
				"Praxisfragen zur Zusammenarbeit im Scrum Team");
	}

	/** Antwort des Chat-Dienstes: die entworfenen Fragen als Text der Modellantwort. */
	private static String chatAntwort(String inhalt) {
		String maskiert = inhalt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
		return "{\"id\":\"chatcmpl-1\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"" + MODELL
				+ "\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"" + maskiert
				+ "\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":1,\"completion_tokens\":1,\"total_tokens\":2}}";
	}

	@Test
	@UseCase(id = "UC-006", scenario = "Main Success Scenario")
	void bildet_die_antwort_auf_fragen_mit_sechs_vorgaben_ab() {
		this.antwortKoerper = chatAntwort("""
				{"fragen":[
				 {"handlungsfeld":1,"frage":"Wie gehen Sie mit Anforderungen um?",
				  "stufen":["s0","s1","s2","s3","s4","s5"]},
				 {"handlungsfeld":2,"frage":"Wie begründen Sie eine Entscheidung?",
				  "stufen":["a0","a1","a2","a3","a4","a5"]}]}""");

		List<VorgeschlageneFrage> fragen = adapter(MODELL, SCHLUESSEL).fragenEntwerfen(anfrage());

		assertThat(fragen).containsExactly(
				new VorgeschlageneFrage(1, "Wie gehen Sie mit Anforderungen um?",
						List.of("s0", "s1", "s2", "s3", "s4", "s5")),
				new VorgeschlageneFrage(2, "Wie begründen Sie eine Entscheidung?",
						List.of("a0", "a1", "a2", "a3", "a4", "a5")));
	}

	@Test
	@UseCase(id = "UC-006", scenario = "Main Success Scenario", businessRules = { "GR-011" })
	void ergaenzt_fehlende_vorgaben_leer_und_behaelt_die_frage() {
		this.antwortKoerper = chatAntwort("""
				{"fragen":[{"handlungsfeld":3,"frage":"Wie testen Sie?","stufen":["t0","t1","t2","t3"]}]}""");

		List<VorgeschlageneFrage> fragen = adapter(MODELL, SCHLUESSEL).fragenEntwerfen(anfrage());

		assertThat(fragen).containsExactly(
				new VorgeschlageneFrage(3, "Wie testen Sie?", List.of("t0", "t1", "t2", "t3", "", "")));
	}

	@Test
	@UseCase(id = "UC-006", scenario = "Main Success Scenario", businessRules = { "GR-007" })
	void sendet_nur_rolle_handlungsfelder_schwerpunkte_und_zahl_der_fragen() {
		this.antwortKoerper = chatAntwort("{\"fragen\":[]}");

		adapter(MODELL, SCHLUESSEL).fragenEntwerfen(anfrage());

		assertThat(this.pfade).containsExactly("/api/v1/chat/completions");
		assertThat(this.koepfe).containsExactly("Bearer " + SCHLUESSEL);
		String gesendet = this.koerper.get(0);
		assertThat(gesendet).contains("\"model\":\"" + MODELL + "\"")
			.contains("Entwicklung")
			.contains("Domänenwissen & Business Alignment")
			.contains("Architektur & Engineering")
			.contains("Praxisfragen zur Zusammenarbeit im Scrum Team")
			.contains("3 Fragen")
			.contains("2 Fragen");
		// GR-007: keine Angabe zu einer Person; die Anfrage kennt nur Rolle, Handlungsfelder, Schwerpunkte, Zahl.
		assertThat(gesendet).doesNotContainIgnoringCase("e-mail").doesNotContain("@").doesNotContainIgnoringCase("benutzer");
	}

	@Test
	@UseCase(id = "UC-006", scenario = "Main Success Scenario", businessRules = { "GR-008" })
	void verlangt_alltagsrelevanz_statt_eintrag_in_der_kompetenzmatrix() {
		this.antwortKoerper = chatAntwort("{\"fragen\":[]}");

		adapter(MODELL, SCHLUESSEL).fragenEntwerfen(anfrage());

		assertThat(this.koerper.get(0)).contains("Relevanz im Alltag").contains("nicht nach dem Eintrag in einer Kompetenzmatrix");
	}

	@Test
	@UseCase(id = "UC-006", scenario = "A2: KI-Modell liefert keinen Entwurf")
	void liefert_eine_leere_liste_wenn_das_modell_keine_frage_nennt() {
		this.antwortKoerper = chatAntwort("{\"fragen\":[]}");

		assertThat(adapter(MODELL, SCHLUESSEL).fragenEntwerfen(anfrage())).isEmpty();
	}

	@Test
	@UseCase(id = "UC-006", scenario = "A2: KI-Modell liefert keinen Entwurf")
	void meldet_nicht_erreichbar_wenn_der_dienst_mit_fehler_antwortet_ohne_dessen_text() {
		this.status = 500;
		this.antwortKoerper = "{\"error\":{\"message\":\"interner-anbietertext\",\"code\":500}}";

		assertThatThrownBy(() -> adapter(MODELL, SCHLUESSEL).fragenEntwerfen(anfrage()))
			.isInstanceOf(KiModellNichtErreichbarException.class)
			.hasMessage("Das KI-Modell ist nicht erreichbar.");
	}

	@Test
	@UseCase(id = "UC-006", scenario = "A2: KI-Modell liefert keinen Entwurf")
	void meldet_nicht_erreichbar_wenn_die_antwort_nicht_lesbar_ist() {
		this.antwortKoerper = chatAntwort("das ist kein json");

		assertThatThrownBy(() -> adapter(MODELL, SCHLUESSEL).fragenEntwerfen(anfrage()))
			.isInstanceOf(KiModellNichtErreichbarException.class)
			.hasMessage("Das KI-Modell ist nicht erreichbar.");
	}

	@Test
	@UseCase(id = "UC-006", scenario = "A2: KI-Modell liefert keinen Entwurf")
	void meldet_nicht_erreichbar_wenn_keine_verbindung_besteht() {
		OpenRouterKiModellClient adapter = adapter(MODELL, SCHLUESSEL);
		this.stub.stop(0);

		assertThatThrownBy(() -> adapter.fragenEntwerfen(anfrage())).isInstanceOf(KiModellNichtErreichbarException.class)
			.hasMessage("Das KI-Modell ist nicht erreichbar.");
	}

	@Test
	@UseCase(id = "UC-006", scenario = "A2: KI-Modell liefert keinen Entwurf")
	void ruft_ohne_modell_oder_schluessel_das_kimodell_nicht_auf() {
		assertThatThrownBy(() -> adapter("", SCHLUESSEL).fragenEntwerfen(anfrage()))
			.isInstanceOf(KiModellNichtErreichbarException.class)
			.hasMessage("Das KI-Modell ist nicht erreichbar.");
		assertThatThrownBy(() -> adapter(MODELL, " ").fragenEntwerfen(anfrage()))
			.isInstanceOf(KiModellNichtErreichbarException.class)
			.hasMessage("Das KI-Modell ist nicht erreichbar.");

		assertThat(this.pfade).isEmpty();
	}

}
