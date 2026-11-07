# Vision: devace – KI-gestütztes Kompetenz-Assessment für Entwicklungsteams

**Status:** Entwurf 0.4 – zur gemeinsamen Erarbeitung
**Name:** devace
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

devace ist ein **KI-gestütztes Assessment**. Es erstellt einen Fragenkatalog, der die sechs Handlungsfelder gezielt prüft, führt die Befragten im Dialog durch die Fragen und bewertet die Antworten nach einem festen Entscheidungsmodell.

Das Ergebnis je Mitarbeitergruppe ist ein **Radar-Chart** über die sechs Handlungsfelder. Das Gespräch dreht sich damit um Handlungsfelder und nächste Entwicklungsschritte, nicht um einzelne Werkzeuge.

## 4. Begriffe

| Begriff | Bedeutung |
|---|---|
| **MAG** (Mitarbeitergruppe) | Setzt sich aus der Matrix zusammen: Scrum Team, Linienteam und Rolle (z. B. Entwicklung, DevEx). |
| **Handlungsfeld** | Eine von sechs Kompetenzdimensionen, die das Assessment bewertet (§5). |
| **Stufe** | Ausprägung je Handlungsfeld auf der Skala 0–5 (§6). |
| **Entscheidungsmodell** | Festes Regelwerk, nach dem die KI aus den Antworten die Stufe ableitet. |

## 5. Zielgruppen und Nutzen

| Rolle | Nutzen |
|---|---|
| Teamleitung | Überblick über Stand und Lücken je MAG; Grundlage für Entwicklungsgespräche |
| Entwickler, Tester, Scrum Master | Eigene Einschätzung im Assessment; klare Sicht auf das Zielbild |
| Schulungsverantwortliche | Schulungsbedarf anhand der Lücken priorisieren |

## 6. Die fünf Handlungsfelder

Die Bewertung erfolgt getrennt je Rolle (siehe §11, Entscheidung 3).

| # | Handlungsfeld | Fokus | Inhalt |
|---|---|---|---|
| 1 | Domänenwissen & Business Alignment | Verstehen, wie Transgourmet arbeitet | Betriebseigenheiten, Kernsysteme (ProlixPro, ProShop), Prozesse der Warenwirtschaft |
| 2 | Architektur & Engineering | Zertifikatswissen (iSAQB Foundation) im Alltag anwenden | Architekturprinzipien, Backend- und Frontend-Entwicklung; bei Scrum Mastern: technisches Verständnis für Impediments |
| 3 | Qualität & Testing | iSTQB Foundation anwenden | Teststrategien, Testautomatisierung, Shift-Left; Tester treiben, Entwickler ziehen mit, Scrum Master achten auf die Definition of Done |
| 4 | Security & DevSecOps | Momentum aus dem CTF-Labor halten | Secure Coding, Schwachstellenanalyse, CI/CD-Pipelines, Automatisierung, Container- und Plattformkenntnisse |
| 5 | Methodik & Agile Delivery | Effizient und transparent Wert liefern | Agile Praktiken, Requirements Engineering, Teamzusammenarbeit, Flow-Metriken |
| 6 | Future Skills & AI | Blick Richtung 2030 | Agentenbasierte Entwicklung, KI-Werkzeuge im Alltag |

**Hinweis zum Umfang:** Die Handlungsfelder beschreiben Kompetenzen der Teams, nicht die Technologien, mit denen das Assessment selbst gebaut wird. Das Assessment deckt alle Technologien ab, die in den Teams tatsächlich vorkommen, auch Altsysteme und Sprachen außerhalb des Zielstacks. Beispiel: Eine Frage zu PHP gehört in den Katalog, wenn die Teams damit arbeiten.

Die Zuordnung der Matrix-Spalten zu den Handlungsfeldern ist noch nicht erarbeitet (siehe §11).

## 7. Bewertungsskala

Die Stufen gelten je Handlungsfeld. Die Beschreibungen sind die verbindliche Grundlage für das Entscheidungsmodell.

| Stufe | Bezeichnung | Beschreibung |
|---|---|---|
| 0 | Nie gehört | Ich habe dieses Thema noch nie gehört oder bewusst wahrgenommen. Ich kenne keine Begriffe oder Zusammenhänge dazu. |
| 1 | Schon mal gehört | Ich habe davon gehört und habe eine sehr grobe Vorstellung, worum es geht. Ich könnte es nicht erklären oder anwenden. |
| 2 | Grundverständnis | Ich kenne die Grundlagen, habe erste Erfahrungen gesammelt, kann einfachen Erklärungen folgen, brauche aber Anleitung bei der Umsetzung. |
| 3 | Sicherer Umgang | Ich kann selbstständig mit dem Thema arbeiten, typische Aufgaben lösen und weiß, wo ich mir Hilfe holen kann. Ich könnte Kolleg:innen erste Fragen beantworten. |
| 4 | Fortgeschrittene Expertise | Ich habe vertiefte Kenntnisse, kann komplexe Probleme lösen und andere anleiten oder coachen. Ich erkenne Zusammenhänge mit angrenzenden Themen. |
| 5 | Expert:in / Lehrniveau | Ich beherrsche das Thema in der Tiefe, kann es strukturiert vermitteln, weiterentwickeln oder an einer Hochschule unterrichten. Ich werde im Team als Ansprechperson dafür gesehen. |

