# Use Case: Fragenkatalog freigeben

## Übersicht

**Use-Case-ID:** UC-007  
**Use-Case-Name:** Fragenkatalog freigeben  
**Primärer Akteur:** Administrator  
**Ziel:** Der Administrator prüft den Entwurf eines Fragenkatalogs samt Entscheidungsmodell und gibt ihn frei, damit nur geprüfte Fragen in Assessments verwendet werden.  
**Auslösendes Ereignis:** Der Administrator fordert die Freigabe eines Fragenkatalogs an.  
**Status:** Getestet  

**Anforderungen:** [FR-014, NFR-005, NFR-010, C-006](../requirements.md)  
**Schichten:** UI  

## Vorbedingungen

- Der Administrator ist mit Benutzername und Passwort angemeldet.
- Die Rollen sind angelegt.

## Hauptablauf

1. Der Administrator öffnet die Freigabe von Fragenkatalogen.
2. Das System zeigt die Rollen mit einem Entwurf zur Auswahl an.
3. Der Administrator wählt eine Rolle.
4. Das System zeigt den Entwurf der Rolle je Handlungsfeld mit den Fragen und dem Entscheidungsmodell jeder Frage zur Prüfung.
5. Der Administrator fordert die Freigabe des Entwurfs an.
6. Das System prüft die Vollständigkeit des Entwurfs und zeigt eine Zusammenfassung mit der Rolle, der Version und der Zahl der Fragen je Handlungsfeld.
7. Der Administrator bestätigt die Freigabe.
8. Das System gibt den Entwurf frei und archiviert die bisher freigegebene Version der Rolle.
9. Das System bestätigt dem Administrator die Freigabe mit „Fragenkatalog freigegeben.“

## Alternativabläufe

### A1: Kein Entwurf vorhanden

**Auslöser:** Es gibt keine Rolle mit einem Entwurf (Schritt 2), oder der Entwurf der gewählten Rolle liegt bei der Freigabe nicht mehr in der zusammengefassten Version vor (Schritt 8)  
**Ablauf:**

1. Das System weist den Administrator mit „Es liegt kein Entwurf zur Freigabe vor.“ darauf hin, dass kein Entwurf zur Freigabe vorliegt.
2. Das System gibt keinen Entwurf frei.
3. Der Use Case endet.

### A2: Entwurf unvollständig

**Auslöser:** Der Entwurf ist nicht vollständig (Schritt 6)  
**Ablauf:**

1. Das System weist den Administrator mit „Der Entwurf ist unvollständig und wird nicht freigegeben.“ auf die Handlungsfelder ohne Frage („Handlungsfelder ohne Frage: …“ mit ihren Namen) und auf die Fragen mit unvollständigem Entscheidungsmodell („Fragen mit unvollständigem Entscheidungsmodell: Frage …“ mit ihren Nummern) hin.
2. Das System gibt den Entwurf nicht frei.
3. Der Use Case endet.

### A3: Administrator bricht ab

**Auslöser:** Der Administrator bricht die Freigabe ab, vor der Anforderung oder bei der Zusammenfassung (Schritt 5 oder 7)  
**Ablauf:**

1. Das System gibt den Entwurf nicht frei.
2. Der Use Case endet.

## Nachbedingungen

### Erfolgsfall

- Der Fragenkatalog hat den Status Freigegeben, mit Zeitpunkt der Freigabe und dem Administrator, der freigegeben hat.
- Die bisher freigegebene Version der Rolle hat den Status Archiviert.
- Die Rolle hat genau eine freigegebene Version.
- Fragen und Entscheidungsmodell sind unverändert.

### Fehlerfall

- Kein Fragenkatalog ist geändert.
- Eine bereits freigegebene Version der Rolle bleibt freigegeben.
- Die Rolle hat höchstens eine freigegebene Version.

## Geschäftsregeln

### BR-001: Vollständigkeit für die Freigabe

Ein Entwurf ist vollständig, wenn jedes der sechs Handlungsfelder mindestens eine Frage hat und jede Frage eine Vorgabe für jede Stufe von 0 bis 5 hat. Ein unvollständiger Entwurf wird nicht freigegeben.

### BR-002: Eine freigegebene Version je Rolle

Je Rolle ist höchstens eine Version freigegeben. Mit der Freigabe wird die bisher freigegebene Version der Rolle archiviert.

### BR-003: Nur ein Entwurf wird freigegeben

Das System gibt nur den Entwurf einer Rolle frei. Freigegebene und archivierte Fragenkataloge bleiben unverändert.

### BR-004: Freigabe ändert den Inhalt nicht

Die Freigabe ändert weder Fragen noch Entscheidungsmodell. Eine Änderung des Inhalts erfolgt im Entwurf nach UC-006.

### BR-005: Freigabe ist nachvollziehbar

Das System hält den Zeitpunkt der Freigabe und den Administrator fest, der freigegeben hat.

### BR-006: Laufende Assessments

Ein laufendes Assessment behält den Fragenkatalog, mit dem es begonnen wurde, auch wenn diese Version archiviert wird.

### BR-007: Nur für Administratoren

Nur der angemeldete Administrator gibt Fragenkataloge frei. Aufrufe ohne Anmeldung und passende Berechtigung werden abgewiesen. Die Navigation zeigt Einträge, für die der Administrator keine passende Berechtigung hat, deaktiviert.
