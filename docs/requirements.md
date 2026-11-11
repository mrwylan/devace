# Anforderungen: devace

Quelle: `docs/vision.md` (Entwurf 0.7), Begriffe: `docs/glossary.md`.
Dieses Dokument beschreibt Fachinhalt. Technische Umsetzung steht in `CLAUDE.md` und in den ADRs.

## Funktionale Anforderungen

| ID | Title | User Story | Priority | Status |
|----|-------|------------|----------|--------|
| FR-001 | Einladung erstellen | Als Teamleitung möchte ich nach der Anmeldung für einen Teilnehmer meiner MAG eine Einladung mit eindeutiger Referenz und einer Gültigkeitsdauer von 1 bis 30 Tagen erstellen, die einer MAG und Rolle zugeordnet ist, damit die Scores der richtigen MAG und Rolle zugerechnet werden, ohne dass der Teilnehmer sich identifizieren muss. | High | Open |
| FR-002 | Einladung versenden | Als Teamleitung möchte ich die Einladung per E-Mail an den Teilnehmer senden, ohne die E-Mail-Adresse zu speichern, damit er das Assessment selbstständig starten kann. | High | Open |
| FR-003 | Assessment mit Einladung starten | Als Teilnehmer möchte ich das Assessment über meine Einladung starten, wobei jede Einladung innerhalb ihrer Gültigkeitsdauer genau ein Assessment erlaubt und ich es vor dem Abschluss wiederaufnehmen kann, damit meine Scores meiner MAG und Rolle zugeordnet werden und niemand mehrfach teilnimmt. | High | Open |
| FR-004 | Starthinweis lesen | Als Teilnehmer möchte ich vor jedem Assessment den Starthinweis lesen (eingesetzte KI-Modelle, nicht zu erfassende Angaben wie Namen von Personen oder Kundenprojekte, Geltung der Firmenweisungen zum Umgang mit KI), damit ich weiß, was ich schreiben darf. | High | Open |
| FR-005 | Fragen im Dialog beantworten | Als Teilnehmer möchte ich die Fragen meiner Rolle im Dialog mit dem System in Freitext beantworten, damit ich meine Kompetenz in eigenen Worten darstellen kann. | High | Open |
| FR-006 | Antworten Stufen zuordnen | Als Teilnehmer möchte ich, dass die KI jede meiner Antworten nach dem Entscheidungsmodell einer Stufe zuordnet, damit meine Einschätzung nachvollziehbar und einheitlich bewertet wird. | High | Open |
| FR-007 | Scores berechnen | Als Teamleitung möchte ich, dass je Handlungsfeld ein Score aus der Summe der Stufen geteilt durch die Anzahl der Fragen berechnet und ohne Personenbezug gespeichert wird, damit Stärken und Lücken meiner MAG vergleichbar sind. | High | Verified |
| FR-008 | Textreport herunterladen | Als Teilnehmer möchte ich nach Abschluss des Assessments einen Textreport einmalig herunterladen, damit ich meine Ergebnisse selbst verwalten kann. | High | Verified |
| FR-009 | Antworten löschen | Als Teilnehmer möchte ich, dass meine einzelnen Antworten nach Erstellung des Textreports gelöscht werden, damit von meinem Assessment nur die Scores bleiben. | High | Verified |
| FR-010 | Radar-Chart je MAG und Rolle ansehen | Als Teamleitung möchte ich nach der Anmeldung das Radar-Chart meiner MAG je Rolle ansehen, auch für kleine MAGs, damit ich Entwicklungsgespräche an Handlungsfeldern ausrichten kann. | High | Open |
| FR-011 | Radar-Charts für Schulungsplanung ansehen | Als Schulungsverantwortliche möchte ich nach der Anmeldung die Radar-Charts aller MAGs ansehen (nur Radar-Charts, keine Einladungen), damit ich den Schulungsbedarf anhand der Lücken priorisieren kann. | Medium | Open |
| FR-012 | Fragenkatalog mit KI erstellen | Als Administrator möchte ich mit KI-Unterstützung einen Fragenkatalog je Rolle erstellen, der die Handlungsfelder gezielt prüft, damit das Assessment die Kompetenzen mit Alltagsrelevanz abdeckt. | High | Open |
| FR-013 | Entscheidungsmodell pflegen | Als Administrator möchte ich zu jeder Frage festlegen, welcher Stufe welche Antwort entspricht, damit die KI die Antworten fair zuordnen kann. | High | Open |
| FR-014 | Fragenkatalog freigeben | Als Administrator möchte ich einen Fragenkatalog samt Entscheidungsmodell prüfen und freigeben, damit nur geprüfte Fragen in Assessments verwendet werden. | High | Open |
| FR-015 | MAGs und Rollen verwalten | Als Administrator möchte ich MAGs mit ihrer Teamleitung und Rollen anlegen und ändern, damit Einladungen und Radar-Charts der Organisation entsprechen. | Medium | Open |
| FR-016 | KI-Bewertung mit Testfällen absichern | Als Administrator möchte ich Testfälle ausführen und deren Ergebnisse einsehen, damit Fehler der KI-Bewertung eingegrenzt und die Vorgaben verfeinert werden können. | High | Open |
| FR-017 | Zuordnung der Kompetenzmatrix dokumentieren | Als Administrator möchte ich festhalten, welche Spalten der Kompetenzmatrix welchem Handlungsfeld zugeordnet sind, damit die Verdichtung nachvollziehbar ist. | Medium | Open |
| FR-018 | Persönliche Auswertung ansehen | Als Teilnehmer möchte ich beim Abschluss meines Assessments mein Radar-Chart, meine Scores je Handlungsfeld und einen Textreport sehen, der meine Leistung würdigt, meine Einstufung begründet und Hinweise auf meine nächsten Investitionen gibt, damit ich meine Kompetenz einordnen und meine Weiterentwicklung gezielt planen kann. | High | Verified |
| FR-019 | Starthinweis pflegen | Als Administrator möchte ich den Starthinweis pflegen (eingesetzte KI-Modelle, nicht zu erfassende Angaben, Geltung der Firmenweisungen zum Umgang mit KI), damit Teilnehmer vor jedem Assessment den aktuellen Hinweis lesen. | Medium | Open |

