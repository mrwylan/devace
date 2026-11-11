# Use Case: Radar-Charts für die Schulungsplanung auswerten

## Übersicht

**Use-Case-ID:** UC-005  
**Use-Case-Name:** Radar-Charts für die Schulungsplanung auswerten  
**Primärer Akteur:** Schulungsverantwortliche  
**Ziel:** Die Schulungsverantwortliche sieht die Radar-Charts aller MAGs je Rolle nebeneinander, damit sie den Schulungsbedarf anhand der Lücken priorisiert.  
**Auslösendes Ereignis:** Die Schulungsverantwortliche fordert die Auswertung für die Schulungsplanung an.  
**Status:** Entwurf  

**Anforderungen:** [FR-011, NFR-002, NFR-005, NFR-007, NFR-010, C-002, C-003, C-005](../requirements.md)  
**Schichten:** UI  

## Vorbedingungen

- Die Schulungsverantwortliche ist mit Benutzername und Passwort angemeldet.

## Hauptablauf

1. Die Schulungsverantwortliche öffnet die Auswertung für die Schulungsplanung.
2. Das System zeigt die Rollen, zu denen Scores vorliegen, zur Auswahl an.
3. Die Schulungsverantwortliche wählt eine Rolle.
4. Das System zeigt die Radar-Charts aller MAGs, zu denen Scores der Rolle vorliegen, nebeneinander mit dem Wert je Handlungsfeld.

## Alternativabläufe

### A1: Keine Scores vorhanden

**Auslöser:** Es liegt noch kein Score vor (Schritt 2)  
**Ablauf:**

1. Das System weist die Schulungsverantwortliche darauf hin, dass noch keine Auswertung möglich ist.
2. Der Use Case endet.

### A2: Schulungsverantwortliche wechselt die Rolle

**Auslöser:** Die Schulungsverantwortliche wählt eine andere Rolle (Schritt 4)  
**Ablauf:**

1. Das System zeigt die Radar-Charts zur neuen Rolle.
2. Der Use Case wird bei Schritt 3 fortgesetzt.

## Nachbedingungen

### Erfolgsfall

- Die Schulungsverantwortliche sieht die Radar-Charts aller MAGs mit Scores zur gewählten Rolle.
- Es sind keine Daten geändert.

### Fehlerfall

- Es werden keine Daten geändert.
- Das System zeigt keine Einladung, keine E-Mail-Adresse und keinen einzelnen Score.

## Geschäftsregeln

### BR-001: Werte der Radar-Charts

Der Wert eines Handlungsfelds einer MAG ist der Mittelwert aller gespeicherten Scores dieses Handlungsfelds der MAG und der gewählten Rolle, wie in UC-004 BR-001. Er liegt zwischen 0 und 5 und wird kaufmännisch auf eine Nachkommastelle gerundet. Die Skala der Radar-Charts zeigt nur die Ganzzahlen an.

### BR-002: Alle MAGs, nur Radar-Charts

Die Schulungsverantwortliche sieht die Radar-Charts aller MAGs, nicht nur die einer zugeordneten MAG. Das System zeigt ihr nur Radar-Charts, keine Einladungen.

### BR-003: Auch kleine MAGs

Das System zeigt auch die Radar-Charts kleiner MAGs und verlangt keine Mindestzahl an Scores, wie in UC-004 BR-003.

### BR-004: Nur Rollen mit Scores

Zur Auswahl stehen nur Rollen, zu denen mindestens ein Score vorliegt. Das System zeigt nur MAGs, zu denen Scores der gewählten Rolle vorliegen.

### BR-005: Nur Mittelwerte

Das System zeigt je Handlungsfeld und MAG nur den Mittelwert nach BR-001. Es zeigt keinen einzelnen Score, keine Zahl von Teilnehmern und keinen Verweis auf eine Einladung, ein Assessment oder eine Person.

### BR-006: Handlungsfeld ohne Score

Hat eine MAG in einem Handlungsfeld keinen Score, zeigt ihr Radar-Chart für dieses Handlungsfeld keinen Wert, wie in UC-004 BR-006.

### BR-007: Nur für angemeldete Schulungsverantwortliche

Nur die angemeldete Schulungsverantwortliche wertet die Radar-Charts aller MAGs aus. Aufrufe ohne Anmeldung und passende Berechtigung werden abgewiesen. Die Navigation zeigt Einträge, für die die Schulungsverantwortliche keine passende Berechtigung hat, deaktiviert.

### BR-008: Keine Leistungsbeurteilung

Die Radar-Charts sind kein Bestandteil der Leistungsbeurteilung.
