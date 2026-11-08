# Open Questions

Questions about the specification that the specification does not answer yet. A question is `Resolved` only when
the answer is in the specification itself; **Resolved in** names where. Resolved and withdrawn questions stay as
history.

## OQ-001: Frist für die Löschung der Antworten nach dem Textreport?

**Element:** NFR-003
**Raised by:** /requirements, 2026-11-07
**Blocking:** No
**Status:** Open
**Assumed:** 1 Stunde nach Erstellung des Textreports.

Die Vision verlangt Löschung nach Erstellung des Textreports, nennt aber keine Frist.

## OQ-002: Was geschieht mit Antworten eines abgebrochenen Assessments?

**Element:** NFR-004
**Raised by:** /requirements, 2026-11-07
**Blocking:** No
**Status:** Open
**Assumed:** Löschung 7 Tage nach der letzten Eingabe.

Die Vision regelt nur den Abschluss. Ohne Regel bliebe Freitext unbegrenzt gespeichert.

## OQ-003: Welche Rollen und welche Anmeldung berechtigen zum Zugriff auf die Auswertung?

**Element:** NFR-005
**Raised by:** /requirements, 2026-11-07
**Blocking:** No
**Status:** Open
**Assumed:** Alle drei Rollen melden sich an; Befragte nutzen nur die Einladung.

Die Antwort der Teamleitung nennt Anmeldung nur für die Teamleitung. Für Schulungsverantwortliche und Administrator wurde dasselbe angenommen.

## OQ-004: Sehen Schulungsverantwortliche die Radar-Charts aller MAGs?

**Element:** FR-011
**Raised by:** /requirements, 2026-11-07
**Blocking:** No
**Status:** Open
**Assumed:** Alle MAGs, nur Radar-Charts, keine Einladungen.

Die Vision nennt ihren Nutzen (Bedarf priorisieren), aber nicht den Umfang ihrer Sicht.

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
**Status:** Open
**Assumed:** 10 s (95 %), 3 s (95 %), 50 gleichzeitige Assessments.

Die Vision nennt keine Zahlen. Die Werte sind Vorschläge für den Pilot mit Lernenden.

## OQ-008: Schwelle für die faire Bewertung?

**Element:** NFR-009
**Raised by:** /requirements, 2026-11-07
**Blocking:** No
**Status:** Open
**Assumed:** Mindestens 90 % der Testfälle ergeben die erwartete Stufe.

Die Vision verlangt Testfälle, nennt aber keine Erfolgsquote.

## OQ-009: Gilt WCAG 2.1 AA und ausschließlich Deutsch?

**Element:** NFR-010
**Raised by:** /requirements, 2026-11-07
**Blocking:** No
**Status:** Open
**Assumed:** Deutsch; WCAG 2.1 AA.

Die Vision nennt weder Sprache noch Zugänglichkeitsstufe.

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
**Status:** Open
**Assumed:** 500 Zeichen.

Die Vision nennt keine Grenze.

## OQ-015: Welche Rollen darf die Teamleitung für eine Einladung wählen?

**Element:** UC-001 GR-003
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Open
**Assumed:** Nur Rollen mit freigegebenem Fragenkatalog stehen zur Wahl (A2, wenn keine existiert).

Eine Einladung für eine Rolle ohne freigegebenen Fragenkatalog wäre nicht nutzbar. Die Anforderungen nennen die Prüfung nicht.

## OQ-016: Wie werden doppelte Adressen in einer Eingabe behandelt?

**Element:** UC-001 GR-005
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Open
**Assumed:** Dieselbe Adresse mehrfach in einer Eingabe erzeugt eine Einladung.

Da Adressen nicht gespeichert werden, lassen sich Doppelungen nur innerhalb einer Eingabe erkennen.

## OQ-017: Gibt es eine Höchstzahl an Adressen je Durchgang?

**Element:** UC-001 step 4
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Open
**Assumed:** Keine Obergrenze.

Die Wahl 'mehrere Adressen je Durchgang' nennt keine Obergrenze.

## OQ-018: Wie wird eine Teamleitung einer MAG zugeordnet?

**Element:** FR-015
**Raised by:** /spec-review UC-001 (angenommen von /use-case-spec), 2026-11-08
**Blocking:** No
**Status:** Open
**Assumed:** Der Administrator ordnet in UC-008 jeder MAG eine Teamleitung zu.

UC-001 setzt voraus, dass der Teamleitung mindestens eine MAG zugeordnet ist (Entity MAG.teamleitung_id). FR-015 nennt die Zuordnung nicht.

## OQ-019: Wer pflegt den Starthinweis, und in welchem Use Case?

**Element:** FR-004
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Open
**Assumed:** Der Administrator pflegt den Starthinweis; ein Use Case dafür fehlt noch (Hand-off an /requirements und /use-case-diagram).

UC-002 setzt voraus, dass ein Starthinweis hinterlegt ist. Kein Use Case im Diagramm pflegt ihn (Text, KI-Modelle, Version).

## OQ-020: Wird der Starthinweis bei der Wiederaufnahme erneut gezeigt?

**Element:** UC-002 A2
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Open
**Assumed:** Nein, der bereits bestätigte Starthinweis gilt für das laufende Assessment.

