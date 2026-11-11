# Open Questions

Questions about the specification that the specification does not answer yet. A question is `Resolved` only when
the answer is in the specification itself; **Resolved in** names where. Resolved and withdrawn questions stay as
history.

## OQ-001: Frist für die Löschung der Antworten nach dem Textreport?

**Element:** NFR-003
**Raised by:** /requirements, 2026-11-07
**Blocking:** No
**Status:** Answered
**Assumed:** 1 Stunde nach Erstellung des Textreports.

Die Vision verlangt Löschung nach Erstellung des Textreports, nennt aber keine Frist.

**Answer:** Bestätigt: 1 Stunde nach Erstellung des Textreports.
**Answered by:** Jakob Albrecht, 2026-11-10

## OQ-002: Was geschieht mit Antworten eines abgebrochenen Assessments?

**Element:** NFR-004
**Raised by:** /requirements, 2026-11-07
**Blocking:** No
**Status:** Answered
**Assumed:** Löschung 7 Tage nach der letzten Eingabe.

Die Vision regelt nur den Abschluss. Ohne Regel bliebe Freitext unbegrenzt gespeichert.

**Answer:** Bestätigt: Löschung 7 Tage nach der letzten Eingabe.
**Answered by:** Jakob Albrecht, 2026-11-10

## OQ-003: Welche Rollen und welche Anmeldung berechtigen zum Zugriff auf die Auswertung?

**Element:** NFR-005
**Raised by:** /requirements, 2026-11-07
**Blocking:** No
**Status:** Answered
**Assumed:** Alle drei Rollen melden sich an; Befragte nutzen nur die Einladung.

Die Antwort der Teamleitung nennt Anmeldung nur für die Teamleitung. Für Schulungsverantwortliche und Administrator wurde dasselbe angenommen.

**Answer:** Bestätigt: Alle drei Rollen melden sich an; Befragte nutzen nur die Einladung.
**Answered by:** Jakob Albrecht, 2026-11-10

## OQ-004: Sehen Schulungsverantwortliche die Radar-Charts aller MAGs?

**Element:** FR-011
**Raised by:** /requirements, 2026-11-07
**Blocking:** No
**Status:** Answered
**Assumed:** Alle MAGs, nur Radar-Charts, keine Einladungen.

Die Vision nennt ihren Nutzen (Bedarf priorisieren), aber nicht den Umfang ihrer Sicht.

**Answer:** Bestätigt: Alle MAGs, nur Radar-Charts, keine Einladungen.
**Answered by:** Jakob Albrecht, 2026-11-10

## OQ-005: Wie lange gilt eine Einladung, und wie oft kann sie genutzt werden?

**Element:** FR-003
**Raised by:** /requirements, 2026-11-07
**Blocking:** No
**Status:** Resolved

Die Antwort legt fest, dass die Einladung zu einer Befragten gehört. Gültigkeitsdauer und Wiederaufnahme eines abgebrochenen Assessments sind offen.

**Answer:** Unbegrenzt, mit Wiederaufnahme: Eine Einladung erlaubt genau ein Assessment ohne Ablauf; vor dem Abschluss kann die Befragte weitermachen, solange ihre Antworten nicht nach NFR-004 gelöscht wurden.
**Answered by:** Jakob Albrecht, 2026-11-08
**Resolved in:** FR-003

## OQ-006: Gibt es einen Fragenkatalog je Rolle?

**Element:** FR-012
**Raised by:** /requirements, 2026-11-07
**Blocking:** No
**Status:** Open
**Assumed:** Je Rolle ein eigener Fragenkatalog.

Die Vision verlangt getrennte Bewertung je Rolle; ob dafür getrennte Fragensätze nötig sind, ist offen.

## OQ-007: Antwortzeiten und Last (10 s Dialog, 3 s Radar-Chart, 50 gleichzeitige Assessments)?

**Element:** NFR-006
**Raised by:** /requirements, 2026-11-07
**Blocking:** No
**Status:** Answered
**Assumed:** 10 s (95 %), 3 s (95 %), 50 gleichzeitige Assessments.

Die Vision nennt keine Zahlen. Die Werte sind Vorschläge für den Pilot mit Lernenden.

**Answer:** Bestätigt: 10 s (95 %), 3 s (95 %), 50 gleichzeitige Assessments.
**Answered by:** Jakob Albrecht, 2026-11-10

## OQ-008: Schwelle für die faire Bewertung?

**Element:** NFR-009
**Raised by:** /requirements, 2026-11-07
**Blocking:** No
**Status:** Answered
**Assumed:** Mindestens 90 % der Testfälle ergeben die erwartete Stufe.

Die Vision verlangt Testfälle, nennt aber keine Erfolgsquote.

**Answer:** Bestätigt: Mindestens 90 % der Testfälle ergeben die erwartete Stufe.
**Answered by:** Jakob Albrecht, 2026-11-10

## OQ-009: Gilt WCAG 2.1 AA und ausschließlich Deutsch?

**Element:** NFR-010
**Raised by:** /requirements, 2026-11-07
**Blocking:** No
**Status:** Answered
**Assumed:** Deutsch; WCAG 2.1 AA.

Die Vision nennt weder Sprache noch Zugänglichkeitsstufe.

**Answer:** Bestätigt: Deutsch; WCAG 2.1 AA.
**Answered by:** Jakob Albrecht, 2026-11-10

## OQ-010: Welche Fassung der Firmenweisungen zum Umgang mit KI gilt?

**Element:** C-001
**Raised by:** /requirements, 2026-11-07
**Blocking:** No
**Status:** Open
**Assumed:** Weisungen in der jeweils gültigen Fassung.

Die Vision nennt die Weisungen ohne Dokument und Version, die Source-Spalte bleibt daher unvollständig.

## OQ-011: Gehört die Rolle zur MAG?

**Element:** Glossary: MAG
**Raised by:** /entity-model, 2026-11-07
**Blocking:** No
**Status:** Resolved

