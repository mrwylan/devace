# ADR-004: Jev über OpenRouter ordnet die Antworten einer Stufe zu, mit jev-latest als Standard

**Status:** Accepted  
**Date:** 2026-11-11  
**Requirements:** NFR-009, NFR-006, NFR-001, C-005, C-001, FR-006, FR-016  
**Supersedes:** ADR-003

## Context

UC-002 Schritt 6 und UC-009 Schritt 7 legen eine Antwort dem KI-Modell vor, das sie nach den Stufenvorgaben der Frage
einer Stufe von 0 bis 5 zuordnet (C-005). Mindestens 90 % der Testfälle müssen die erwartete Stufe erhalten
(NFR-009), 95 % der Antworten des Systems erscheinen innerhalb von 10 Sekunden (NFR-006), und keine Anfrage enthält
einen Identifikator des Teilnehmers (NFR-001). Die Textantworten unterliegen den Firmenweisungen zum Umgang mit KI
(C-001).

Die Zuordnung ist keine Textaufgabe, sondern eine Entscheidung auf einer festen Skala. Bisher erledigt sie ein
Chat-Modell über Spring AI mit strukturierter Ausgabe (UC-002, Commit auf `feature/uc-002-umsetzung`). Ein Chat-Modell
liefert keine Wahrscheinlichkeit und kann eine Stufe außerhalb der Skala nennen.

Jev (TypeSafe AI) ist ein Entscheidungsmodell: Es beantwortet typisierte Fragen zu einem Text mit kalibrierten
Wahrscheinlichkeiten und schreibt keinen Text. Der Fragetyp Score ordnet auf einer Skala mit 2 bis 10 Stufen ein. Nach
Angaben des Herstellers kostet es nur die Eingabe-Tokens und antwortet in 70 bis 500 ms. OpenRouter bietet Jev als
Proxy an: `POST https://openrouter.ai/api/v1/systemone` mit dem Schema `state`, `model`, `questions`; das ist kein
`/chat/completions`, Spring AI und die OpenAI-SDKs sprechen es nicht. Es gibt kein Java-SDK.

CLAUDE.md §1 legt für den Zugang zu KI-Modellen „Spring AI only, routed through OpenRouter“ fest. OpenRouter als
einziges Gateway bleibt mit dieser Entscheidung erhalten (OQ-044); die Ausnahme von „Spring AI only“ ist der Grund für
diesen ADR. ADR-002 (das OpenAI-SDK nur als Abhängigkeit von Spring AI) bleibt unberührt: Der Zugang zu Jev braucht
kein Anbieter-SDK.

## Decision

- Die Zuordnung einer Antwort zu einer Stufe (UC-002 Schritt 6, UC-009 Schritt 7) übernimmt Jev mit dem Fragetyp
  Score auf einer Skala von 6 Stufen, Stufe 0 bis 5. Als Stufe gilt die mit der höchsten Wahrscheinlichkeit; die
  Wahrscheinlichkeiten werden über ihren Namen gelesen, nie über die Reihenfolge.
- Der Zugang läuft ausschließlich über OpenRouter: `POST {devace.ki.base-url}/systemone` (heute
  `https://openrouter.ai/api/v1/systemone`) mit dem Schlüssel aus `OPENROUTER_API_KEY` als Bearer-Token. Ein direkter
  Aufruf von `api.typesafe.ai` ist ausgeschlossen.
- Der Adapter im Client-Layer (`info.wylan.devace.client.kimodell`) ruft Jev mit `RestClient` und eigenen DTOs auf,
  hinter dem bestehenden Port `KiModellClient.antwortBewerten`. Spring AI bleibt für die Modelle, die Text erzeugen
  (UC-006 Fragenkatalog, UC-003 Textreport).
- Standard ist der Alias `~typesafe/jev-latest`; über `DEVACE_KI_MODELL_BEWERTUNG` lässt sich eine konkrete
  Version pinnen (z. B. `typesafe/jev-1.14-20261014`). Das `model`-Feld der Antwort, also die tatsächlich verwendete
  Version, wird protokolliert, der Antworttext nicht.