Die Vision verlangt den Hinweis vor jedem Assessment. Bei der Wiederaufnahme läuft dasselbe Assessment weiter.

## OQ-021: Verfällt eine Einladung, mit der noch kein Assessment gestartet wurde?

**Element:** UC-002 GR-001
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Resolved

Die Antwort zum Review sagt, eine nicht eingelöste Einladung verfalle. Gemeint ist angenommen die Einladung eines gelöschten, nicht abgeschlossenen Assessments. Auf Einladungen, die nie geöffnet wurden, trifft das nicht zu.

**Answer:** Die Teamleitung legt beim Erstellen eine Gültigkeitsdauer von 1 bis 30 Tagen fest (Vorschlag 30). Danach ist die Einladung abgelaufen; ein gestartetes Assessment läuft weiter. Eine Einladung eines gelöschten Assessments ist eingelöst und nicht wiederverwendbar.
**Answered by:** Jakob Albrecht, 2026-11-08
**Resolved in:** UC-001 GR-008, UC-002 GR-001

## OQ-022: Wann endet die Gültigkeit einer Einladung genau?

**Element:** UC-001 GR-008
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Open
**Assumed:** Die Gültigkeit beginnt mit der Erstellung und endet nach Ablauf des letzten Tages der Gültigkeitsdauer (24 Uhr).

Die Gültigkeitsdauer wird in ganzen Tagen angegeben; Beginn und Ende innerhalb eines Tages sind nicht festgelegt.

## OQ-023: Wie meldet sich die Teamleitung an?

**Element:** UC-001
**Raised by:** /use-case-implement, 2026-11-08
**Blocking:** No
**Status:** Resolved

UC-001 setzt voraus, dass die Teamleitung angemeldet ist (NFR-005). Keine Spezifikation legt das Verfahren fest, und der Benutzerbestand (BENUTZER) ist im Entity-Modell nur eine externe Systemgrenze.

**Answer:** Formular-Anmeldung mit Benutzername und Passwort gegen einen Benutzerbestand des Systems. Zugangsdaten stehen nie im Repository.
**Answered by:** Jakob Albrecht, 2026-11-08
**Resolved in:** UC-001 GR-009

## OQ-024: Wie sieht die Einladungs-E-Mail aus?

**Element:** UC-001 GR-007
**Raised by:** /use-case-implement, 2026-11-08
**Blocking:** No
**Status:** Resolved

GR-007 sagt, die E-Mail enthalte nur die Einladung. Betreff, Text, Absender und die Form des Links sind nicht festgelegt.

**Answer:** Kurztext mit Link: Betreff "Einladung zum devace-Assessment", ein Satz und ein Link zur Einladung. Absender und Basis-Adresse des Links kommen aus der Konfiguration.
**Answered by:** Jakob Albrecht, 2026-11-08
**Resolved in:** UC-001 GR-007

## OQ-025: Wer legt Benutzerkonten an und weist ihnen die Rolle Teamleitung, Schulungsverantwortliche oder Administrator zu?

**Element:** UC-008 GR-004
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Open
**Assumed:** Die Verwaltung der Benutzerkonten liegt außerhalb von UC-008; UC-008 wählt die Teamleitung aus vorhandenen Teamleitungen. Hand-off an /requirements für eine Anforderung und an /use-case-diagram für einen Use Case.

Die Anmeldung (OQ-023) setzt Benutzerkonten voraus. Keine Anforderung und kein Use Case legt sie an oder ändert sie.

## OQ-026: Wird bei der Eindeutigkeit von MAG und Rolle die Schreibweise unterschieden?

**Element:** UC-008 GR-002
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Open
**Assumed:** Nein, Groß- und Kleinschreibung sowie umgebende Leerzeichen bleiben beim Vergleich unberücksichtigt.

Ohne diese Regel entstünden "Entwicklung" und "entwicklung" als zwei Rollen mit getrennten Radar-Charts.

## OQ-027: Wie viele Entwürfe hat eine Rolle?

**Element:** UC-006 GR-004
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Open
**Assumed:** Höchstens einen. Besteht ein Entwurf, setzt der Administrator ihn fort; ein weiterer Entwurf entsteht nicht.

Das Entity-Modell begrenzt nur die freigegebenen Fragenkataloge je Rolle auf einen. Mehrere parallele Entwürfe würden die Freigabe (UC-007) mehrdeutig machen.

## OQ-028: Wie viele Fragen je Handlungsfeld darf der Administrator für den KI-Entwurf anfordern?

**Element:** UC-006 GR-003
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Open
**Assumed:** 1 bis 10, Vorschlag 3.

Die Vision nennt keine Zahl. Die Grenze ist ein Vorschlag für den Pilot mit Lernenden.

## OQ-029: Darf ein Entwurf Handlungsfelder ohne Frage enthalten?

**Element:** UC-006 GR-006
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Open
**Assumed:** Ja. Ob ein Fragenkatalog vollständig genug für die Freigabe ist (zum Beispiel mindestens eine Frage je Handlungsfeld), prüft UC-007.

Ohne Frage in einem Handlungsfeld ließe sich kein Score für dieses Handlungsfeld berechnen. Das betrifft die Freigabe, nicht die Erstellung.