Das Glossar und die Vision §4 sagen, die MAG setze sich aus Scrum Team, Linienteam und Rolle zusammen. Gleichzeitig werden Radar-Charts "je MAG und Rolle" gebildet. Das Entity-Modell trennt MAG und ROLLE.

**Answer:** Rolle getrennt: MAG besteht aus Scrum Team und Linienteam; die Rolle ist ein eigener Wert daneben.
**Answered by:** Jakob Albrecht, 2026-11-08
**Resolved in:** Glossary: MAG, Entity: MAG, Entity: ROLLE

## OQ-012: Wird die E-Mail-Adresse der Befragten gespeichert?

**Element:** Entity: EINLADUNG
**Raised by:** /entity-model, 2026-11-07
**Blocking:** No
**Status:** Resolved

Die Teamleitung versendet die Einladung per E-Mail. Eine gespeicherte Adresse würde neben der Einladung einen Personenbezug schaffen.

**Answer:** Nicht speichern: Die E-Mail-Adresse wird nur zum Versand verwendet.
**Answered by:** Jakob Albrecht, 2026-11-08
**Resolved in:** Entity: EINLADUNG, FR-002

## OQ-013: Wann wird das Assessment selbst gelöscht?

**Element:** Entity: ASSESSMENT
**Raised by:** /entity-model, 2026-11-07
**Blocking:** No
**Status:** Resolved

Die Vision löscht nur die Antworten. Ein bleibender Assessment-Datensatz mit Zeitstempel könnte mit dem Score korreliert werden.

**Answer:** Mit den Antworten löschen: Assessment, Antworten und Bewertungen werden gemeinsam gelöscht; die Einladung bleibt mit Status Eingelöst.
**Answered by:** Jakob Albrecht, 2026-11-08
**Resolved in:** Entity: ASSESSMENT, FR-009

## OQ-014: Maximale Länge einer Freitext-Antwort?

**Element:** Entity: ANTWORT.text
**Raised by:** /entity-model, 2026-11-07
**Blocking:** No
**Status:** Answered
**Assumed:** 500 Zeichen.

Die Vision nennt keine Grenze.

**Answer:** Bestätigt: 500 Zeichen.
**Answered by:** Jakob Albrecht, 2026-11-10

## OQ-015: Welche Rollen darf die Teamleitung für eine Einladung wählen?

**Element:** UC-001 BR-003
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Resolved
**Assumed:** Nur Rollen mit freigegebenem Fragenkatalog stehen zur Wahl (A2, wenn keine existiert).

Eine Einladung für eine Rolle ohne freigegebenen Fragenkatalog wäre nicht nutzbar. Die Anforderungen nennen die Prüfung nicht.

**Answer:** Bestätigt: Nur Rollen mit freigegebenem Fragenkatalog stehen zur Wahl (A2, wenn keine existiert).
**Answered by:** Jakob Albrecht, 2026-11-10
**Resolved in:** UC-001 BR-003, UC-001 A2

## OQ-016: Wie werden doppelte Adressen in einer Eingabe behandelt?

**Element:** UC-001 BR-005
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Resolved
**Assumed:** Dieselbe Adresse mehrfach in einer Eingabe erzeugt eine Einladung.

Da Adressen nicht gespeichert werden, lassen sich Doppelungen nur innerhalb einer Eingabe erkennen.

**Answer:** Bestätigt: Dieselbe Adresse mehrfach in einer Eingabe erzeugt eine Einladung.
**Answered by:** Jakob Albrecht, 2026-11-10
**Resolved in:** UC-001 BR-005

## OQ-017: Gibt es eine Höchstzahl an Adressen je Durchgang?

**Element:** UC-001 step 4
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Resolved
**Assumed:** Keine Obergrenze.

Die Wahl 'mehrere Adressen je Durchgang' nennt keine Obergrenze.

**Answer:** Max. 50 Adressen
**Answered by:** Jakob Albrecht, 2026-11-10
**Resolved in:** UC-001 A1, UC-001 BR-010

## OQ-018: Wie wird eine Teamleitung einer MAG zugeordnet?

**Element:** FR-015
**Raised by:** /spec-review UC-001 (angenommen von /use-case-spec), 2026-11-08
**Blocking:** No
**Status:** Answered
**Assumed:** Der Administrator ordnet in UC-008 jeder MAG eine Teamleitung zu.

UC-001 setzt voraus, dass der Teamleitung mindestens eine MAG zugeordnet ist (Entity MAG.teamleitung_id). FR-015 nennt die Zuordnung nicht.

**Answer:** Bestätigt: Der Administrator ordnet in UC-008 jeder MAG eine Teamleitung zu.
**Answered by:** Jakob Albrecht, 2026-11-10

## OQ-019: Wer pflegt den Starthinweis, und in welchem Use Case?

**Element:** FR-004
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Answered
**Assumed:** Der Administrator pflegt den Starthinweis; ein Use Case dafür fehlt noch (Hand-off an /requirements und /use-case-diagram).

UC-002 setzt voraus, dass ein Starthinweis hinterlegt ist. Kein Use Case im Diagramm pflegt ihn (Text, KI-Modelle, Version).

**Answer:** Bestätigt: Der Administrator pflegt den Starthinweis; ein Use Case dafür fehlt noch (Hand-off an /requirements und /use-case-diagram).
**Answered by:** Jakob Albrecht, 2026-11-10

## OQ-020: Wird der Starthinweis bei der Wiederaufnahme erneut gezeigt?

**Element:** UC-002 A2
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Resolved
**Assumed:** Nein, der bereits bestätigte Starthinweis gilt für das laufende Assessment.

Die Vision verlangt den Hinweis vor jedem Assessment. Bei der Wiederaufnahme läuft dasselbe Assessment weiter.

**Answer:** Bestätigt: Nein, der bereits bestätigte Starthinweis gilt für das laufende Assessment.
**Answered by:** Jakob Albrecht, 2026-11-10
**Resolved in:** UC-002 A2

