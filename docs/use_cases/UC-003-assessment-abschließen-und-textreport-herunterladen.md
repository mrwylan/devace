# Use Case: Assessment abschließen und Textreport herunterladen

## Übersicht

**Use-Case-ID:** UC-003  
**Use-Case-Name:** Assessment abschließen und Textreport herunterladen  
**Primärer Akteur:** Befragte  
**Sekundäre Akteure:** KI-Modell  
**Ziel:** Die Befragte schließt ihr Assessment ab, sieht ihre Auswertung mit Radar-Chart, Scores und Textreport und lädt den Textreport einmalig herunter, damit sie ihre Ergebnisse selbst verwaltet und von ihrem Assessment nur die Scores bleiben.  
**Auslösendes Ereignis:** Die Befragte fordert den Abschluss ihres Assessments an.  
**Status:** Entwurf  

**Anforderungen:** [FR-007, FR-008, FR-009, NFR-001, NFR-002, NFR-003, NFR-010, C-002, C-003, C-005](../requirements.md)  
**Schichten:** UI

## Vorbedingungen

- Zur Einladung der Befragten besteht ein laufendes Assessment, in dem jede Frage beantwortet und einer Stufe zugeordnet ist.

## Hauptablauf

1. Die Befragte schließt ihr Assessment ab.
2. Das System berechnet je Handlungsfeld den Score aus den Stufen ihrer Antworten.
3. Das System lässt das KI-Modell den Textreport erstellen, der ihre Leistung würdigt, ihre Einstufung begründet und Hinweise auf ihre nächsten Investitionen gibt.
4. Das System speichert die Scores ohne Personenbezug und setzt das Assessment auf abgeschlossen.
5. Das System zeigt der Befragten ihre Auswertung mit dem Radar-Chart, den erreichten Scores je Handlungsfeld und dem Textreport.
6. Die Befragte lädt den Textreport herunter.
7. Das System stellt den Textreport einmalig bereit und löscht danach die Antworten des Assessments.

## Alternativabläufe

### A1: KI-Modell liefert keinen Textreport

**Auslöser:** Das KI-Modell ist nicht erreichbar oder liefert keinen Textreport (Schritt 3)  
**Ablauf:**

1. Das System weist die Befragte darauf hin, dass der Textreport nicht erstellt werden konnte.
2. Das System speichert keine Scores und lässt das Assessment laufend.
3. Der Use Case wird bei Schritt 1 fortgesetzt.

### A2: Befragte lädt den Textreport nicht herunter

**Auslöser:** Die Befragte verlässt die Auswertung, ohne den Textreport herunterzuladen (Schritt 6)  
**Ablauf:**

1. Das System bietet den Textreport nicht erneut an.
2. Das System löscht die Antworten des Assessments spätestens 1 Stunde nach Erstellung des Textreports.
3. Der Use Case endet.

### A3: Textreport nicht mehr verfügbar

**Auslöser:** Die Befragte fordert den Textreport an, nachdem er heruntergeladen wurde oder verfallen ist (Schritt 6)  
**Ablauf:**

1. Das System weist die Befragte darauf hin, dass der Textreport nicht mehr zur Verfügung steht.
2. Der Use Case endet.

## Nachbedingungen

### Erfolgsfall

- Je Handlungsfeld mit Fragen ist ein Score gespeichert, der MAG und Rolle der Befragten zugeordnet ist und auf keine Einladung, kein Assessment und keine Person verweist.
- Das Assessment ist abgeschlossen.
- Die Befragte hat den Textreport erhalten.
- Alle Antworten des Assessments sind gelöscht.

### Fehlerfall

- Ohne Textreport ist kein Score gespeichert und das Assessment nicht abgeschlossen.
- Kein gespeicherter Score und kein Textreport verweist auf eine Einladung, eine E-Mail-Adresse oder eine Person.
- Das KI-Modell erhält keine Angabe, die auf die Befragte schließen lässt.
- Eine bereits gespeicherte Antwort mit ihrer Stufe bleibt unverändert, bis das Assessment abgeschlossen ist oder nach 7 Tagen gelöscht wird.

## Geschäftsregeln

### GR-001: Score je Handlungsfeld

Der Score eines Handlungsfelds ist die Summe der Stufen aller Antworten dieses Handlungsfelds geteilt durch die Anzahl seiner Fragen. Er liegt zwischen 0 und 5 und hat höchstens zwei Nachkommastellen. Ein Handlungsfeld ohne Frage hat keinen Score.

### GR-002: Scores ohne Personenbezug

Das System speichert je Score nur MAG, Rolle, Handlungsfeld, Fragenkatalog, Wert und Datum. Es speichert keinen Verweis auf eine Einladung, ein Assessment, eine Antwort, eine E-Mail-Adresse oder eine Person. Die Scores werden fünf Jahre aufbewahrt.

### GR-003: Inhalt des Textreports

Der Textreport ist in deutscher Sprache. Er würdigt je Handlungsfeld die Leistung der Befragten, begründet die Einstufung und gibt Hinweise auf ihre nächsten Investitionen. Scores und Textreport sind kein Bestandteil der Leistungsbeurteilung.

### GR-004: Anonyme Vorlage an das KI-Modell

Das System legt dem KI-Modell nur die Rolle, die Handlungsfelder, die Fragen mit den Antworten und ihren zugeordneten Stufen sowie die Scores vor, aber weder die Einladung noch die E-Mail-Adresse oder eine andere Angabe, die auf die Befragte schließen lässt.

### GR-005: Auswertung nur für die Befragte

Radar-Chart, Scores und Textreport zeigt das System nur der Befragten, die das Assessment abgeschlossen hat, und nur beim Abschluss. Es speichert sie nicht für eine spätere Anzeige.

### GR-006: Einmaliger Download

Der Textreport steht der Befragten einmal zum Download bereit. Nach dem Download, nach seinem Verfall und bei erneuter Anforderung steht er nicht mehr zur Verfügung.

### GR-007: Löschung der Antworten

Die Antworten eines Assessments werden nach dem Download des Textreports gelöscht, spätestens 1 Stunde nach seiner Erstellung. Die Scores bleiben erhalten.
