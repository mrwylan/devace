# Use Case: Starthinweis pflegen

## Übersicht

**Use-Case-ID:** UC-011  
**Use-Case-Name:** Starthinweis pflegen  
**Primärer Akteur:** Administrator  
**Ziel:** Der Administrator pflegt den Starthinweis, damit Teilnehmer vor jedem Assessment den aktuellen Hinweis zu den eingesetzten KI-Modellen, den nicht zu erfassenden Angaben und den Firmenweisungen zum Umgang mit KI lesen.  
**Auslösendes Ereignis:** Der Administrator fordert die Pflege des Starthinweises an.  
**Status:** Genehmigt  

**Anforderungen:** [FR-019, FR-004, NFR-005, NFR-010, C-001](../requirements.md)  
**Schichten:** UI  

## Vorbedingungen

- Der Administrator ist mit Benutzername und Passwort angemeldet.

## Hauptablauf

1. Der Administrator öffnet die Pflege des Starthinweises.
2. Das System zeigt den geltenden Starthinweis und die früheren Versionen mit Versionsnummer und Beginn der Gültigkeit.
3. Der Administrator erfasst eine neue Version mit dem Text des Hinweises, den eingesetzten KI-Modellen und dem Beginn der Gültigkeit.
4. Der Administrator speichert die neue Version.
5. Das System prüft die Angaben, legt die neue Version an und bestätigt dem Administrator die Änderung.

## Alternativabläufe

### A1: Angaben fehlen oder sind ungültig

**Auslöser:** Eine Pflichtangabe fehlt, eine Angabe überschreitet die zulässige Länge oder der Beginn der Gültigkeit ist ungültig (Schritt 5)  
**Ablauf:**

1. Das System weist auf die fehlenden oder ungültigen Angaben hin.
2. Das System legt keine Version an.
3. Der Use Case wird bei Schritt 3 fortgesetzt.

### A2: Administrator bricht ab

**Auslöser:** Der Administrator bricht die Pflege ab (Schritt 4)  
**Ablauf:**

1. Das System verwirft die Angaben.
2. Der Use Case endet.

## Nachbedingungen

### Erfolgsfall

- Die neue Version des Starthinweises ist mit Text, eingesetzten KI-Modellen, Versionsnummer und Beginn der Gültigkeit gespeichert.
- Alle früheren Versionen sind unverändert.

### Fehlerfall

- Es ist keine Version angelegt oder geändert.
- Der geltende Starthinweis ist unverändert.

## Geschäftsregeln

### BR-001: Inhalt des Starthinweises

Der Text des Starthinweises ist eine Pflichtangabe mit höchstens 500 Zeichen. Er nennt die Angaben, die der Teilnehmer nicht erfassen soll (zum Beispiel Namen von Personen oder Kundenprojekte), und die Geltung der Firmenweisungen zum Umgang mit KI für Textantworten. Die eingesetzten KI-Modelle sind eine Pflichtangabe mit höchstens 500 Zeichen.

### BR-002: Neue Version statt Änderung

Jede Änderung des Starthinweises erzeugt eine neue Version mit einer um 1 höheren Nummer. Frühere Versionen bleiben unverändert, damit nachvollziehbar ist, welchen Hinweis ein Teilnehmer bestätigt hat.

### BR-003: Beginn der Gültigkeit

Der Beginn der Gültigkeit ist ein Datum. Er liegt nicht in der Vergangenheit.

### BR-004: Geltender Starthinweis

Als Starthinweis gilt die Version mit dem jüngsten Beginn der Gültigkeit, der nicht in der Zukunft liegt. Ein Teilnehmer sieht beim Start eines Assessments die geltende Version.

### BR-005: Laufende Assessments

Ein neuer Starthinweis ändert nichts an einem laufenden Assessment. Der bereits bestätigte Starthinweis gilt für dieses Assessment (UC-002 BR-002, UC-002 A2).

### BR-006: Nur für Administratoren

Nur der angemeldete Administrator pflegt den Starthinweis. Aufrufe ohne Anmeldung und passende Berechtigung werden abgewiesen. Die Navigation zeigt Einträge, für die der Administrator keine passende Berechtigung hat, deaktiviert.