## OQ-021: Verfällt eine Einladung, mit der noch kein Assessment gestartet wurde?

**Element:** UC-002 BR-001
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Resolved

Die Antwort zum Review sagt, eine nicht eingelöste Einladung verfalle. Gemeint ist angenommen die Einladung eines gelöschten, nicht abgeschlossenen Assessments. Auf Einladungen, die nie geöffnet wurden, trifft das nicht zu.

**Answer:** Die Teamleitung legt beim Erstellen eine Gültigkeitsdauer von 1 bis 30 Tagen fest (Vorschlag 30). Danach ist die Einladung abgelaufen; ein gestartetes Assessment läuft weiter. Eine Einladung eines gelöschten Assessments ist eingelöst und nicht wiederverwendbar.
**Answered by:** Jakob Albrecht, 2026-11-08
**Resolved in:** UC-001 BR-008, UC-002 BR-001

## OQ-022: Wann endet die Gültigkeit einer Einladung genau?

**Element:** UC-001 BR-008
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Resolved
**Assumed:** Die Gültigkeit beginnt mit der Erstellung und endet nach Ablauf des letzten Tages der Gültigkeitsdauer (24 Uhr).

Die Gültigkeitsdauer wird in ganzen Tagen angegeben; Beginn und Ende innerhalb eines Tages sind nicht festgelegt.

**Answer:** Bestätigt: Die Gültigkeit beginnt mit der Erstellung und endet nach Ablauf des letzten Tages der Gültigkeitsdauer (24 Uhr).
**Answered by:** Jakob Albrecht, 2026-11-10
**Resolved in:** UC-001 BR-008

## OQ-023: Wie meldet sich die Teamleitung an?

**Element:** UC-001
**Raised by:** /use-case-implement, 2026-11-08
**Blocking:** No
**Status:** Resolved

UC-001 setzt voraus, dass die Teamleitung angemeldet ist (NFR-005). Keine Spezifikation legt das Verfahren fest, und der Benutzerbestand (BENUTZER) ist im Entity-Modell nur eine externe Systemgrenze.

**Answer:** Formular-Anmeldung mit Benutzername und Passwort gegen einen Benutzerbestand des Systems. Zugangsdaten stehen nie im Repository.
**Answered by:** Jakob Albrecht, 2026-11-08
**Resolved in:** UC-001 BR-009

## OQ-024: Wie sieht die Einladungs-E-Mail aus?

**Element:** UC-001 BR-007
**Raised by:** /use-case-implement, 2026-11-08
**Blocking:** No
**Status:** Resolved

BR-007 sagt, die E-Mail enthalte nur die Einladung. Betreff, Text, Absender und die Form des Links sind nicht festgelegt.

**Answer:** Kurztext mit Link: Betreff "Einladung zum devace-Assessment", ein Satz und ein Link zur Einladung. Absender und Basis-Adresse des Links kommen aus der Konfiguration.
**Answered by:** Jakob Albrecht, 2026-11-08
**Resolved in:** UC-001 BR-007

## OQ-025: Wer legt Benutzerkonten an und weist ihnen die Rolle Teamleitung, Schulungsverantwortliche oder Administrator zu?

**Element:** UC-008 BR-004
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Answered
**Assumed:** Die Verwaltung der Benutzerkonten liegt außerhalb von UC-008; UC-008 wählt die Teamleitung aus vorhandenen Teamleitungen. Hand-off an /requirements für eine Anforderung und an /use-case-diagram für einen Use Case.

Die Anmeldung (OQ-023) setzt Benutzerkonten voraus. Keine Anforderung und kein Use Case legt sie an oder ändert sie.

**Answer:** Bestätigt: Die Verwaltung der Benutzerkonten liegt außerhalb von UC-008; UC-008 wählt die Teamleitung aus vorhandenen Teamleitungen. Hand-off an /requirements für eine Anforderung und an /use-case-diagram für einen Use Case.
**Answered by:** Jakob Albrecht, 2026-11-10

## OQ-026: Wird bei der Eindeutigkeit von MAG und Rolle die Schreibweise unterschieden?

**Element:** UC-008 BR-002
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Answered
**Assumed:** Nein, Groß- und Kleinschreibung sowie umgebende Leerzeichen bleiben beim Vergleich unberücksichtigt.

Ohne diese Regel entstünden "Entwicklung" und "entwicklung" als zwei Rollen mit getrennten Radar-Charts.

**Answer:** Bestätigt: Nein, Groß- und Kleinschreibung sowie umgebende Leerzeichen bleiben beim Vergleich unberücksichtigt.
**Answered by:** Jakob Albrecht, 2026-11-10

## OQ-027: Wie viele Entwürfe hat eine Rolle?

**Element:** UC-006 BR-004
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Answered
**Assumed:** Höchstens einen. Besteht ein Entwurf, setzt der Administrator ihn fort; ein weiterer Entwurf entsteht nicht.

Das Entity-Modell begrenzt nur die freigegebenen Fragenkataloge je Rolle auf einen. Mehrere parallele Entwürfe würden die Freigabe (UC-007) mehrdeutig machen.

**Answer:** Bestätigt: Höchstens einen. Besteht ein Entwurf, setzt der Administrator ihn fort; ein weiterer Entwurf entsteht nicht.
**Answered by:** Jakob Albrecht, 2026-11-10

## OQ-028: Wie viele Fragen je Handlungsfeld darf der Administrator für den KI-Entwurf anfordern?

**Element:** UC-006 BR-003
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Answered
**Assumed:** 1 bis 10, Vorschlag 3.

Die Vision nennt keine Zahl. Die Grenze ist ein Vorschlag für den Pilot mit Lernenden.

**Answer:** Bestätigt: 1 bis 10, Vorschlag 3.
**Answered by:** Jakob Albrecht, 2026-11-10

## OQ-029: Darf ein Entwurf Handlungsfelder ohne Frage enthalten?

