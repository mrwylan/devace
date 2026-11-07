# Vision: devace – KI-gestütztes Kompetenz-Assessment für Entwicklungsteams

**Status:** Entwurf 0.5 – zur gemeinsamen Erarbeitung
**Name:** devace
**Geltungsbereich:** Fachlicher Inhalt. Technische Umsetzung steht nicht in diesem Dokument.

Offene Punkte sind in §11 gesammelt.

---

## 1. Ausgangslage

- Die Teams pflegen eine detaillierte Kompetenzmatrix mit über 150 Spalten. Sie erfasst Frameworks, Versionen, Tools und Zertifikate.
- Die Detailtiefe ist für Gespräche auf Teamebene ungeeignet: Man diskutiert einzelne Werkzeuge statt Handlungsfelder.
- Das Zielbild 2030 setzt Schwerpunkte, die in den letzten Schulungen aufgebaut wurden: Software-Architektur (iSAQB), Testing (iSTQB), Security (Capture-the-Flag-Labor), agentenbasierte Entwicklung.

## 2. Problem

Die Kompetenzmatrix zeigt viele Einzelwerte, aber kein Gesamtbild. Die Teamleitung kann Stärken und Lücken je Mitarbeitergruppe (MAG) nicht auf einen Blick erkennen. Entwicklungsgespräche und Schulungsplanung bleiben dadurch auf Detailwissen stehen, statt auf Handlungsfelder zu zielen.

## 3. Vision

devace ist ein **KI-gestütztes Assessment**. Es erstellt einen Fragenkatalog, der die Handlungsfelder gezielt prüft, führt die Befragten im Dialog durch die Fragen und bewertet die Antworten nach einem festen Entscheidungsmodell.

Das Ergebnis je Mitarbeitergruppe ist ein **Radar-Chart** über die Handlungsfelder. Das Gespräch dreht sich damit um Handlungsfelder und nächste Entwicklungsschritte, nicht um einzelne Werkzeuge.

## 4. Begriffe

| Begriff | Bedeutung |
|---|---|
| **MAG** (Mitarbeitergruppe) | Setzt sich aus der Matrix zusammen: Scrum Team, Linienteam und Rolle (z. B. Entwicklung, DevEx). |
| **Handlungsfeld** | Eine von fünf Kompetenzdimensionen, die das Assessment bewertet (§6). |
| **Stufe** | Ausprägung je Handlungsfeld auf der Skala 0–5 (§7). |
| **Entscheidungsmodell** | Je Handlungsfeld und Frage ist vorgegeben, welcher Stufe eine Antwort entspricht. Die KI ordnet die Antworten diesen Vorgaben zu. |

## 5. Zielgruppen und Nutzen

| Rolle | Nutzen |
|---|---|
| Teamleitung | Überblick über Stand und Lücken je MAG; Grundlage für Entwicklungsgespräche |
| Entwickler, Tester, Scrum Master | Eigene Einschätzung im Assessment; klare Sicht auf das Zielbild |
| Schulungsverantwortliche | Schulungsbedarf anhand der Lücken priorisieren |

## 6. Die fünf Handlungsfelder

Die Bewertung erfolgt getrennt je Rolle.

| # | Handlungsfeld | Fokus | Inhalt |
|---|---|---|---|
| 1 | Domänenwissen & Business Alignment | Verstehen, wie Transgourmet arbeitet | Betriebseigenheiten, Kernsysteme (ProlixPro, ProShop), Prozesse der Warenwirtschaft |
| 2 | Architektur & Engineering | Zertifikatswissen (iSAQB Foundation) im Alltag anwenden | Architekturprinzipien, Backend- und Frontend-Entwicklung; bei Scrum Mastern: technisches Verständnis für Impediments |
| 3 | Qualität & Testing | iSTQB Foundation anwenden | Teststrategien, Testautomatisierung, Shift-Left; Tester treiben, Entwickler ziehen mit, Scrum Master achten auf die Definition of Done |
| 4 | Security & DevSecOps | Momentum aus dem CTF-Labor halten | Secure Coding, Schwachstellenanalyse, CI/CD-Pipelines, Automatisierung, Container- und Plattformkenntnisse |
| 5 | Methodik & Agile Delivery | Effizient und transparent Wert liefern | Agile Praktiken, Requirements Engineering, Teamzusammenarbeit, Flow-Metriken |
| 6 | Future Skills & AI | Blick Richtung 2030 | Agentenbasierte Entwicklung, KI-Werkzeuge im Alltag |

**Umfang des Fragenkatalogs:** Der Katalog fokussiert Technologien und Grundlagen mit Alltagsrelevanz für die Teams. Nicht jede Technologie der Matrix wird abgefragt. Maßstab für die Aufnahme ist die Relevanz im Alltag, nicht der Eintrag in der Matrix.

