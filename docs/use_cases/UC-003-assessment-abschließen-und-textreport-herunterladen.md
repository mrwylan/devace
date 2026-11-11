# Use Case: Assessment abschließen und Textreport herunterladen

## Übersicht

**Use-Case-ID:** UC-003  
**Use-Case-Name:** Assessment abschließen und Textreport herunterladen  
**Primärer Akteur:** Teilnehmer  
**Sekundäre Akteure:** KI-Modell  
**Ziel:** Der Teilnehmer schließt sein Assessment ab, sieht seine Auswertung mit Radar-Chart, Scores und Textreport und lädt den Textreport einmalig herunter, damit er seine Ergebnisse selbst verwaltet und von seinem Assessment nur die Scores bleiben.  
**Auslösendes Ereignis:** Der Teilnehmer fordert den Abschluss seines Assessments an.  
**Status:** Genehmigt  

**Anforderungen:** [FR-007, FR-008, FR-009, FR-018, NFR-001, NFR-002, NFR-003, NFR-006, NFR-007, NFR-010, C-002, C-003, C-005](../requirements.md)  
**Schichten:** UI  
**Mockup:** [Auswertung](https://www.figma.com/design/SSeuF968QrgtXzD5Wvna6x/devace-%E2%80%93-Application?node-id=48-4323), [Textreport nicht erstellt (A1)](https://www.figma.com/design/SSeuF968QrgtXzD5Wvna6x/devace-%E2%80%93-Application?node-id=48-4487), [Textreport nicht mehr verfügbar (A3)](https://www.figma.com/design/SSeuF968QrgtXzD5Wvna6x/devace-%E2%80%93-Application?node-id=48-4564)

## Vorbedingungen

- Zur Einladung des Teilnehmers besteht ein laufendes Assessment, in dem jede Frage beantwortet und einer Stufe zugeordnet ist.

## Hauptablauf

1. Der Teilnehmer schließt sein Assessment ab.
2. Das System berechnet je Handlungsfeld den Score aus den Stufen seiner Antworten.
3. Das System lässt das KI-Modell den Textreport erstellen, der seine Leistung würdigt, seine Einstufung begründet und Hinweise auf seine nächsten Investitionen gibt.
4. Das System speichert die Scores ohne Personenbezug, setzt das Assessment auf abgeschlossen und löscht die Antworten des Assessments.
5. Das System zeigt dem Teilnehmer den Hinweis „Assessment abgeschlossen: Ihre Auswertung ist fertig. Radar-Chart, Scores und Textreport sehen nur Sie, und nur jetzt.“ und seine Auswertung mit dem Radar-Chart, den erreichten Scores je Handlungsfeld und dem Textreport, darunter den Hinweis „Der Textreport steht einmal zum Download bereit. Ihre Antworten sind bereits gelöscht, Ihre Scores bleiben ohne Personenbezug erhalten.“
6. Der Teilnehmer lädt den Textreport herunter.
7. Das System stellt den Textreport einmalig als PDF-Dokument bereit.

## Alternativabläufe

### A1: KI-Modell liefert keinen Textreport

**Auslöser:** Das KI-Modell ist nicht erreichbar, liefert nicht innerhalb von 10 Sekunden einen Textreport, liefert keinen Textreport oder einen, der eine Antwort des Teilnehmers wörtlich enthält (Schritt 3)  
**Ablauf:**

1. Das System zeigt den Hinweis „Textreport nicht erstellt: Das KI-Modell hat keinen Textreport geliefert. Es wurde nichts gespeichert, Ihre Antworten bleiben erhalten. Sie können den Abschluss erneut anfordern.“
2. Das System speichert keine Scores und lässt das Assessment laufend; die Antworten bleiben erhalten.
3. Der Use Case wird bei Schritt 1 fortgesetzt.

### A2: Teilnehmer lädt den Textreport nicht herunter

**Auslöser:** Der Teilnehmer verlässt die Auswertung, ohne den Textreport herunterzuladen (Schritt 6)  
**Ablauf:**

1. Das System bietet den Textreport nicht erneut an.
2. Der Use Case endet.

### A3: Textreport nicht mehr verfügbar

**Auslöser:** Der Teilnehmer fordert den Textreport an, nachdem er heruntergeladen wurde oder verfallen ist (Schritt 6)  
**Ablauf:**

1. Das System zeigt den Hinweis „Textreport nicht mehr verfügbar: Der Textreport wurde bereits heruntergeladen oder ist verfallen. Ihre Scores bleiben ohne Personenbezug gespeichert.“
2. Der Use Case endet.

## Nachbedingungen

### Erfolgsfall

- Je Handlungsfeld mit Fragen ist ein Score gespeichert, der MAG und Rolle des Teilnehmers zugeordnet ist und auf keine Einladung, kein Assessment und keine Person verweist.
- Das Assessment ist abgeschlossen und alle seine Antworten sind gelöscht.
- Hat der Teilnehmer den Textreport heruntergeladen, hat er ihn als PDF-Dokument erhalten.
- Hat der Teilnehmer ihn nicht heruntergeladen (A2), hat er den Textreport nicht erhalten; die Scores sind gespeichert und das Assessment ist abgeschlossen.

### Fehlerfall

- Ohne Textreport ist kein Score gespeichert und das Assessment nicht abgeschlossen.
- Kein gespeicherter Score und kein Textreport verweist auf eine Einladung, eine E-Mail-Adresse oder eine Person.
- Das KI-Modell erhält keine Angabe, die auf den Teilnehmer schließen lässt.
- Eine bereits gespeicherte Antwort mit ihrer Stufe bleibt unverändert, bis das Assessment abgeschlossen ist oder nach 7 Tagen gelöscht wird.

## Geschäftsregeln

### BR-001: Score je Handlungsfeld

Der Score eines Handlungsfelds ist die Summe der Stufen aller Antworten dieses Handlungsfelds geteilt durch die Anzahl seiner Fragen. Er liegt zwischen 0 und 5 und wird kaufmännisch auf eine Nachkommastelle gerundet, gespeichert und immer mit einer Nachkommastelle ausgegeben. Die Skala des Radar-Charts zeigt nur die Ganzzahlen an. Ein Handlungsfeld ohne Frage hat keinen Score.

### BR-002: Scores ohne Personenbezug

Das System speichert je Score nur MAG, Rolle, Handlungsfeld, Fragenkatalog, Wert und Datum. Es speichert keinen Verweis auf eine Einladung, ein Assessment, eine Antwort, eine E-Mail-Adresse oder eine Person. Die Scores werden fünf Jahre aufbewahrt.

### BR-003: Inhalt des Textreports

Der Textreport ist in deutscher Sprache. Er würdigt je Handlungsfeld die Leistung des Teilnehmers, begründet die Einstufung und gibt Hinweise auf seine nächsten Investitionen. Er enthält keine wörtlichen Antworttexte des Teilnehmers: Das System prüft den Textreport und behandelt ihn als nicht erstellt (A1), wenn er eine Folge von mindestens acht aufeinanderfolgenden Wörtern einer Antwort enthält. Scores und Textreport sind kein Bestandteil der Leistungsbeurteilung.

### BR-004: Anonyme Vorlage an das KI-Modell

Das System legt dem KI-Modell nur die Rolle, die Handlungsfelder, die Fragen mit den Antworten und ihren zugeordneten Stufen sowie die Scores vor, aber weder die Einladung noch die E-Mail-Adresse oder eine andere Angabe, die auf den Teilnehmer schließen lässt.

### BR-005: Auswertung nur für den Teilnehmer

Radar-Chart, Scores und Textreport zeigt das System nur dem Teilnehmer, der das Assessment abgeschlossen hat, und nur beim Abschluss. Es speichert sie nicht für eine spätere Anzeige.

### BR-006: Einmaliger Download

Der Textreport steht dem Teilnehmer einmal zum Download bereit und verfällt 1 Stunde nach seiner Erstellung. Nach dem Download, nach seinem Verfall und bei erneuter Anforderung steht er nicht mehr zur Verfügung.

### BR-007: Löschung der Antworten

Die Antworten eines Assessments werden beim Abschluss gelöscht, sobald Scores und Textreport vorliegen, noch vor dem Download. Die Scores bleiben erhalten.