**Element:** UC-006 BR-006
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Answered
**Assumed:** Ja. Ob ein Fragenkatalog vollständig genug für die Freigabe ist (zum Beispiel mindestens eine Frage je Handlungsfeld), prüft UC-007.

Ohne Frage in einem Handlungsfeld ließe sich kein Score für dieses Handlungsfeld berechnen. Das betrifft die Freigabe, nicht die Erstellung.

**Answer:** Bestätigt: Ja. Ob ein Fragenkatalog vollständig genug für die Freigabe ist (zum Beispiel mindestens eine Frage je Handlungsfeld), prüft UC-007.
**Answered by:** Jakob Albrecht, 2026-11-10

## OQ-030: Welchen Wortlaut haben die Hinweise bei fehlenden oder ungültigen Angaben und bei bestehender MAG oder Rolle?

**Element:** UC-008 A1
**Raised by:** /use-case-implement, 2026-11-08
**Blocking:** No
**Status:** Answered

A1 und A2 sagen nur, das System weise auf fehlende oder ungültige Angaben bzw. darauf hin, dass die MAG oder die Rolle bereits besteht. Ein Wortlaut fehlt.

**Answer:** Hinweise am jeweiligen Feld: "Pflichtangabe fehlt." bzw. "Höchstens 100 Zeichen." (Rolle: 50) für A1, "Diese MAG besteht bereits." bzw. "Diese Rolle besteht bereits." für A2.
**Answered by:** Jakob Albrecht, 2026-11-08

## OQ-031: Welche Tabelle hält die Benutzerkonten, und wie entsteht der erste Administrator?

**Element:** Entity: BENUTZER
**Raised by:** /use-case-implement, 2026-11-08
**Blocking:** No
**Status:** Resolved

Das Entity-Modell nennt BENUTZER nur als externe Systemgrenze mit der Spalte id. Die Anmeldung (OQ-023) braucht Benutzername, Passwort und die Funktion der Person (Teamleitung, Schulungsverantwortliche, Administrator). Wer weitere Konten anlegt, ist in OQ-025 offen.

**Answer:** Tabelle benutzer mit id, benutzername (eindeutig), passwort_hash und berechtigung (Teamleitung, Schulungsverantwortliche, Administrator). Der erste Administrator entsteht beim Start aus den Umgebungsvariablen DEVACE_ADMIN_USERNAME und DEVACE_ADMIN_PASSWORD, wenn es noch keinen Administrator gibt; das Passwort wird gehasht gespeichert und nie ins Repository geschrieben.
**Answered by:** Jakob Albrecht, 2026-11-08
**Resolved in:** Entity: BENUTZER

## OQ-032: Welchen Wortlaut hat der Hinweis, wenn eine MAG eine nicht vorhandene Teamleitung erhält?

**Element:** UC-008 BR-004
**Raised by:** /spring-service-test, 2026-11-08
**Blocking:** No
**Status:** Answered

BR-004 verlangt, dass die Teamleitung aus den vorhandenen Teamleitungen ausgewählt wird. Kein Ablauf und kein Wortlaut sagt, was geschieht, wenn eine Angabe außerhalb der Oberfläche eine nicht vorhandene Person oder einen Benutzer ohne Funktion Teamleitung nennt. Der Dienst lehnt sie mit dem Hinweis "Die Teamleitung ist nicht vorhanden." ab; der Wortlaut stammt aus der Umsetzung, nicht aus der Spezifikation. Der Test prüft deshalb nur die Ablehnung, nicht den Text.

**Answer:** „Diese Teamleitung ist nicht vorhanden.“
**Answered by:** Jakob Albrecht, 2026-11-10

## OQ-033: Welchen Wortlaut haben die Hinweise und Bestätigungen beim Erstellen des Fragenkatalogs?

**Element:** UC-006 A2, A3, A4, A6, Schritt 12
**Raised by:** /use-case-implement, 2026-11-08
**Blocking:** No
**Status:** Withdrawn

A2, A3, A4 und A6 sprechen von Hinweisen, Schritt 12 von einer Bestätigung, nennen aber keinen Wortlaut.

**Answer:** A2: "Das KI-Modell hat keinen Entwurf geliefert."; A3: "Die Zahl muss zwischen 1 und 10 liegen." und "Höchstens 2000 Zeichen."; A4: "Wortlaut fehlt.", "Vorgabe fehlt." und "Höchstens 500 Zeichen."; A6: "Jedes Handlungsfeld hat bereits mindestens N Fragen." mit den Auswahlmöglichkeiten "Abbrechen" und "Weitere Fragen ergänzen"; Bestätigung: "Entwurf gespeichert."
**Answered by:** Jakob Albrecht, 2026-11-08
**Withdrawn:** Das Feld Element nannte mehrere Stellen. Ersetzt durch OQ-041 (UC-006) mit unverändertem Inhalt und Antwort.

## OQ-034: Wie werden die KI-Modelle je Aufgabe gewählt?

**Element:** UC-006 Schritt 6 (Sekundärer Akteur KI-Modell)
**Raised by:** /use-case-implement, 2026-11-08
**Blocking:** No
**Status:** Withdrawn
**Assumed:** Das Modell je Aufgabe (Fragenkatalog erstellen, Antworten bewerten, Textreport) wird in der Konfiguration festgelegt und ist ohne Neubau über Umgebungsvariablen änderbar. Für UC-006 gilt nur die Aufgabe Fragenkatalog erstellen.

Der Nutzer wünscht, dass die Modelle dynamisch gewählt werden können: ein besseres Modell zum Erstellen der Fragen samt Bewertungs-Prompt, ein günstigeres für die Durchführung und Bewertung eines Assessments, ein kleineres für den Textreport. Ob die Wahl später in der Oberfläche (zur Laufzeit) erfolgen soll, ist offen und gehört in eine eigene Anforderung.
**Withdrawn:** Das Feld Element hatte keine gültige Form. Ersetzt durch OQ-044 (UC-006 step 6) mit unverändertem Inhalt.

