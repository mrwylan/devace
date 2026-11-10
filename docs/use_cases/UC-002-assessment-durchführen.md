# Use Case: Assessment durchführen

## Übersicht

**Use-Case-ID:** UC-002  
**Use-Case-Name:** Assessment durchführen  
**Primärer Akteur:** Befragte  
**Sekundäre Akteure:** KI-Modell  
**Ziel:** Die Befragte beantwortet alle Fragen ihres Fragenkatalogs im Dialog, sodass jede Antwort einer Stufe zugeordnet ist.  
**Auslösendes Ereignis:** Die Befragte ruft das Assessment über ihre Einladung auf.  
**Status:** Genehmigt  

**Anforderungen:** [FR-003, FR-004, FR-005, FR-006, NFR-001, NFR-004, NFR-006, NFR-010, C-001, C-005](../requirements.md)  
**Schichten:** UI  
**Mockup:** [Starthinweis](https://www.figma.com/design/SSeuF968QrgtXzD5Wvna6x/devace-%E2%80%93-Application?node-id=33-3403), [Frage beantworten](https://www.figma.com/design/SSeuF968QrgtXzD5Wvna6x/devace-%E2%80%93-Application?node-id=33-3586), [Antwort zu lang (A4)](https://www.figma.com/design/SSeuF968QrgtXzD5Wvna6x/devace-%E2%80%93-Application?node-id=33-3794), [Alle Fragen beantwortet](https://www.figma.com/design/SSeuF968QrgtXzD5Wvna6x/devace-%E2%80%93-Application?node-id=33-3988), [Einladung nicht nutzbar (A1)](https://www.figma.com/design/SSeuF968QrgtXzD5Wvna6x/devace-%E2%80%93-Application?node-id=33-4094), [Wiederaufnahme (A2)](https://www.figma.com/design/SSeuF968QrgtXzD5Wvna6x/devace-%E2%80%93-Application?node-id=33-4179)

## Vorbedingungen

- Die Befragte besitzt eine Einladung.
- Ein Starthinweis ist hinterlegt.
- Für die Rolle der Einladung ist ein freigegebener Fragenkatalog hinterlegt.

## Hauptablauf

1. Die Befragte öffnet die Einladung aus der E-Mail.
2. Das System zeigt den Starthinweis mit den eingesetzten KI-Modellen, den nicht zu erfassenden Angaben und dem Hinweis auf die Firmenweisungen zum Umgang mit KI.
3. Die Befragte bestätigt den Starthinweis.
4. Das System startet das Assessment und stellt die erste Frage des Fragenkatalogs ihrer Rolle.
5. Die Befragte beantwortet die Frage in Freitext.
6. Das System legt die Antwort dem KI-Modell vor, das sie nach dem Entscheidungsmodell einer Stufe zuordnet.
7. Das System speichert Antwort und Stufe und stellt die nächste Frage. Die Schritte 5 bis 7 wiederholen sich, bis alle Fragen beantwortet sind.
8. Das System meldet, dass alle Fragen beantwortet sind, und bietet den Abschluss des Assessments an.

## Alternativabläufe

### A1: Einladung unbekannt, eingelöst oder abgelaufen

**Auslöser:** Die Einladung ist unbekannt, bereits eingelöst oder abgelaufen, ohne dass ein laufendes Assessment besteht (Schritt 1)  
**Ablauf:**

1. Das System weist die Befragte darauf hin, dass die Einladung bereits eingelöst wurde oder abgelaufen ist.
2. Der Use Case endet.

### A2: Laufendes Assessment wiederaufnehmen

**Auslöser:** Zur Einladung gibt es ein laufendes Assessment (Schritt 1)  
**Ablauf:**

1. Das System zeigt den Starthinweis nicht erneut.
2. Das System stellt die erste Frage ohne Antwort.
3. Der Use Case wird bei Schritt 5 fortgesetzt.

### A3: Befragte bestätigt den Starthinweis nicht

**Auslöser:** Die Befragte bestätigt den Starthinweis nicht (Schritt 3)  
**Ablauf:**

1. Das System startet das Assessment nicht.
2. Der Use Case endet.

### A4: Antwort leer oder zu lang

**Auslöser:** Die Antwort ist leer oder überschreitet die zulässige Länge (Schritt 5)  
**Ablauf:**

1. Das System weist auf die fehlende oder zu lange Antwort hin.
2. Der Use Case wird bei Schritt 5 fortgesetzt.

### A5: Bewertung nicht möglich

**Auslöser:** Das KI-Modell ist nicht erreichbar oder ordnet der Antwort keine Stufe von 0 bis 5 zu (Schritt 6)  
**Ablauf:**

1. Das System ordnet der Antwort die Stufe 0 zu.
2. Der Use Case wird bei Schritt 7 fortgesetzt.

### A6: Befragte unterbricht das Assessment

**Auslöser:** Die Befragte verlässt das Assessment vor der letzten Frage (Schritt 5)  
**Ablauf:**

1. Das System behält die bereits gegebenen Antworten.
2. Der Use Case endet.

## Nachbedingungen

### Erfolgsfall

- Zur Einladung ist ein laufendes Assessment mit dem bestätigten Starthinweis gespeichert.
- Zu jeder Frage des Fragenkatalogs ist eine Antwort mit zugeordneter Stufe gespeichert.
- Die Befragte kann das Assessment abschließen.

### Fehlerfall

- Ein Assessment ohne bestätigten Starthinweis wird nicht gestartet.
- Eine bereits gegebene Antwort bleibt mit ihrer Stufe unverändert.
- Das KI-Modell erhält keine Angabe, die auf die Befragte schließen lässt.

## Geschäftsregeln

### GR-001: Einladung gilt für ein Assessment

Eine Einladung berechtigt zu genau einem Assessment. Das Assessment muss innerhalb der Gültigkeitsdauer der Einladung gestartet werden, danach ist die Einladung abgelaufen. Mit dem Start gilt sie als eingelöst und kann nicht erneut verwendet werden, auch nicht, wenn das Assessment nach GR-009 gelöscht wurde. Ein gestartetes Assessment läuft nach Ablauf der Gültigkeitsdauer weiter.

### GR-002: Starthinweis vor dem Start

Das Assessment beginnt erst, nachdem die Befragte den Starthinweis bestätigt hat. Der Starthinweis nennt die eingesetzten KI-Modelle, die Angaben, die die Befragte nicht erfassen soll (zum Beispiel Namen von Personen oder Kundenprojekte), und die Geltung der Firmenweisungen zum Umgang mit KI für Textantworten.

### GR-003: Fragen der Rolle

Die Befragte erhält die Fragen des freigegebenen Fragenkatalogs der Rolle ihrer Einladung, in der Reihenfolge des Fragenkatalogs.

### GR-004: Länge der Antwort

Eine Antwort ist nicht leer und hat höchstens 500 Zeichen.

### GR-005: Eine endgültige Antwort je Frage

Jede Frage wird genau einmal beantwortet. Die Befragte ändert eine abgegebene Antwort nicht. Das KI-Modell stellt keine Nachfragen.

### GR-006: Stufenzuordnung

Das KI-Modell ordnet jede Antwort nach der Stufenvorgabe der Frage einer Stufe von 0 bis 5 zu. Ordnet es keine Stufe zu, gilt die Stufe 0. Eine Antwort erreicht eine höhere Stufe nur durch die Zuordnung des KI-Modells.

### GR-007: Anonyme Vorlage an das KI-Modell

Das System legt dem KI-Modell nur die Frage, ihre Stufenvorgaben und den Text der Antwort vor, aber weder die Einladung noch die E-Mail-Adresse oder eine andere Angabe, die auf die Befragte schließen lässt.

### GR-008: Stufe bleibt verborgen

Das System zeigt der Befragten die zugeordnete Stufe während des Assessments nicht an.

### GR-009: Aufbewahrung laufender Antworten

Die Antworten eines laufenden Assessments bleiben erhalten, bis es abgeschlossen wird oder 7 Tage seit der letzten Eingabe vergangen sind. Danach wird das Assessment gelöscht und die Einladung ist eingelöst.
