package info.wylan.devace.client.kimodell;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.net.httpserver.HttpServer;
import info.wylan.devace.service.KiModellClient.Abschnitt;
import info.wylan.devace.service.KiModellClient.BewerteteFrage;
import info.wylan.devace.service.KiModellClient.BewertungAnfrage;
import info.wylan.devace.service.KiModellClient.HandlungsfeldAuswertung;
import info.wylan.devace.service.KiModellClient.Textreport;
import info.wylan.devace.service.KiModellClient.TextreportAnfrage;
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
 * Adapter des KI-Modells gegen einen Stub der Schnittstellen von OpenRouter (Chat für UC-006, System One von Jev für
 * UC-002, ADR-003): kein Aufruf eines echten Modells.
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

	/** Verzögerung der Antwort des Stubs in Millisekunden. */
	private volatile long verzoegerung;

	/** Status der ersten Aufrufe, danach gilt {@link #status}. */
	private final Deque<Integer> statusFolge = new ConcurrentLinkedDeque<>();

	@BeforeEach
	void startStub() throws IOException {
		this.stub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		this.stub.createContext("/", (austausch) -> {
			this.pfade.add(austausch.getRequestURI().getPath());
			this.koepfe.add(String.valueOf(austausch.getRequestHeaders().getFirst("Authorization")));
			this.koerper.add(new String(austausch.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			if (this.verzoegerung > 0) {
				try {
					Thread.sleep(this.verzoegerung);
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}
			byte[] antwort = this.antwortKoerper.getBytes(StandardCharsets.UTF_8);
			austausch.getResponseHeaders().add("Content-Type", "application/json");
			Integer naechster = this.statusFolge.poll();
			austausch.sendResponseHeaders((naechster != null) ? naechster : this.status, antwort.length);
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
	@UseCase(id = "UC-006", scenario = "Main Success Scenario", businessRules = { "BR-011" })
	void ergaenzt_fehlende_vorgaben_leer_und_behaelt_die_frage() {
		this.antwortKoerper = chatAntwort("""
				{"fragen":[{"handlungsfeld":3,"frage":"Wie testen Sie?","stufen":["t0","t1","t2","t3"]}]}""");

		List<VorgeschlageneFrage> fragen = adapter(MODELL, SCHLUESSEL).fragenEntwerfen(anfrage());

		assertThat(fragen).containsExactly(
				new VorgeschlageneFrage(3, "Wie testen Sie?", List.of("t0", "t1", "t2", "t3", "", "")));
	}

	@Test
	@UseCase(id = "UC-006", scenario = "Main Success Scenario", businessRules = { "BR-007" })
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
		// BR-007: keine Angabe zu einer Person; die Anfrage kennt nur Rolle, Handlungsfelder, Schwerpunkte, Zahl.
		assertThat(gesendet).doesNotContainIgnoringCase("e-mail").doesNotContain("@").doesNotContainIgnoringCase("benutzer");
	}

	@Test
	@UseCase(id = "UC-006", scenario = "Main Success Scenario", businessRules = { "BR-008" })
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

	// ---------------------------------------------------------------- UC-002: Bewertung über Jev (ADR-003)

	private static final String JEV = "typesafe/jev-test";

	private OpenRouterKiModellClient bewertungsAdapter(String modell, String schluessel) {
		String basis = "http://127.0.0.1:" + this.stub.getAddress().getPort() + "/api/v1";
		return new OpenRouterKiModellClient(new KiProperties(basis, schluessel, new KiProperties.Modelle("", modell)),
				new ClassPathResource("prompts/fragenkatalog-entwurf.st"));
	}

	private static BewertungAnfrage bewertung() {
		return new BewertungAnfrage("Wie begründen Sie eine Architekturentscheidung?",
				List.of("v0", "v1", "v2", "v3", "v4", "v5"), "Ich halte Kontext, Alternativen und Folgen fest.");
	}

	/** Antwort von Jev mit den Wahrscheinlichkeiten je Stufe, die Schlüssel bewusst nicht in Reihenfolge. */
	private static String jevAntwort(String wahrscheinlichkeiten) {
		return "{\"model\":\"typesafe/jev-test-1\",\"answers\":{\"stufe\":{\"type\":\"score\",\"score\":3.1,"
				+ "\"probabilities\":" + wahrscheinlichkeiten + ",\"confidence\":0.8}},\"usage\":{\"input_tokens\":1}}";
	}

	@Test
	@UseCase(id = "UC-002", scenario = "Main Success Scenario", businessRules = { "BR-006" })
	void ordnet_die_stufe_mit_der_hoechsten_wahrscheinlichkeit_zu_gelesen_ueber_den_namen() {
		this.antwortKoerper = jevAntwort("{\"5\":0.01,\"2\":0.1,\"4\":0.2,\"0\":0,\"3\":0.69,\"1\":0}");

		assertThat(bewertungsAdapter(JEV, SCHLUESSEL).antwortBewerten(bewertung())).isEqualTo(OptionalInt.of(3));
	}

	@Test
	@UseCase(id = "UC-002", scenario = "Main Success Scenario", businessRules = { "BR-007" })
	void sendet_nur_frage_stufenvorgaben_und_antworttext_an_die_system_one_route() {
		this.antwortKoerper = jevAntwort("{\"0\":1}");

		bewertungsAdapter(JEV, SCHLUESSEL).antwortBewerten(bewertung());

		// ADR-003: über OpenRouter, nicht direkt beim Anbieter
		assertThat(this.pfade).containsExactly("/api/v1/systemone");
		assertThat(this.koepfe).containsExactly("Bearer " + SCHLUESSEL);
		String gesendet = this.koerper.get(0);
		assertThat(gesendet).contains("\"model\":\"" + JEV + "\"")
			.contains("\"state\":\"Ich halte Kontext, Alternativen und Folgen fest.\"")
			.contains("\"type\":\"score\"")
			.contains("Wie begründen Sie eine Architekturentscheidung?")
			.contains("\"criteria\":[\"Stufe 0: v0\",\"Stufe 1: v1\",\"Stufe 2: v2\",\"Stufe 3: v3\",\"Stufe 4: v4\",\"Stufe 5: v5\"]");
		// BR-007: keine Einladung, keine E-Mail-Adresse, keine Angabe zur Person
		assertThat(gesendet).doesNotContain("@").doesNotContainIgnoringCase("einladung").doesNotContainIgnoringCase("referenz");
	}

	@Test
	@UseCase(id = "UC-002", scenario = "A5: Bewertung nicht möglich", businessRules = { "BR-006" })
	void liefert_keine_stufe_wenn_jev_keine_wahrscheinlichkeiten_nennt() {
		this.antwortKoerper = "{\"model\":\"typesafe/jev-test-1\",\"answers\":{}}";

		assertThat(bewertungsAdapter(JEV, SCHLUESSEL).antwortBewerten(bewertung())).isEmpty();
	}

	@Test
	@UseCase(id = "UC-002", scenario = "A5: Bewertung nicht möglich")
	void wiederholt_bei_429_und_529_und_ordnet_dann_zu() {
		this.statusFolge.add(429);
		this.statusFolge.add(529);
		this.antwortKoerper = jevAntwort("{\"4\":0.9,\"3\":0.1}");

		assertThat(bewertungsAdapter(JEV, SCHLUESSEL).antwortBewerten(bewertung())).isEqualTo(OptionalInt.of(4));
		// ADR-003: höchstens drei Versuche
		assertThat(this.pfade).hasSize(3);
	}

	@Test
	@UseCase(id = "UC-002", scenario = "A5: Bewertung nicht möglich")
	void meldet_nicht_erreichbar_nach_drei_versuchen_mit_429() {
		this.status = 429;
		this.antwortKoerper = "{\"error\":{\"message\":\"zu viele\",\"code\":429}}";

		assertThatThrownBy(() -> bewertungsAdapter(JEV, SCHLUESSEL).antwortBewerten(bewertung()))
			.isInstanceOf(KiModellNichtErreichbarException.class)
			.hasMessage("Das KI-Modell ist nicht erreichbar.");
		assertThat(this.pfade).hasSize(3);
	}

	@Test
	@UseCase(id = "UC-002", scenario = "A5: Bewertung nicht möglich")
	void meldet_nicht_erreichbar_ohne_wiederholung_bei_anderen_fehlern() {
		this.status = 422;
		this.antwortKoerper = "{\"error\":{\"message\":\"interner-anbietertext\",\"code\":422}}";

		assertThatThrownBy(() -> bewertungsAdapter(JEV, SCHLUESSEL).antwortBewerten(bewertung()))
			.isInstanceOf(KiModellNichtErreichbarException.class)
			.hasMessage("Das KI-Modell ist nicht erreichbar.");
		assertThat(this.pfade).hasSize(1);
	}

	@Test
	@UseCase(id = "UC-002", scenario = "A5: Bewertung nicht möglich")
	void meldet_nicht_erreichbar_wenn_keine_verbindung_besteht_bei_der_bewertung() {
		OpenRouterKiModellClient adapter = bewertungsAdapter(JEV, SCHLUESSEL);
		this.stub.stop(0);

		assertThatThrownBy(() -> adapter.antwortBewerten(bewertung())).isInstanceOf(KiModellNichtErreichbarException.class);
	}

	@Test
	@UseCase(id = "UC-002", scenario = "A5: Bewertung nicht möglich")
	void ruft_jev_ohne_modell_oder_schluessel_nicht_auf() {
		assertThatThrownBy(() -> bewertungsAdapter("", SCHLUESSEL).antwortBewerten(bewertung()))
			.isInstanceOf(KiModellNichtErreichbarException.class);
		assertThatThrownBy(() -> bewertungsAdapter(JEV, " ").antwortBewerten(bewertung()))
			.isInstanceOf(KiModellNichtErreichbarException.class);

		assertThat(this.pfade).isEmpty();
	}

	@Test
	@UseCase(id = "UC-002", scenario = "A5: Bewertung nicht möglich", requirements = { "NFR-006" })
	void bricht_nach_3_sekunden_ab_und_wiederholt_eine_zeitueberschreitung_nicht() {
		this.verzoegerung = 5_000;
		this.antwortKoerper = jevAntwort("{\"3\":1}");
		long start = System.nanoTime();

		assertThatThrownBy(() -> bewertungsAdapter(JEV, SCHLUESSEL).antwortBewerten(bewertung()))
			.isInstanceOf(KiModellNichtErreichbarException.class);

		long dauerMs = (System.nanoTime() - start) / 1_000_000;
		assertThat(dauerMs).isBetween(2_500L, 4_500L);
		assertThat(this.pfade).hasSize(1);
	}

	@Test
	@UseCase(id = "UC-002", scenario = "A5: Bewertung nicht möglich", requirements = { "NFR-006" })
	void haelt_mit_wiederholungen_das_gesamtbudget_von_10_sekunden_ein() {
		// jeder Versuch antwortet erst nach 2,8 s mit 429: drei volle Versuche plus Wartezeiten lägen über 10 s
		this.verzoegerung = 2_800;
		this.status = 429;
		this.antwortKoerper = "{\"error\":{\"message\":\"zu viele\",\"code\":429}}";
		long start = System.nanoTime();

		assertThatThrownBy(() -> bewertungsAdapter(JEV, SCHLUESSEL).antwortBewerten(bewertung()))
			.isInstanceOf(KiModellNichtErreichbarException.class);

		long dauerMs = (System.nanoTime() - start) / 1_000_000;
		assertThat(dauerMs).isLessThanOrEqualTo(10_500L);
		assertThat(this.pfade).hasSizeBetween(1, 3);
	}

	// ---------------------------------------------------------------- UC-003: Textreport über Spring AI

	private static final String TEXTREPORT_MODELL = "test/textreport";

	private OpenRouterKiModellClient textreportAdapter(String modell, String schluessel) {
		String basis = "http://127.0.0.1:" + this.stub.getAddress().getPort() + "/api/v1";
		return new OpenRouterKiModellClient(new KiProperties(basis, schluessel, new KiProperties.Modelle("", "", modell)),
				new ClassPathResource("prompts/fragenkatalog-entwurf.st"));
	}

	private static TextreportAnfrage textreportAnfrage() {
		return new TextreportAnfrage("Entwicklung",
				List.of(new HandlungsfeldAuswertung("Architektur & Engineering", new BigDecimal("3.5"),
						List.of(new BewerteteFrage("Wie begründen Sie eine Architekturentscheidung?",
								"Ich halte Kontext, Alternativen und Folgen fest.", 4),
								new BewerteteFrage("Wie gehen Sie mit technischen Schulden um?",
										"Wir planen Zeit für Aufräumarbeiten ein.", 3)))));
	}

	@Test
	@UseCase(id = "UC-003", scenario = "Main Success Scenario")
	void bildet_die_antwort_auf_ueberblick_und_abschnitte_je_handlungsfeld_ab() {
		this.antwortKoerper = chatAntwort("""
				{"ueberblick":"Sie zeigen solide Grundlagen.",
				 "abschnitte":[{"handlungsfeld":"Architektur & Engineering","wuerdigung":"Sie begründen nachvollziehbar.",
				  "naechsterSchritt":"Halten Sie Entscheidungen in ADRs fest."}]}""");

		Optional<Textreport> textreport = textreportAdapter(TEXTREPORT_MODELL, SCHLUESSEL)
			.textreportErstellen(textreportAnfrage());

		assertThat(textreport).contains(new Textreport("Sie zeigen solide Grundlagen.",
				List.of(new Abschnitt("Architektur & Engineering", "Sie begründen nachvollziehbar.",
						"Halten Sie Entscheidungen in ADRs fest."))));
	}

	@Test
	@UseCase(id = "UC-003", scenario = "Main Success Scenario", businessRules = { "BR-004" },
			requirements = { "NFR-001" })
	void sendet_nur_rolle_handlungsfelder_scores_fragen_antworten_und_stufen() {
		this.antwortKoerper = chatAntwort("{\"ueberblick\":\"\",\"abschnitte\":[]}");

		textreportAdapter(TEXTREPORT_MODELL, SCHLUESSEL).textreportErstellen(textreportAnfrage());

		assertThat(this.pfade).containsExactly("/api/v1/chat/completions");
		assertThat(this.koepfe).containsExactly("Bearer " + SCHLUESSEL);
		String gesendet = this.koerper.get(0);
		assertThat(gesendet).contains("\"model\":\"" + TEXTREPORT_MODELL + "\"")
			.contains("Rolle: Entwicklung")
			.contains("Architektur & Engineering")
			.contains("Score 3.5")
			.contains("Wie begründen Sie eine Architekturentscheidung?")
			.contains("Ich halte Kontext, Alternativen und Folgen fest.")
			.contains("Stufe: 4")
			.contains("Stufe: 3");
		// BR-004: keine Einladung, keine E-Mail-Adresse, keine Angabe zur Person
		assertThat(gesendet).doesNotContainIgnoringCase("e-mail")
			.doesNotContain("@")
			.doesNotContainIgnoringCase("einladung")
			.doesNotContainIgnoringCase("benutzer");
	}

	@Test
	@UseCase(id = "UC-003", scenario = "Main Success Scenario", businessRules = { "BR-003" })
	void verlangt_deutsch_wuerdigung_begruendung_naechsten_schritt_und_keine_woertlichen_antworten() {
		this.antwortKoerper = chatAntwort("{\"ueberblick\":\"\",\"abschnitte\":[]}");

		textreportAdapter(TEXTREPORT_MODELL, SCHLUESSEL).textreportErstellen(textreportAnfrage());

		assertThat(this.koerper.get(0)).contains("Schreibe auf Deutsch")
			.contains("würdige die Leistung")
			.contains("begründe die Einstufung")
			.contains("nächsten Schritt")
			.contains("Zitiere keine Antworten wörtlich")
			.contains("kein Bestandteil einer Leistungsbeurteilung");
	}

	@Test
	@UseCase(id = "UC-003", scenario = "A1: KI-Modell liefert keinen Textreport")
	void liefert_keinen_textreport_ohne_ueberblick_oder_ohne_abschnitt() {
		this.antwortKoerper = chatAntwort("{\"ueberblick\":\"\",\"abschnitte\":[{\"handlungsfeld\":\"A\",\"wuerdigung\":\"W\"}]}");
		assertThat(textreportAdapter(TEXTREPORT_MODELL, SCHLUESSEL).textreportErstellen(textreportAnfrage())).isEmpty();

		this.antwortKoerper = chatAntwort("{\"ueberblick\":\"Überblick\",\"abschnitte\":[]}");
		assertThat(textreportAdapter(TEXTREPORT_MODELL, SCHLUESSEL).textreportErstellen(textreportAnfrage())).isEmpty();
	}

	@Test
	@UseCase(id = "UC-003", scenario = "A1: KI-Modell liefert keinen Textreport")
	void meldet_nicht_erreichbar_wenn_der_dienst_beim_textreport_mit_fehler_antwortet() {
		this.status = 500;
		this.antwortKoerper = "{\"error\":{\"message\":\"interner-anbietertext\",\"code\":500}}";

		assertThatThrownBy(() -> textreportAdapter(TEXTREPORT_MODELL, SCHLUESSEL).textreportErstellen(textreportAnfrage()))
			.isInstanceOf(KiModellNichtErreichbarException.class)
			.hasMessage("Das KI-Modell ist nicht erreichbar.");
	}

	@Test
	@UseCase(id = "UC-003", scenario = "A1: KI-Modell liefert keinen Textreport")
	void meldet_nicht_erreichbar_wenn_der_textreport_nicht_lesbar_ist() {
		this.antwortKoerper = chatAntwort("das ist kein json");

		assertThatThrownBy(() -> textreportAdapter(TEXTREPORT_MODELL, SCHLUESSEL).textreportErstellen(textreportAnfrage()))
			.isInstanceOf(KiModellNichtErreichbarException.class)
			.hasMessage("Das KI-Modell ist nicht erreichbar.");
	}

	@Test
	@UseCase(id = "UC-003", scenario = "A1: KI-Modell liefert keinen Textreport")
	void meldet_nicht_erreichbar_wenn_beim_textreport_keine_verbindung_besteht() {
		OpenRouterKiModellClient adapter = textreportAdapter(TEXTREPORT_MODELL, SCHLUESSEL);
		this.stub.stop(0);

		assertThatThrownBy(() -> adapter.textreportErstellen(textreportAnfrage()))
			.isInstanceOf(KiModellNichtErreichbarException.class)
			.hasMessage("Das KI-Modell ist nicht erreichbar.");
	}

	@Test
	@UseCase(id = "UC-003", scenario = "A1: KI-Modell liefert keinen Textreport", requirements = { "NFR-006" })
	void meldet_nicht_erreichbar_ohne_textreport_innerhalb_von_10_sekunden() {
		// der Dienst antwortet erst nach 12 s
		this.verzoegerung = 12_000;
		this.antwortKoerper = chatAntwort("{\"ueberblick\":\"zu spät\",\"abschnitte\":[]}");
		long start = System.nanoTime();

		assertThatThrownBy(() -> textreportAdapter(TEXTREPORT_MODELL, SCHLUESSEL).textreportErstellen(textreportAnfrage()))
			.isInstanceOf(KiModellNichtErreichbarException.class)
			.hasMessage("Das KI-Modell ist nicht erreichbar.");

		long dauerMs = (System.nanoTime() - start) / 1_000_000;
		assertThat(dauerMs).isBetween(9_500L, 11_000L);
		// ohne Wiederholung
		assertThat(this.pfade).hasSize(1);
	}

	@Test
	@UseCase(id = "UC-003", scenario = "A1: KI-Modell liefert keinen Textreport")
	void ruft_ohne_textreport_modell_oder_schluessel_das_kimodell_nicht_auf() {
		assertThatThrownBy(() -> textreportAdapter("", SCHLUESSEL).textreportErstellen(textreportAnfrage()))
			.isInstanceOf(KiModellNichtErreichbarException.class)
			.hasMessage("Das KI-Modell ist nicht erreichbar.");
		assertThatThrownBy(() -> textreportAdapter(TEXTREPORT_MODELL, " ").textreportErstellen(textreportAnfrage()))
			.isInstanceOf(KiModellNichtErreichbarException.class)
			.hasMessage("Das KI-Modell ist nicht erreichbar.");

		assertThat(this.pfade).isEmpty();
	}

}