## OQ-035: Gilt die Spalte "Grund" der nicht versendeten Adressen aus dem Mockup?

**Element:** UC-001 Schritt 8
**Raised by:** /use-case-spec, 2026-11-10
**Blocking:** No
**Status:** Resolved

Das Mockup "Einladungen – Ergebnis" zeigt in der Liste der nicht versendeten Adressen eine Spalte "Grund" (z. B. "E-Mail-System hat den Versand abgelehnt"). Schritt 8 und A4 nennen nur die Adressen, bei denen der Versand scheiterte.

**Answer:** Nein. Die Liste zeigt nur die Adressen. Die Spezifikation gilt; die Spalte "Grund" im Mockup wird nicht gebaut. Die Zusammenfassung vor dem Versand ist dagegen übernommen (UC-001 Schritt 5).
**Answered by:** Jakob Albrecht, 2026-11-10
**Resolved in:** UC-001 step 10

## OQ-036: Welcher Wortlaut gilt, wenn das Mockup von früheren Antworten abweicht?

**Element:** UC-006 A2, A3, A4, A6; UC-008 A1, A2
**Raised by:** /use-case-spec, 2026-11-10
**Blocking:** No
**Status:** Withdrawn

Die Mockups zeigen andere Hinweistexte als OQ-030 (UC-008) und OQ-033 (UC-006) festlegen, z. B. "Scrum Team ist eine Pflichtangabe.", "Diese MAG besteht bereits: Scrum Team Alpha · Linienteam Entwicklung." und "Kein Entwurf entstanden – Das KI-Modell ist nicht erreichbar. Der Ausgangspunkt und Ihre bisherigen Angaben bleiben erhalten."

**Answer:** Die bisherigen Antworten OQ-030 und OQ-033 gelten. Der Wortlaut im Mockup ist ein Platzhalter; das Layout folgt dem Mockup.
**Answered by:** Jakob Albrecht, 2026-11-10
**Withdrawn:** Das Feld Element nannte zwei Use Cases mit mehreren Abläufen. Ersetzt durch OQ-042 (UC-006) und OQ-043 (UC-008) mit unveränderter Antwort.

## OQ-037: Wie erscheinen Navigationseinträge, für die der Benutzer keine Berechtigung hat?

**Element:** UC-006 BR-009; UC-008 BR-007; UC-001 BR-009
**Raised by:** /use-case-spec, 2026-11-10
**Blocking:** No
**Status:** Withdrawn

Alle Mockups zeigen links die Einträge der Administration (Fragenkatalog, MAGs und Rollen, Testfälle, Matrix-Zuordnung) neben "Alex Beispiel · Teamleitung". Die Regeln BR-009 und BR-007 lassen nur Administratoren zu.

**Answer:** Die Navigation zeigt alle Einträge. Einträge ohne passende Berechtigung sind sichtbar, aber deaktiviert. Ein Aufruf ohne Berechtigung wird weiterhin abgewiesen.
**Answered by:** Jakob Albrecht, 2026-11-10
**Withdrawn:** Das Element nannte drei Regeln statt einer. Ersetzt durch OQ-038 (UC-006 BR-009), OQ-039 (UC-008 BR-007) und OQ-040 (UC-001 BR-009) mit derselben Antwort.

## OQ-038: Wie erscheinen Navigationseinträge, für die der Benutzer keine Berechtigung hat?

**Element:** UC-006 BR-009
**Raised by:** /use-case-spec, 2026-11-10
**Blocking:** No
**Status:** Answered

Alle Mockups zeigen links die Einträge der Administration (Fragenkatalog, MAGs und Rollen, Testfälle, Matrix-Zuordnung) neben "Alex Beispiel · Teamleitung". BR-009 lässt nur die berechtigte Rolle zu. Ersetzt OQ-037.

**Answer:** Die Navigation zeigt alle Einträge. Einträge ohne passende Berechtigung sind sichtbar, aber deaktiviert. Ein Aufruf ohne Berechtigung wird weiterhin abgewiesen.
**Answered by:** Jakob Albrecht, 2026-11-10

## OQ-039: Wie erscheinen Navigationseinträge, für die der Benutzer keine Berechtigung hat?

**Element:** UC-008 BR-007
**Raised by:** /use-case-spec, 2026-11-10
**Blocking:** No
**Status:** Answered

Alle Mockups zeigen links die Einträge der Administration (Fragenkatalog, MAGs und Rollen, Testfälle, Matrix-Zuordnung) neben "Alex Beispiel · Teamleitung". BR-007 lässt nur die berechtigte Rolle zu. Ersetzt OQ-037.

**Answer:** Die Navigation zeigt alle Einträge. Einträge ohne passende Berechtigung sind sichtbar, aber deaktiviert. Ein Aufruf ohne Berechtigung wird weiterhin abgewiesen.
**Answered by:** Jakob Albrecht, 2026-11-10

## OQ-040: Wie erscheinen Navigationseinträge, für die der Benutzer keine Berechtigung hat?

**Element:** UC-001 BR-009
**Raised by:** /use-case-spec, 2026-11-10
**Blocking:** No
**Status:** Resolved

Alle Mockups zeigen links die Einträge der Administration (Fragenkatalog, MAGs und Rollen, Testfälle, Matrix-Zuordnung) neben "Alex Beispiel · Teamleitung". BR-009 lässt nur die berechtigte Rolle zu. Ersetzt OQ-037.

**Answer:** Die Navigation zeigt alle Einträge. Einträge ohne passende Berechtigung sind sichtbar, aber deaktiviert. Ein Aufruf ohne Berechtigung wird weiterhin abgewiesen.
**Answered by:** Jakob Albrecht, 2026-11-10
**Resolved in:** UC-001 BR-009

## OQ-041: Welchen Wortlaut haben die Hinweise und Bestätigungen beim Erstellen des Fragenkatalogs?

