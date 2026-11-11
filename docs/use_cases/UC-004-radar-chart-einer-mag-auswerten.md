# Use Case: Radar-Chart einer MAG auswerten

## Übersicht

**Use-Case-ID:** UC-004  
**Use-Case-Name:** Radar-Chart einer MAG auswerten  
**Primärer Akteur:** Teamleitung  
**Ziel:** Die Teamleitung sieht das Radar-Chart ihrer MAG je Rolle, damit sie Stärken und Lücken auf einen Blick erkennt und Entwicklungsgespräche an Handlungsfeldern ausrichtet.  
**Auslösendes Ereignis:** Die Teamleitung fordert die Auswertung ihrer MAG an.  
**Status:** Entwurf  

**Anforderungen:** [FR-010, NFR-002, NFR-005, NFR-007, NFR-010, C-002, C-003, C-005](../requirements.md)  
**Schichten:** UI  

## Vorbedingungen

- Die Teamleitung ist mit Benutzername und Passwort angemeldet.
- Der Teamleitung ist mindestens eine MAG zugeordnet.

## Hauptablauf

1. Die Teamleitung öffnet die Auswertung.
2. Das System zeigt die MAGs der Teamleitung, zu denen Scores vorliegen, zur Auswahl an, bei der gewählten MAG die Rollen, zu denen Scores vorliegen.
3. Die Teamleitung wählt eine MAG und eine Rolle.
4. Das System zeigt das Radar-Chart der MAG und Rolle mit dem Wert je Handlungsfeld.

## Alternativabläufe

### A1: Keine Scores vorhanden

**Auslöser:** Zu keiner MAG der Teamleitung liegt ein Score vor (Schritt 2)  
**Ablauf:**

1. Das System weist die Teamleitung darauf hin, dass noch keine Auswertung möglich ist.
2. Der Use Case endet.

### A2: Teamleitung wechselt MAG oder Rolle

**Auslöser:** Die Teamleitung wählt eine andere MAG oder Rolle (Schritt 4)  
**Ablauf:**

1. Das System zeigt das Radar-Chart zur neuen Auswahl.
2. Der Use Case wird bei Schritt 3 fortgesetzt.

## Nachbedingungen

### Erfolgsfall

- Die Teamleitung sieht das Radar-Chart der gewählten MAG und Rolle.
- Es sind keine Daten geändert.

### Fehlerfall

- Es werden keine Daten geändert.
- Das System zeigt keine Scores einer MAG, die der Teamleitung nicht zugeordnet ist.

## Geschäftsregeln

### BR-001: Werte des Radar-Charts

Der Wert eines Handlungsfelds ist der Mittelwert aller gespeicherten Scores dieses Handlungsfelds der gewählten MAG und Rolle, unabhängig vom Fragenkatalog, auf dem sie beruhen. Er liegt zwischen 0 und 5 und wird kaufmännisch auf eine Nachkommastelle gerundet. Die Skala des Radar-Charts zeigt nur die Ganzzahlen an.

### BR-002: Nur eigene MAGs

Die Teamleitung wertet nur MAGs aus, die ihr zugeordnet sind.

### BR-003: Auch kleine MAGs

Das System erstellt das Radar-Chart auch für kleine MAGs. Es verlangt keine Mindestzahl an Scores.

### BR-004: Nur MAGs und Rollen mit Scores

Zur Auswahl stehen nur MAGs der Teamleitung, zu denen mindestens ein Score vorliegt, und nur Rollen, zu denen die gewählte MAG mindestens einen Score hat.

### BR-005: Nur Mittelwerte

Das System zeigt je Handlungsfeld nur den Mittelwert nach BR-001. Es zeigt keinen einzelnen Score, keine Zahl von Teilnehmern und keinen Verweis auf eine Einladung, ein Assessment oder eine Person.

### BR-006: Handlungsfeld ohne Score

Hat die MAG in einem Handlungsfeld keinen Score, zeigt das Radar-Chart für dieses Handlungsfeld keinen Wert.

### BR-007: Nur für angemeldete Teamleitungen

Nur die angemeldete Teamleitung wertet Radar-Charts aus. Aufrufe ohne Anmeldung und passende Berechtigung werden abgewiesen. Die Navigation zeigt Einträge, für die die Teamleitung keine passende Berechtigung hat, deaktiviert.

### BR-008: Keine Leistungsbeurteilung

Das Radar-Chart ist kein Bestandteil der Leistungsbeurteilung.