## Nicht-funktionale Anforderungen

| ID | Title | Requirement | Category | Priority | Status |
|----|-------|-------------|----------|----------|--------|
| NFR-001 | Anonyme KI-Anfragen | Keine Anfrage an ein KI-Modell enthält die Einladung, die E-Mail-Adresse, den Namen oder einen anderen Identifikator eines Teilnehmers (0 Treffer in einer Prüfung aller übergebenen Daten). | Security | High | In Progress |
| NFR-002 | Scores ohne Personenbezug | Kein gespeicherter Score verweist auf eine Einladung, E-Mail-Adresse oder Person; die Zuordnung erfolgt nur über MAG und Rolle (0 Verweise im gespeicherten Datenbestand). | Security | High | In Progress |
| NFR-003 | Löschung der Antworten | Alle Antworten eines Assessments sind spätestens 1 Stunde nach Erstellung des Textreports gelöscht. | Security | High | Verified |
| NFR-004 | Löschung abgebrochener Assessments | Antworten eines nicht abgeschlossenen Assessments werden spätestens 7 Tage nach der letzten Eingabe gelöscht. | Security | High | Open |
| NFR-005 | Zugriffsschutz Auswertung | 100 % der Aufrufe von Radar-Charts, Einladungen, Fragenkatalog und Entscheidungsmodell ohne Anmeldung und passende Berechtigung werden abgewiesen. | Security | High | Open |
| NFR-006 | Antwortzeit Dialog | 95 % der Antworten des Systems auf eine Eingabe des Teilnehmers (inklusive Stufenzuordnung) erscheinen innerhalb von 10 Sekunden. | Performance | Medium | In Progress |
| NFR-007 | Antwortzeit Radar-Chart | 95 % der Radar-Charts sind innerhalb von 3 Sekunden nach Aufruf vollständig dargestellt; beim Abschluss eines Assessments zählt die Zeit ab dem Erscheinen der Auswertung. | Performance | Medium | In Progress |
| NFR-008 | Gleichzeitige Assessments | Das System unterstützt 50 gleichzeitige Assessments ohne Überschreitung der Grenzwerte aus NFR-006. | Scalability | Medium | Open |
| NFR-009 | Faire Bewertung | Bei Ausführung des Testfall-Katalogs ordnet die KI mindestens 90 % der Testfälle die erwartete Stufe zu; Ergebnis und Abweichungen sind dokumentiert. | Maintainability | High | Open |
| NFR-010 | Sprache und Zugänglichkeit | Alle Oberflächen, Fragen und Hinweise sind in deutscher Sprache und erfüllen WCAG 2.1 AA. | Usability | Medium | In Progress |

## Randbedingungen

| ID | Title | Constraint | Category | Source | Priority | Status |
|----|-------|------------|----------|--------|----------|--------|
| C-001 | Firmenweisungen zur KI | Die allgemeinen Firmenweisungen zum Umgang mit KI gelten für alle Textantworten der Teilnehmer. | Regulatory | Allgemeine Firmenweisungen zum Umgang mit KI (Fassung nicht benannt) | High | Open |
| C-002 | Keine Leistungsbeurteilung | Scores und Textreports sind kein Bestandteil der Leistungsbeurteilung; diese erfolgt unabhängig und nach Abschluss des Assessments. | Business | | High | In Progress |
| C-003 | Aufbewahrung der Scores | Scores je Handlungsfeld werden fünf Jahre aufbewahrt. | Business | | High | In Progress |
| C-004 | Programmlaufzeit | Das Programm läuft bis 2030; der Erfolg ist anhand der Scores über fünf Jahre nachzuweisen. | Schedule | | High | Open |
| C-005 | Skala 0 bis 5 | Antworten werden auf der Skala 0 bis 5 mit den Stufenbeschreibungen der Vision bewertet. | Business | | High | In Progress |
| C-006 | Umfang nach Alltagsrelevanz | Der Fragenkatalog nimmt Technologien und Grundlagen nach Relevanz im Alltag der Teams auf, nicht nach Eintrag in der Kompetenzmatrix. | Business | | Medium | Open |
| C-007 | Kompetenzmatrix bleibt Quelle | Die Kompetenzmatrix bleibt die fachliche Quelle und wird von devace nicht gepflegt. | Business | | Medium | Open |
| C-008 | Keine Anbindung an HR-Systeme | devace wird nicht an HR-Systeme angebunden und verwaltet weder Zertifikate noch Schulungsbuchungen. | Business | | Medium | Open |