**Element:** UC-006
**Raised by:** /use-case-implement, 2026-11-08
**Blocking:** No
**Status:** Answered

A2, A3, A4 und A6 sprechen von Hinweisen, Schritt 12 von einer Bestätigung, nennen aber keinen Wortlaut. Ersetzt OQ-033, dessen Element mehrere Stellen nannte.

**Answer:** A2: "Das KI-Modell hat keinen Entwurf geliefert."; A3: "Die Zahl muss zwischen 1 und 10 liegen." und "Höchstens 2000 Zeichen."; A4: "Wortlaut fehlt.", "Vorgabe fehlt." und "Höchstens 500 Zeichen."; A6: "Jedes Handlungsfeld hat bereits mindestens N Fragen." mit den Auswahlmöglichkeiten "Abbrechen" und "Weitere Fragen ergänzen"; Bestätigung: "Entwurf gespeichert."
**Answered by:** Jakob Albrecht, 2026-11-08

## OQ-042: Welcher Wortlaut gilt in UC-006, wenn das Mockup von früheren Antworten abweicht?

**Element:** UC-006
**Raised by:** /use-case-spec, 2026-11-10
**Blocking:** No
**Status:** Answered

Das Mockup zeigt für A2, A3, A4 und A6 andere Hinweistexte als OQ-041 festlegt, z. B. "Kein Entwurf entstanden – Das KI-Modell ist nicht erreichbar. Der Ausgangspunkt und Ihre bisherigen Angaben bleiben erhalten." Ersetzt den UC-006-Teil von OQ-036.

**Answer:** Die bisherige Antwort OQ-041 gilt. Der Wortlaut im Mockup ist ein Platzhalter; das Layout folgt dem Mockup.
**Answered by:** Jakob Albrecht, 2026-11-10

## OQ-043: Welcher Wortlaut gilt in UC-008, wenn das Mockup von früheren Antworten abweicht?

**Element:** UC-008
**Raised by:** /use-case-spec, 2026-11-10
**Blocking:** No
**Status:** Answered

Das Mockup zeigt für A1 und A2 andere Hinweistexte als OQ-030 festlegt, z. B. "Scrum Team ist eine Pflichtangabe." und "Diese MAG besteht bereits: Scrum Team Alpha · Linienteam Entwicklung." Ersetzt den UC-008-Teil von OQ-036.

**Answer:** Die bisherige Antwort OQ-030 gilt. Der Wortlaut im Mockup ist ein Platzhalter; das Layout folgt dem Mockup.
**Answered by:** Jakob Albrecht, 2026-11-10

## OQ-044: Wie werden die KI-Modelle je Aufgabe gewählt?

**Element:** UC-006 step 6
**Raised by:** /use-case-implement, 2026-11-08
**Blocking:** No
**Status:** Answered
**Assumed:** Das Modell je Aufgabe (Fragenkatalog erstellen, Antworten bewerten, Textreport) wird in der Konfiguration festgelegt und ist ohne Neubau über Umgebungsvariablen änderbar. Für UC-006 gilt nur die Aufgabe Fragenkatalog erstellen.

Der Nutzer wünscht, dass die Modelle dynamisch gewählt werden können: ein besseres Modell zum Erstellen der Fragen samt Bewertungs-Prompt, ein günstigeres für die Durchführung und Bewertung eines Assessments, ein kleineres für den Textreport. Ob die Wahl später in der Oberfläche (zur Laufzeit) erfolgen soll, ist offen und gehört in eine eigene Anforderung. Der Sekundärakteur ist das KI-Modell. Ersetzt OQ-034, dessen Element keine gültige Form hatte.

**Answer:** Wird per Umgebungsvariable festgelegt. - Aber Achtung JEV Model für die Bewertung hat ein abweichendes REST api (wird aber auch über den OpenRouter URL) angesprochen.
**Answered by:** Jakob Albrecht, 2026-11-10

## OQ-045: Welche Angaben enthält der Textreport je Handlungsfeld, und enthält er Antworttexte?

**Element:** UC-003 BR-003
**Raised by:** /use-case-spec, 2026-11-10
**Blocking:** No
**Status:** Resolved
**Assumed:** Der Textreport enthält je Handlungsfeld die Würdigung der Leistung, die Begründung der Einstufung und Hinweise auf die nächsten Investitionen, aber keine wörtlichen Antworttexte des Teilnehmers.

Die Vorgabe lautet, dass der Textreport die Leistung würdigt, die Einstufung begründet und Hinweise zu den nächsten Investitionen gibt. Ob er zusätzlich Fragen, Antworten und Stufen im Wortlaut aufführt, ist offen. Die Antworten werden nach dem Download gelöscht; der Textreport ist danach die einzige Aufzeichnung des Teilnehmers.

**Answer:** Bestätigt: Der Textreport enthält je Handlungsfeld die Würdigung der Leistung, die Begründung der Einstufung und Hinweise auf die nächsten Investitionen, aber keine wörtlichen Antworttexte des Teilnehmers.
**Answered by:** Jakob Albrecht, 2026-11-10
**Resolved in:** UC-003 BR-003

## OQ-046: Wie läuft der einmalige Download, und wann verfällt der Textreport?

**Element:** UC-003 step 7
**Raised by:** /use-case-spec, 2026-11-10
**Blocking:** No
**Status:** Resolved
**Assumed:** Der Textreport steht nach dem Abschluss einmal zum Download bereit. Die Antworten werden nach dem Download gelöscht, spätestens 1 Stunde nach Erstellung des Textreports (NFR-003). Wird er nicht heruntergeladen, verfällt er und die Scores bleiben.

FR-008 verlangt den einmaligen Download, FR-009 die Löschung der Antworten nach der Erstellung des Textreports, NFR-003 die Löschung innerhalb einer Stunde. Offen ist, was geschieht, wenn der Download scheitert oder nicht stattfindet, und ob ein abgebrochener Download wiederholt werden darf.