- Die Anfrage enthält nur den Text der Antwort als `state` und Frage samt Stufenvorgaben in `instructions` und
  `criteria` (die Schlüssel der Fragen gehen nicht ans Modell). Sie enthält keine Einladung, keine E-Mail-Adresse
  und keine andere Angabe zur Person (NFR-001, UC-002 BR-007).
- Jeder Versuch hat ein Zeitlimit von 3 Sekunden, alle Versuche und Wartezeiten zusammen höchstens 10 Sekunden
  (NFR-006). Bei 429 und 529 wiederholt der Adapter mit wachsender Wartezeit (0,5 s, 1,5 s), höchstens dreimal und
  nur, solange das Gesamtbudget reicht. Eine Zeitüberschreitung, 401, 422 und jede andere Störung sind UC-002 A5:
  Die Antwort erhält Stufe 0.
- Vor dem produktiven Einsatz gelten zwei Bedingungen: (1) ein Testlauf von UC-009 mit deutschsprachigen Testfällen
  erreicht mindestens 90 % (NFR-009); (2) die Verarbeitung der Antworttexte in den USA ohne Zero Data Retention ist
  nach den Firmenweisungen (C-001) geklärt.

## Consequences

- Einfacher: Die Zuordnung liegt immer auf der Skala 0 bis 5 (C-005), mit einer Wahrscheinlichkeit je Stufe; das
  Modell ist schneller und günstiger als ein Chat-Modell, was NFR-006 und NFR-008 entlastet.
- Einfacher: UC-009 misst mit denselben Aufrufen, ob die Zuordnung fair ist (NFR-009); die Abweichungen lassen sich
  als Grundlage für die Verfeinerung der Stufenvorgaben auswerten.
- Schwerer: Englisch ist die Hauptsprache von Jev; für deutsche Antworten gibt der Hersteller eine geringere
  Genauigkeit an. Ob NFR-009 erreicht wird, zeigt erst ein Testlauf von UC-009.
- Schwerer: Der Dienst läuft an der US-Westküste, Zero Data Retention gibt es nur für Enterprise-Kunden. Die Fassung
  der Firmenweisungen ist noch offen (OQ-010); ohne Klärung darf Jev keine echten Antworten bewerten.
- Schwerer: Jev ist auf OpenRouter im Beta-Stadium; das Konto muss für das Modell freigeschaltet sein (sonst 404).
- Schwerer: Mit `jev-latest` als Standard wechselt die Version mit jeder Veröffentlichung von TypeSafe ohne eigenes
  Zutun. Die Zuordnung kann sich dadurch ändern, ohne dass devace sich ändert; das protokollierte `model`-Feld zeigt,
  welche Version bewertet hat. Wer die Fairness nach NFR-009 an eine geprüfte Version binden will, pinnt sie und
  prüft den Wechsel mit UC-009.
- Schwerer: Ein zweiter Zugangsweg neben Spring AI im Client-Layer, mit eigenem Request-Schema, eigenen DTOs und
  eigenen Adaptertests; `spring-boot-starter-restclient` kommt als Abhängigkeit dazu. CLAUDE.md §1 und
  `docs/architecture/stack.md` werden um die Ausnahme ergänzt.
- Die Wahrscheinlichkeit der zugeordneten Stufe wird nicht gespeichert und steuert keinen Ablauf; das Entity-Modell
  (BEWERTUNG) und UC-002 kennen dafür nichts. Eine Schwelle mit Nachprüfung wäre eine neue Anforderung.
- Verworfen: ein Chat-Modell über Spring AI mit strukturierter Ausgabe (bisheriger Stand) — keine Wahrscheinlichkeiten,
  Stufen außerhalb der Skala möglich, teurer.
- Verworfen: Jev direkt über `api.typesafe.ai` — verletzt OpenRouter als einziges Gateway (CLAUDE.md §1, OQ-044).
- Ersetzt ADR-003 in einem Punkt: Dort war `~typesafe/jev-latest` ausgeschlossen und eine gepinnte Version Pflicht.
  Jetzt ist `latest` der Standard, das Pinnen bleibt über die Umgebung möglich.
- Verworfen: der Fragetyp Choice mit sechs Optionen — er verliert die Ordnung der Stufen, die Score ausdrückt.
