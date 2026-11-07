# Glossar

Quelle: `docs/vision.md` (Entwurf 0.7). Pro Konzept gilt genau ein Begriff. Spalte „Vermeiden“ nennt Synonyme, die in den Spezifikationen nicht verwendet werden.

| Term | Definition | Avoid |
|------|------------|-------|
| devace | Das KI-gestützte Kompetenz-Assessment für Entwicklungsteams, das diese Dokumente beschreiben. | |
| Assessment | Ein vollständiger Durchlauf einer Befragten durch den Fragenkatalog ihrer Rolle, von der Einladung bis zum Textreport. | Umfrage, Befragung |
| Befragte | Person, die ein Assessment ausfüllt. Sie bleibt für devace anonym. | Teilnehmer, Proband |
| Teamleitung | Person, die Einladungen für die Befragten ihrer MAG erstellt und die Radar-Charts auswertet. | Führungskraft |
| Schulungsverantwortliche | Person, die anhand der Radar-Charts den Schulungsbedarf priorisiert. | Trainer |
| Administrator | Person, die Fragenkatalog, Entscheidungsmodell sowie MAGs und Rollen pflegt. | Admin |
| MAG | Mitarbeitergruppe. Setzt sich aus Scrum Team, Linienteam und Rolle zusammen. | |
| Rolle | Aufgabe einer Befragten im Team, zum Beispiel Entwicklung, Test, Scrum Master oder DevEx. Die Bewertung erfolgt getrennt je Rolle. | |
| Handlungsfeld | Eine der sechs Kompetenzdimensionen, die ein Assessment bewertet: Domänenwissen & Business Alignment, Architektur & Engineering, Qualität & Testing, Security & DevSecOps, Methodik & Agile Delivery, Future Skills & AI. | Kompetenzdimension, Dimension |
| Kompetenzmatrix | Die bestehende, detaillierte Matrix der Teams mit über 150 Spalten. Sie bleibt die fachliche Quelle und wird von devace nicht gepflegt. | |
| Fragenkatalog | Die freigegebene Menge der Fragen eines Assessments, geordnet nach Handlungsfeldern. | Fragebogen |
| Frage | Eine einzelne Aufgabe des Fragenkatalogs, die zu genau einem Handlungsfeld gehört und mit Freitext beantwortet wird. | |
| Antwort | Der Freitext, den eine Befragte zu einer Frage eingibt. | |
| Stufe | Ausprägung einer Antwort auf der Skala 0 bis 5 (0 Nie gehört, 1 Schon mal gehört, 2 Grundverständnis, 3 Sicherer Umgang, 4 Fortgeschrittene Expertise, 5 Expert:in / Lehrniveau). | Level |
| Entscheidungsmodell | Vorgabe je Frage, welcher Stufe welche Antwort entspricht. Die KI ordnet die Antworten diesen Vorgaben zu. | Bewertungsschema |
| Score | Ergebnis eines Handlungsfelds: Summe der Stufen aller Fragen dieses Handlungsfelds geteilt durch deren Anzahl, also ein Wert zwischen 0 und 5. | Punktzahl, Punkte |
| Radar-Chart | Darstellung der Scores aller Handlungsfelder je MAG und Rolle als Netzdiagramm. | Spinnendiagramm |
| Textreport | Textdokument zum Abschluss eines Assessments. Die Befragte erhält es einmalig zum Download und verwaltet es selbst. | Bericht |
| Einladung | Eindeutige Referenz, die eine Teamleitung für eine Befragte erstellt, einer MAG und Rolle zuordnet und per E-Mail verschickt. Sie berechtigt zu genau einem Assessment. | Token |
| Startinweis | Information, die jede Befragte vor dem Assessment erhält: eingesetzte KI-Modelle, nicht zu erfassende Angaben und Geltung der Firmenweisungen zum Umgang mit KI. | |
| Testfall | Vorgegebene Antwort mit erwarteter Stufe, mit dem die KI-Bewertung abgesichert wird. | |