**Answer:** Bestätigt: Der Textreport steht nach dem Abschluss einmal zum Download bereit. Die Antworten werden nach dem Download gelöscht, spätestens 1 Stunde nach Erstellung des Textreports (NFR-003). Wird er nicht heruntergeladen, verfällt er und die Scores bleiben.
**Answered by:** Jakob Albrecht, 2026-11-10
**Resolved in:** UC-003 BR-006, UC-003 BR-007, UC-003 A2

## OQ-047: Was geschieht, wenn das KI-Modell den Textreport nicht erstellt?

**Element:** UC-003 A1
**Raised by:** /use-case-spec, 2026-11-10
**Blocking:** No
**Status:** Resolved
**Assumed:** Das System weist darauf hin, speichert keine Scores und lässt das Assessment laufend. Der Teilnehmer kann den Abschluss erneut anfordern; die Antworten bleiben bis dahin erhalten (NFR-004).

Weder die Anforderungen noch die Vision sagen, wie das System mit einem Ausfall des KI-Modells beim Abschluss umgeht. Alternativen: Scores sofort speichern und den Textreport später nachliefern, oder den Abschluss ohne Textreport zulassen.

**Answer:** Bestätigt: Das System weist darauf hin, speichert keine Scores und lässt das Assessment laufend. Der Teilnehmer kann den Abschluss erneut anfordern; die Antworten bleiben bis dahin erhalten (NFR-004).
**Answered by:** Jakob Albrecht, 2026-11-10
**Resolved in:** UC-003 A1

## OQ-048: Wie wird der Score gerundet?

**Element:** UC-003 BR-001
**Raised by:** /use-case-spec, 2026-11-10
**Blocking:** No
**Status:** Resolved
**Assumed:** Der Score hat höchstens zwei Nachkommastellen (kaufmännisch gerundet); ein Handlungsfeld ohne Frage hat keinen Score.

Das Entity-Modell führt den Score als Dezimalzahl mit zwei Nachkommastellen (SCORE.wert). Die Anforderung nennt keine Rundung, und ein Handlungsfeld ohne Frage ergäbe eine Division durch null.

**Answer:** Wird immer mit einer Nachkommastelle, kaufmännisch gerundet ausgegeben. Die Skala auf dem Radar zeigt aber nur die Ganzzahlen an
**Answered by:** Jakob Albrecht, 2026-11-10
**Answer (Ergänzung):** Die Rundung auf eine Nachkommastelle gilt auch für den gespeicherten Score; SCORE.wert im Entity-Modell ist dem noch anzupassen (/entity-model).
**Answered by:** Jakob Albrecht, 2026-11-11
**Resolved in:** UC-003 BR-001

## OQ-049: Zeigt das System dem Teilnehmer seine eigene Auswertung mit Radar-Chart und Scores?

**Element:** UC-003 step 5
**Raised by:** /use-case-spec, 2026-11-10
**Blocking:** No
**Status:** Resolved
**Assumed:** Ja. Der Teilnehmer sieht beim Abschluss sein Radar-Chart, seine Scores je Handlungsfeld und den Textreport. Sie werden nicht gespeichert und nicht erneut angezeigt.

Der Product Owner wünscht eine Auswertung für den Teilnehmer direkt beim Abschluss. Die Anforderungen kennen das Radar-Chart nur je MAG und Rolle für Teamleitung und Schulungsverantwortliche (FR-010, FR-011). Die Anforderung für die persönliche Auswertung fehlt und gehört in `/requirements`; das Glossar beschreibt das Radar-Chart nur je MAG und Rolle.

**Answer:** Ja. Der Teilnehmer sieht beim Abschluss seines Assessments sein Radar-Chart, seine Scores je Handlungsfeld und den Textreport. Der Textreport würdigt seine Leistung, begründet seine Einstufung und gibt Hinweise, wo er als Nächstes investieren sollte; das KI-Modell erstellt ihn. Die Anforderung steht als FR-018 im Katalog.
**Answered by:** Jakob Albrecht, 2026-11-10
**Resolved in:** UC-003 step 5, UC-003 BR-005

## OQ-050: Zählen doppelte Adressen gegen die Höchstzahl von 50?

**Element:** UC-001 BR-010
**Raised by:** /use-case-spec, 2026-11-10
**Blocking:** No
**Status:** Resolved
**Assumed:** Doppelte Adressen einer Eingabe zählen nur einmal gegen die Grenze von 50 Adressen.

Die Antwort auf OQ-017 begrenzt den Durchgang auf höchstens 50 Adressen. BR-005 lässt dieselbe Adresse mehrfach in einer Eingabe zu und erzeugt daraus eine Einladung. Offen ist, ob die Grenze die Zeilen der Eingabe oder die verschiedenen Adressen zählt.

**Answer:** Doppelte Adressen zählen nur einmal gegen die Grenze von 50 (bestätigt).
**Answered by:** Jakob Albrecht, 2026-11-10
**Resolved in:** UC-001 BR-010

## OQ-051: Welchen Wortlaut haben die Hinweise, die Zusammenfassung und das Ergebnis beim Erstellen der Einladungen?

**Element:** UC-001
**Raised by:** /use-case-implement, 2026-11-10
**Blocking:** No
**Status:** Resolved

A1, A2, Schritt 6 und Schritt 10 sprechen von Hinweisen, einer Zusammenfassung und einem Ergebnis, nennen aber keinen Wortlaut. Das Figma-Mockup zeigt Texte für die meisten Fälle.

