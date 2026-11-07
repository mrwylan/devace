# Vision: devace – Kompetenz-Assessment für Entwicklungsteams

**Status:** Entwurf 0.1 – zur gemeinsamen Erarbeitung
**Arbeitstitel:** devace (zu bestätigen)
**Quelle:** Vorschlag der Teamleitung zu 6 Handlungsfeldern und der Bewertungsskala; Detailmatrix mit über 150 Spalten.

Offene Punkte sind in §10 gesammelt. Alles andere ist als Entwurf zu verstehen.

---

## 1. Ausgangslage

- Die Teams pflegen eine detaillierte Kompetenzmatrix mit über 150 Spalten (Frameworks, Versionen, Tools, Zertifikate).
- Die Detailtiefe ist für Gespräche auf Teamebene ungeeignet: Man diskutiert einzelne Framework-Versionen statt Handlungsfelder.
- Das Zielbild 2030 setzt Schwerpunkte, die in den letzten Schulungen aufgebaut wurden: Software-Architektur (iSAQB), Testing (iSTQB), Security (Capture-the-Flag-Labor), agentenbasierte Entwicklung.

## 2. Problem

Die Kompetenzmatrix zeigt viele Einzelwerte, aber kein Gesamtbild. Die Teamleitung kann Stärken und Lücken je Mitarbeitergruppe (MAG) nicht auf einen Blick erkennen. Entwicklungsgespräche und Schulungsplanung bleiben dadurch auf Detailwissen stehen, statt auf Handlungsfelder zu zielen.

## 3. Vision

devace überführt die bestehende Kompetenzmatrix in ein **interaktives Assessment**. Die Detaildaten werden auf **sechs Handlungsfelder** aggregiert und je Mitarbeitergruppe als **Radar-Chart** dargestellt.

Das Gespräch dreht sich damit um Handlungsfelder und nächste Entwicklungsschritte, nicht um einzelne Werkzeuge.

## 4. Zielgruppen und Nutzen

| Rolle | Nutzen |
|---|---|
| Teamleitung | Überblick über Stand und Lücken je MAG; Grundlage für Entwicklungsgespräche |
| Entwickler, Tester, Scrum Master | Eigene Einschätzung im Assessment; klare Sicht auf das Zielbild |
| Schulungsverantwortliche | Schulungsbedarf anhand der Lücken priorisieren |

## 5. Die sechs Handlungsfelder

| # | Handlungsfeld | Fokus | Inhalt |
|---|---|---|---|
| 1 | Domänenwissen & Business Alignment | Verstehen, wie Transgourmet arbeitet | Betriebseigenheiten, Kernsysteme (ProlixPro, ProShop), Prozesse der Warenwirtschaft |
| 2 | Architektur & Engineering | Zertifikatswissen (iSAQB Foundation) im Alltag anwenden | Architekturprinzipien, Backend (Spring Boot), Frontend (Vaadin); bei Scrum Mastern: technisches Verständnis für Impediments |
| 3 | Qualität & Testing | iSTQB Foundation anwenden | Teststrategien, Testautomatisierung, Shift-Left; Tester treiben, Entwickler ziehen mit, Scrum Master achten auf die Definition of Done |
| 4 | Security & DevSecOps | Momentum aus dem CTF-Labor halten | Secure Coding, Schwachstellenanalyse, CI/CD-Pipelines, Automatisierung (Docker, OpenShift) |
| 5 | Methodik & Agile Delivery | Effizient und transparent Wert liefern | Agile Praktiken, Requirements Engineering, Teamzusammenarbeit, Flow-Metriken |
| 6 | Future Skills & AI | Blick Richtung 2030 | Agentenbasierte Entwicklung, KI-Werkzeuge im Alltag (z. B. Copilot), Offenheit für neue Architekturparadigmen |

Die Zuordnung der Matrix-Spalten zu den Handlungsfeldern ist noch nicht erarbeitet (siehe §10).

## 6. Bewertungsskala

Die bestehende Skala aus Tabelle 1 wird übernommen:

| Stufe | Bedeutung |
|---|---|
| 0 | Nie gehört |
| 1–4 | *noch zu definieren* |
| 5 | Expert / Lehrniveau |

## 7. Erfolgskriterien (vorläufig)

1. Für jede MAG lässt sich ein Radar-Chart mit den sechs Handlungsfeldern erzeugen.
2. Die Aggregation von Matrix-Spalten zu Handlungsfeldern ist dokumentiert und nachvollziehbar.
3. Ein Pilot mit einer MAG zeigt, dass die Teamleitung die Gespräche auf Handlungsfelder und Entwicklungsschritte fokussieren kann.
4. Das Assessment deckt die Fragen ab, die aus der Matrix nicht beantwortbar sind (zu prüfen nach dem Pilot).

## 8. Rahmenbedingungen

**Technik** (verbindlich, siehe `CLAUDE.md`):
Java 25, Spring Boot 4.1.2, Maven, Vaadin 25 (Flow), PostgreSQL mit Flyway, jOOQ, Spring AI über OpenRouter, Container-Image über Spring Boot Buildpacks, GitHub Actions.

**Datenschutz** (Vorschlag, noch zu bestätigen):
- Assessment-Daten sind Personendaten. Zugriff, Aufbewahrung und Löschfristen müssen vor dem Pilot geklärt sein.
- Die Ergebnisse dienen der Entwicklung und Schulungsplanung, nicht als Grundlage für Leistungsbeurteilungen.

## 9. Nicht Ziel (vorläufig)

- Verwaltung von Zertifikaten oder Schulungsbuchungen.
- Integration in HR-Systeme.
- Pflege der Detailmatrix selbst (die Matrix bleibt Quelle, devace liest sie aus).

## 10. Offene Fragen

1. **Bewertungsart:** Selbsteinschätzung, Fremdbeurteilung durch die Teamleitung oder beides?
2. **Abkürzung MAG:** Ist „Mitarbeitergruppe“ gemeint? Bitte bestätigen.
3. **Rollen in Handlungsfeld 2 und 3:** Sollen Entwickler, Tester und Scrum Master getrennt bewertet werden? Die Felder vermischen heute mehrere Rollen.
4. **Handlungsfeld 6:** „Offenheit für neue Architekturparadigmen“ ist eher eine Haltung als Wissen. Eigene Achse, eigene Frage oder streichen?
5. **Zielplattform:** Handlungsfeld 4 nennt OpenShift und Docker. Die Pipeline in `CLAUDE.md` publiziert nach GHCR. Welche Plattform gilt für den Betrieb?
6. **Skala 1–4:** Die Zwischenstufen fehlen in der Vorlage. Bitte definieren.
7. **Datenschutz:** Wer sieht Einzelergebnisse, wie lange werden sie gespeichert, und ist die Nutzung für Leistungsbeurteilungen ausgeschlossen?
8. **Rolle von KI:** Soll das Assessment Fragen per LLM (Spring AI über OpenRouter) generieren oder Antworten auswerten? Oder bleibt es ein klassischer Fragebogen? Das bestimmt den Umfang.
9. **Pilot:** Welche MAG und welcher Zeitrahmen?
10. **Name:** Ist „devace“ der endgültige Name?

---

## Änderungsverlauf

| Version | Datum | Änderung |
|---|---|---|
| 0.1 | 2026-11-07 | Erster Entwurf aus der Vorlage der Teamleitung |