## 8. Ablauf des Assessments (Fachsicht)

1. **Fragenkatalog:** Das System erstellt mit KI-Unterstützung einen Fragenkatalog, der die Handlungsfelder gezielt prüft.
2. **Dialog:** Die befragte Person beantwortet die Fragen im Dialog mit dem System. Antworten sind Freitext.
3. **Bewertung:** Die KI leitet aus den Antworten je Handlungsfeld eine Stufe ab. Grundlage ist das Entscheidungsmodell und die Stufenbeschreibungen aus §7.
4. **Ergebnis:** Die Stufen werden je MAG und Rolle als Radar-Chart dargestellt.

## 9. Erfolgskriterien (vorläufig)

1. Für jede MAG und Rolle lässt sich ein Radar-Chart mit den sechs Handlungsfeldern erzeugen.
2. Die Verdichtung von Matrix-Spalten zu Handlungsfeldern ist dokumentiert und nachvollziehbar.
3. Die Stufen aus dem Dialog stimmen mit einer unabhängigen Einschätzung überein. Das wird im Pilot geprüft.
4. Ein Pilot mit einer MAG zeigt, dass die Teamleitung die Gespräche auf Handlungsfelder und Entwicklungsschritte fokussieren kann.
5. Das Assessment erfasst Kompetenzen, die die Matrix heute nicht sichtbar macht.

## 10. Fachliche Rahmenbedingungen

- **Personendaten:** Assessment-Ergebnisse beziehen sich auf einzelne Mitarbeitende. Zugriff, Aufbewahrung und Löschfristen sind vor dem Pilot zu klären.
- **Antworten an externe KI:** Die Antworten werden zur Bewertung an KI-Modelle übergeben. Ob und in welchem Umfang das zulässig ist, muss geklärt sein, bevor der Pilot startet.
- **Zweck:** Die Ergebnisse dienen der Entwicklung und Schulungsplanung. Sie sind (Vorschlag) keine Grundlage für Leistungsbeurteilungen.

## 11. Offene Fragen und Nicht-Ziele

### Nicht-Ziele (vorläufig)

- Verwaltung von Zertifikaten oder Schulungsbuchungen.
- Anbindung an HR-Systeme.
- Pflege der Kompetenzmatrix selbst. Die Matrix bleibt die fachliche Quelle.
- **Später:** Selbst- und Fremdbeurteilung als ergänzende Sicht. Das ist für eine spätere Phase vorgesehen.

### Offene Fragen

1. **Entscheidungsmodell:** Wie werden die Stufen aus den Antworten abgeleitet? Welche Kriterien gelten je Stufe?
2. **Prüfung der KI-Bewertung:** Wer prüft die von der KI vergebene Stufe, bevor sie gespeichert oder der Teamleitung gezeigt wird?
3. **Datenschutz:** Wer sieht Einzelergebnisse, wie lange werden sie gespeichert, und ist die Nutzung für Leistungsbeurteilungen ausgeschlossen?
4. **Externe KI:** Dürfen Antworttexte von Mitarbeitenden an externe KI-Modelle übergeben werden? Falls ja: unter welchen Bedingungen?
5. **Umfang des Fragenkatalogs:** Sollen alle Technologien der Matrix abgedeckt werden, auch Altsysteme wie PHP, oder nur die aktuellen Schwerpunkte?
6. **Pilot:** Welche MAG und welcher Zeitrahmen?

---

## Änderungsverlauf

| Version | Datum | Änderung |
|---|---|---|
| 0.1 | 2026-11-07 | Erster Entwurf aus der Vorlage der Teamleitung |
| 0.2 | 2026-11-07 | Umsetzungsdetails entfernt; Fachinhalt getrennt von technischer Umsetzung |
| 0.3 | 2026-11-07 | Antworten eingearbeitet: KI-gestützter Fragenkatalog und Dialog, Entscheidungsmodell, getrennte Bewertung je Rolle, MAG-Definition, Skala 0–5 mit Stufenbeschreibungen, Handlungsfeld „Haltung“ gestrichen |
| 0.4 | 2026-11-07 | Name „devace“ festgelegt |