**Answer:** Der Wortlaut des Figma-Mockups gilt, ergänzt um die Vorschläge für die Fälle ohne Mockup-Text: A1 ungültige Adresse „Mindestens eine Adresse ist ungültig: <Adresse>. Bitte korrigieren.“, keine Adresse „Bitte mindestens eine Adresse eingeben.“, mehr als 50 Adressen „Höchstens 50 Adressen je Durchgang.“, Gültigkeit „Die Zahl muss zwischen 1 und 30 liegen.“; A2 „Noch keine Einladung möglich“ mit „Es gibt noch keine Rolle mit freigegebenem Fragenkatalog. Bitte wenden Sie sich an einen Administrator.“; Zusammenfassung „Einladungen versenden?“ mit „Es werden <n> Einladungen für <MAG>, Rolle <Rolle> erstellt und per E-Mail versendet. Gültigkeit: <t> Tage.“; Ergebnis „<n> Einladungen versendet“ und „<m> Einladung nicht versendet“.
**Answered by:** Jakob Albrecht, 2026-11-10
**Resolved in:** UC-001 A1, UC-001 A2

## OQ-052: Welchen Text hat die E-Mail der Einladung?

**Element:** UC-001 BR-007
**Raised by:** /use-case-implement, 2026-11-10
**Blocking:** No
**Status:** Resolved

BR-007 nennt den Betreff und verlangt „einen kurzen Text“ und den Link, ohne den Text zu nennen.

**Answer:** „Sie sind zu einem devace-Assessment eingeladen. Über den folgenden Link starten Sie es:“, danach der Link, sonst nichts.
**Answered by:** Jakob Albrecht, 2026-11-10
**Resolved in:** UC-001 BR-007

## OQ-053: Gilt die Adresse „ben.muster@example“ als gültig?

**Element:** UC-001 BR-006
**Raised by:** /vaadin-flow-ui, 2026-11-10
**Blocking:** No
**Status:** Resolved
**Assumed:** BR-006 gilt wörtlich: Die Adresse hat einen lokalen Teil, das Zeichen @ und einen Domänenteil ohne Leerzeichen; „ben.muster@example“ ist gültig.

Das Mockup „Eingabe prüfen (A1)“ zeigt „ben.muster@example“ als ungültige Adresse. Nach BR-006 genügt ein Domänenteil; ein Punkt ist nicht verlangt. Die Oberfläche folgt der Spezifikation.

**Answer:** Ungültig: Der Domänenteil braucht mindestens einen Punkt. BR-006 wird verschärft; das Mockup gilt.
**Answered by:** Jakob Albrecht, 2026-11-10
**Resolved in:** UC-001 BR-006

## OQ-054: Wird bei doppelten Adressen die Schreibweise unterschieden?

**Element:** UC-001 BR-005
**Raised by:** /use-case-implement, 2026-11-10
**Blocking:** No
**Status:** Resolved
**Assumed:** Zwei Adressen gelten als dieselbe, wenn sie sich nur in Groß- und Kleinschreibung oder in umgebenden Leerzeichen unterscheiden; die erste Schreibweise wird verwendet.

BR-005 nennt „dieselbe Adresse“, ohne die Schreibweise zu regeln. Ohne Regel erhielte „Anna@example.com“ und „anna@example.com“ zwei Einladungen.

**Answer:** Nicht unterscheiden (bestätigt): Groß- und Kleinschreibung und umgebende Leerzeichen zählen nicht, die erste Schreibweise gilt.
**Answered by:** Jakob Albrecht, 2026-11-10
**Resolved in:** UC-001 BR-005

## OQ-055: Was geschieht, wenn der Teamleitung keine MAG zugeordnet ist?

**Element:** UC-001
**Raised by:** /use-case-coverage, 2026-11-10
**Blocking:** No
**Status:** Resolved

Die Vorbedingung verlangt mindestens eine zugeordnete MAG. Die Spezifikation nennt keinen Ablauf für den Fall, dass sie nicht erfüllt ist. Die Oberfläche zeigt dann ein leeres Auswahlfeld für die MAG; „Einladungen erstellen und versenden“ löst einen Fehler aus. Lesarten: (a) die Vorbedingung gilt als Vertrag, ein Ablauf ist nicht nötig; (b) neuer Ablauf wie A2 mit Hinweis und Ende des Use Cases.

**Answer:** Die Vorbedingung gilt als Vertrag; ein eigener Ablauf ist nicht nötig. Die Oberfläche muss den Fall ohne Fehler überstehen.
**Answered by:** Jakob Albrecht, 2026-11-10
**Resolved in:** UC-001

## OQ-056: Wo wird das Ende der Gültigkeit einer Einladung geprüft?

**Element:** UC-001 BR-008
**Raised by:** /use-case-coverage, 2026-11-10
**Blocking:** No
**Status:** Resolved

BR-008 lässt die Einladung bis 24:00 Uhr des letzten Gültigkeitstags gelten. UC-001 speichert nur Erstellzeitpunkt und Zahl der Tage; eine Prüfung des Ablaufs gibt es nicht und kein Test belegt das Ende. Lesarten: (a) die Prüfung gehört zu dem Use Case, der die Einladung einlöst (UC-002), UC-001 speichert nur die Angaben; (b) UC-001 legt das Ablaufdatum selbst fest und speichert es.

**Answer:** UC-001 berechnet und speichert das Ablaufdatum selbst; das Einlösen prüft nur dieses Datum.
**Answered by:** Jakob Albrecht, 2026-11-10
**Resolved in:** UC-001 step 8, UC-001 BR-008

## OQ-057: Ist der Tag der Erstellung der erste Tag der Gültigkeitsdauer?

**Element:** Entity: EINLADUNG.laeuft_ab_am
**Raised by:** /entity-model, 2026-11-10
**Blocking:** No
**Status:** Open
**Assumed:** Der Tag der Erstellung zählt als erster Tag: Eine Einladung mit einem Tag Gültigkeit gilt bis 24 Uhr des Erstellungstags; das Ablaufdatum ist der Tag von erstellt_am plus gueltigkeit_tage minus 1 Tag.

BR-008 lässt die Gültigkeit mit der Erstellung beginnen und bis 24 Uhr des letzten Tages gelten, nennt aber nicht, ob der Erstellungstag mitzählt. Lesart (b): Der Erstellungstag zählt nicht, das Ablaufdatum ist der Tag von erstellt_am plus gueltigkeit_tage.