**Trennung zur Umsetzung:** Die Handlungsfelder beschreiben Kompetenzen der Teams, nicht die Technologien, mit denen devace selbst gebaut wird.

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

1. **Fragenkatalog:** Das System erstellt mit KI-Unterstützung einen Fragenkatalog, der die Handlungsfelder gezielt prüft. Zu jeder Frage ist vorgegeben, welcher Stufe welche Antwort entspricht.
2. **Dialog:** Die befragte Person beantwortet die Fragen im Dialog mit dem System. Antworten sind Freitext. Die Interaktion mit der KI ist anonymisiert und lässt keinen Rückschluss auf die Person zu.
3. **Bewertung:** Die KI ordnet die Antworten den vorgegebenen Stufen zu und leitet daraus je Handlungsfeld eine Stufe ab.
4. **Report:** Nach Abschluss liegt je Handlungsfeld der Score vor. Daraus kann ein Textreport erstellt werden. Danach werden die einzelnen Antworten gelöscht.
5. **Ergebnis:** Die Stufen werden je MAG und Rolle als Radar-Chart dargestellt.

## 9. Erfolgskriterien (vorläufig)

1. Für jede MAG und Rolle lässt sich ein Radar-Chart mit den Handlungsfeldern erzeugen.
2. Die Verdichtung von Matrix-Spalten zu Handlungsfeldern ist dokumentiert und nachvollziehbar.
3. Die KI-Bewertung besteht Testfälle, die Fehler eingrenzen. Die Testfälle und ihre Ergebnisse sind dokumentiert.
4. Ein Pilot mit den Lernenden zeigt, dass die Ergebnisse für die Entwicklungsgespräche nutzbar sind.
5. Das Assessment erfasst Kompetenzen, die die Matrix heute nicht sichtbar macht.

## 10. Fachliche Rahmenbedingungen

- **Anonymität:** Die Interaktion mit der KI ist anonymisiert. Sie lässt keinen Rückschluss auf die Person zu.
- **Faire Bewertung:** Die Bewertung wird durch Testfälle abgesichert und die Vorgaben werden so verfeinert, dass sie fair sind.
- **Aufbewahrung:** Einzelne Antworten werden nach Abschluss gelöscht. Es bleiben die Scores je Handlungsfeld und der Textreport.
- **Keine Leistungsbeurteilung:** Das Assessment ist kein Bestandteil der Leistungsbeurteilung. Diese erfolgt unabhängig davon, nach Abschluss des Assessments.

## 11. Offene Fragen und Nicht-Ziele

### Nicht-Ziele (vorläufig)

- Leistungsbeurteilung (erfolgt unabhängig vom Assessment).
- Verwaltung von Zertifikaten oder Schulungsbuchungen.
- Anbindung an HR-Systeme.
- Pflege der Kompetenzmatrix selbst. Die Matrix bleibt die fachliche Quelle.
- **Später:** Selbst- und Fremdbeurteilung als ergänzende Sicht.

### Offene Fragen

1. **Anonymisierung:** Welche Merkmale der Antworten müssen entfernt werden, damit kein Rückschluss auf die Person möglich ist?
2. **Mindestgröße:** Ab welcher Anzahl Personen zeigt ein Radar-Chart je MAG und Rolle, damit kein Rückschluss auf Einzelne möglich ist?
3. **Aufbewahrung:** Wie lange werden Scores und Textreports gespeichert?
4. **Pilot:** Welche Lernenden-Gruppe und welcher Zeitrahmen?

---

## Änderungsverlauf

| Version | Datum | Änderung |
|---|---|---|
| 0.1 | 2026-11-07 | Erster Entwurf aus der Vorlage der Teamleitung |
| 0.2 | 2026-11-07 | Umsetzungsdetails entfernt; Fachinhalt getrennt von technischer Umsetzung |
| 0.3 | 2026-11-07 | Antworten eingearbeitet: KI-gestützter Fragenkatalog und Dialog, Entscheidungsmodell, getrennte Bewertung je Rolle, MAG-Definition, Skala 0–5 mit Stufenbeschreibungen, Handlungsfeld „Haltung“ gestrichen |
| 0.4 | 2026-11-07 | Name „devace“ festgelegt |
| 0.5 | 2026-11-07 | Anonymisierung, Aufbewahrung nach Abschluss, Trennung von Leistungsbeurteilung, Fragenumfang nach Alltagsrelevanz, Pilot mit Lernenden, Vorgabe der Stufen je Frage; Fragen zur Prüfung der KI-Bewertung durch Testfälle ersetzt |
