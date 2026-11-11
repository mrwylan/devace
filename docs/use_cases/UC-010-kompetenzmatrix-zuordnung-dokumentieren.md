# Use Case: Kompetenzmatrix-Zuordnung dokumentieren

## Übersicht

**Use-Case-ID:** UC-010  
**Use-Case-Name:** Kompetenzmatrix-Zuordnung dokumentieren  
**Primärer Akteur:** Administrator  
**Ziel:** Der Administrator hält fest, welche Spalten der Kompetenzmatrix welchem Handlungsfeld zugeordnet sind, damit die Verdichtung der Matrix zu Handlungsfeldern nachvollziehbar ist.  
**Auslösendes Ereignis:** Der Administrator fordert die Dokumentation der Zuordnung an.  
**Status:** Genehmigt  

**Anforderungen:** [FR-017, NFR-005, NFR-010, C-007, C-008](../requirements.md)  
**Schichten:** UI  

## Vorbedingungen

- Der Administrator ist mit Benutzername und Passwort angemeldet.
- Die sechs Handlungsfelder sind als feste Stammdaten vorgegeben.

## Hauptablauf

1. Der Administrator öffnet die Dokumentation der Zuordnung.
2. Das System zeigt die vorhandenen Zuordnungen je Handlungsfeld.
3. Der Administrator legt eine Zuordnung an, ändert oder entfernt sie, mit dem Namen der Matrix-Spalte, dem Handlungsfeld und optional einer Begründung.
4. Der Administrator speichert die Änderung.
5. Das System prüft die Angaben, speichert die Änderung und zeigt die aktualisierten Zuordnungen.

## Alternativabläufe

### A1: Angaben fehlen oder sind ungültig

**Auslöser:** Der Name der Spalte fehlt oder die Angaben überschreiten die zulässige Länge (Schritt 5)  
**Ablauf:**

1. Das System weist auf die fehlenden oder ungültigen Angaben hin.
2. Das System speichert die Änderung nicht.
3. Der Use Case wird bei Schritt 3 fortgesetzt.

### A2: Spalte bereits zugeordnet

**Auslöser:** Die Spalte ist bereits einem Handlungsfeld zugeordnet (Schritt 5)  
**Ablauf:**

1. Das System weist den Administrator auf die bestehende Zuordnung und ihr Handlungsfeld hin.
2. Das System speichert die Änderung nicht.
3. Der Use Case wird bei Schritt 3 fortgesetzt.

### A3: Administrator bricht ab

**Auslöser:** Der Administrator bricht die Änderung ab (Schritt 4)  
**Ablauf:**

1. Das System verwirft die Angaben.
2. Der Use Case endet.

## Nachbedingungen

### Erfolgsfall

- Die Zuordnungen sind so gespeichert, wie der Administrator sie angegeben hat.
- Jede Spalte der Kompetenzmatrix ist höchstens einem Handlungsfeld zugeordnet.

### Fehlerfall

- Die gespeicherten Zuordnungen sind unverändert.
- Keine Spalte ist mehr als einem Handlungsfeld zugeordnet.

## Geschäftsregeln

### BR-001: Zuordnung

Eine Zuordnung verbindet eine Spalte der Kompetenzmatrix mit genau einem der sechs Handlungsfelder. Der Name der Spalte ist eine Pflichtangabe mit höchstens 200 Zeichen. Die Begründung ist optional und hat höchstens 500 Zeichen.

### BR-002: Eine Spalte, ein Handlungsfeld

Eine Spalte der Kompetenzmatrix ist höchstens einem Handlungsfeld zugeordnet. Der Name der Spalte gilt als derselbe, wenn er sich nur in Groß- und Kleinschreibung oder in umgebenden Leerzeichen unterscheidet.

### BR-003: Matrix bleibt Quelle

Die Kompetenzmatrix bleibt die fachliche Quelle und wird von devace nicht gepflegt. Das System speichert nur den Namen der Spalte und ihre Zuordnung, keine Werte der Matrix, und bindet keine HR-Systeme an.

### BR-004: Nur für Administratoren

Nur der angemeldete Administrator dokumentiert die Zuordnung. Aufrufe ohne Anmeldung und passende Berechtigung werden abgewiesen. Die Navigation zeigt Einträge, für die der Administrator keine passende Berechtigung hat, deaktiviert.
