# Vision: devace – Kompetenz-Assessment für Entwicklungsteams

**Status:** Entwurf 0.2 – zur gemeinsamen Erarbeitung
**Arbeitstitel:** devace (zu bestätigen)
**Geltungsbereich:** Fachlicher Inhalt. Technische Umsetzung steht nicht in diesem Dokument.

Offene Punkte sind in §10 gesammelt.

---

## 1. Ausgangslage

- Die Teams pflegen eine detaillierte Kompetenzmatrix mit über 150 Spalten. Sie erfasst Frameworks, Versionen, Tools und Zertifikate.
- Die Detailtiefe ist für Gespräche auf Teamebene ungeeignet: Man diskutiert einzelne Werkzeuge statt Handlungsfelder.
- Das Zielbild 2030 setzt Schwerpunkte, die in den letzten Schulungen aufgebaut wurden: Software-Architektur (iSAQB), Testing (iSTQB), Security (Capture-the-Flag-Labor), agentenbasierte Entwicklung.

## 2. Problem

Die Kompetenzmatrix zeigt viele Einzelwerte, aber kein Gesamtbild. Die Teamleitung kann Stärken und Lücken je Mitarbeitergruppe (MAG) nicht auf einen Blick erkennen. Entwicklungsgespräche und Schulungsplanung bleiben dadurch auf Detailwissen stehen, statt auf Handlungsfelder zu zielen.

## 3. Vision

devace überführt die bestehende Kompetenzmatrix in ein **interaktives Assessment**. Die Detaildaten werden auf **sechs Handlungsfelder** verdichtet und je Mitarbeitergruppe als **Radar-Chart** dargestellt.

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
| 2 | Architektur & Engineering | Zertifikatswissen (iSAQB Foundation) im Alltag anwenden | Architekturprinzipien, Backend- und Frontend-Entwicklung; bei Scrum Mastern: technisches Verständnis für Impediments |
| 3 | Qualität & Testing | iSTQB Foundation anwenden | Teststrategien, Testautomatisierung, Shift-Left; Tester treiben, Entwickler ziehen mit, Scrum Master achten auf die Definition of Done |
| 4 | Security & DevSecOps | Momentum aus dem CTF-Labor halten | Secure Coding, Schwachstellenanalyse, CI/CD-Pipelines, Automatisierung, Container- und Plattformkenntnisse |
| 5 | Methodik & Agile Delivery | Effizient und transparent Wert liefern | Agile Praktiken, Requirements Engineering, Teamzusammenarbeit, Flow-Metriken |
| 6 | Future Skills & AI | Blick Richtung 2030 | Agentenbasierte Entwicklung, KI-Werkzeuge im Alltag, Offenheit für neue Architekturparadigmen |

**Hinweis zum Umfang:** Die Handlungsfelder beschreiben Kompetenzen der Teams, nicht die Technologien, mit denen das Assessment selbst gebaut wird. Das Assessment deckt alle Technologien ab, die in den Teams tatsächlich vorkommen, auch Altsysteme und Sprachen außerhalb des Zielstacks. Beispiel: Eine Frage zu PHP gehört in den Katalog, wenn die Teams damit arbeiten.

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
2. Die Verdichtung von Matrix-Spalten zu Handlungsfeldern ist dokumentiert und nachvollziehbar.
3. Ein Pilot mit einer MAG zeigt, dass die Teamleitung die Gespräche auf Handlungsfelder und Entwicklungsschritte fokussieren kann.
4. Das Assessment erfasst Kompetenzen, die die Matrix heute nicht sichtbar macht. Das wird nach dem Pilot geprüft.

## 8. Fachliche Rahmenbedingungen

- **Personendaten:** Assessment-Ergebnisse beziehen sich auf einzelne Mitarbeitende. Zugriff, Aufbewahrung und Löschfristen sind vor dem Pilot zu klären.
- **Zweck:** Die Ergebnisse dienen der Entwicklung und Schulungsplanung. Sie sind (Vorschlag) keine Grundlage für Leistungsbeurteilungen.

## 9. Nicht-Ziele (vorläufig)

- Verwaltung von Zertifikaten oder Schulungsbuchungen.
- Anbindung an HR-Systeme.
- Pflege der Kompetenzmatrix selbst. Die Matrix bleibt die fachliche Quelle; ihre Pflege ist nicht Teil des Assessments.

## 10. Offene Fragen

1. **Bewertungsart:** Selbsteinschätzung, Fremdbeurteilung durch die Teamleitung oder beides?
2. **Abkürzung MAG:** Ist „Mitarbeitergruppe“ gemeint? Bitte bestätigen.
3. **Rollen in Handlungsfeld 2 und 3:** Sollen Entwickler, Tester und Scrum Master getrennt bewertet werden? Die Felder vermischen heute mehrere Rollen.
4. **Handlungsfeld 6:** „Offenheit für neue Architekturparadigmen“ ist eher eine Haltung als Wissen. Eigene Achse, eigene Frage oder streichen?
5. **Umfang des Fragenkatalogs:** Sollen alle Technologien der Matrix abgedeckt werden, auch Altsysteme wie PHP, oder nur die aktuellen Schwerpunkte?
6. **Skala 1–4:** Die Zwischenstufen fehlen in der Vorlage. Bitte definieren.
7. **Datenschutz:** Wer sieht Einzelergebnisse, wie lange werden sie gespeichert, und ist die Nutzung für Leistungsbeurteilungen ausgeschlossen?
8. **Fragenform:** Fester Fragenkatalog, oder sollen Fragen je nach bisherigen Antworten variieren und Freitextantworten ausgewertet werden?
9. **Pilot:** Welche MAG und welcher Zeitrahmen?
10. **Name:** Ist „devace“ der endgültige Name?

---

## Änderungsverlauf

| Version | Datum | Änderung |
|---|---|---|
| 0.1 | 2026-11-07 | Erster Entwurf aus der Vorlage der Teamleitung |
| 0.2 | 2026-11-07 | Umsetzungsdetails entfernt; Fachinhalt getrennt von technischer Umsetzung; Fragen zu Technologien auf Fachfragen umformuliert |
