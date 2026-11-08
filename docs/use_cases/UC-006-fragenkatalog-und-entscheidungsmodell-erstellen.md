# Use Case: Fragenkatalog und Entscheidungsmodell erstellen

## Übersicht

**Use-Case-ID:** UC-006  
**Use-Case-Name:** Fragenkatalog und Entscheidungsmodell erstellen  
**Primärer Akteur:** Administrator  
**Sekundäre Akteure:** KI-Modell  
**Ziel:** Der Administrator erstellt für eine Rolle einen Entwurf des Fragenkatalogs mit dem Entscheidungsmodell jeder Frage, damit die Handlungsfelder gezielt und fair geprüft werden können.  
**Auslösendes Ereignis:** Der Administrator fordert die Erstellung eines Fragenkatalogs für eine Rolle an.  
**Status:** Entwurf  

**Anforderungen:** [FR-012, FR-013, NFR-005, NFR-010, C-006](../requirements.md)  
**Schichten:** UI

## Vorbedingungen

- Der Administrator ist angemeldet.
- Die Rollen sind angelegt.
- Die sechs Handlungsfelder sind als feste Stammdaten vorgegeben.

## Hauptablauf

1. Der Administrator öffnet die Erstellung von Fragenkatalogen.
2. Das System zeigt die Rollen mit ihren Fragenkatalogen, deren Version und Status an.
3. Der Administrator wählt eine Rolle und legt fest, womit er beginnt: mit einem leeren Entwurf, mit der freigegebenen Version der Rolle als neuer Version oder mit dem vorhandenen Entwurf der Rolle.
4. Das System zeigt den gewählten Ausgangspunkt je Handlungsfeld an.
5. Der Administrator gibt optional Schwerpunkte und die Zahl der Fragen je Handlungsfeld an und fordert einen KI-Entwurf an.
6. Das System lässt das KI-Modell zusätzliche Fragen mit je einem Entscheidungsmodell für die Handlungsfelder der Rolle entwerfen und ergänzt den Entwurf um sie.
7. Das System zeigt den Entwurf je Handlungsfeld an.
8. Der Administrator prüft den Entwurf und ändert, ergänzt oder entfernt Fragen und die Entscheidungsmodelle.
9. Der Administrator speichert den Entwurf.
10. Das System prüft den Entwurf.
11. Das System speichert den Entwurf als Fragenkatalog der Rolle mit dem Status Entwurf.
12. Das System bestätigt das Speichern.

## Alternativabläufe

### A1: Ohne KI-Entwurf

**Auslöser:** Der Administrator verzichtet auf den KI-Entwurf und erfasst Fragen und Entscheidungsmodelle selbst (Schritt 5)  
**Ablauf:**

1. Der Administrator erfasst Fragen und Entscheidungsmodelle selbst.
2. Der Use Case wird bei Schritt 8 fortgesetzt.

### A2: KI-Modell liefert keinen Entwurf

**Auslöser:** Das KI-Modell ist nicht erreichbar oder liefert keine Frage (Schritt 6)  
**Ablauf:**

1. Das System weist darauf hin, dass kein Entwurf entstanden ist.
2. Das System behält den Ausgangspunkt und die bisherigen Angaben.
3. Der Use Case wird bei Schritt 5 fortgesetzt.

### A3: Angaben für den KI-Entwurf ungültig

**Auslöser:** Die Zahl der Fragen je Handlungsfeld liegt außerhalb des zulässigen Bereichs oder die Schwerpunkte sind zu lang (Schritt 5)  
**Ablauf:**

1. Das System weist auf die ungültige Angabe hin.
2. Der Administrator korrigiert die Angabe.
3. Der Use Case wird bei Schritt 5 fortgesetzt.

### A4: Entwurf unvollständig oder ungültig

**Auslöser:** Eine Frage hat keinen Wortlaut, ein Entscheidungsmodell hat nicht für jede Stufe von 0 bis 5 eine Vorgabe, oder eine Angabe ist zu lang (Schritt 10)  
**Ablauf:**

1. Das System weist auf die betroffenen Fragen und Angaben hin.
2. Der Administrator korrigiert den Entwurf.
3. Der Use Case wird bei Schritt 8 fortgesetzt.

### A5: Administrator bricht ab

**Auslöser:** Der Administrator bricht die Bearbeitung ab (Schritt 9)  
**Ablauf:**

1. Das System verwirft die Änderungen dieser Sitzung.
2. Der Use Case endet.

## Nachbedingungen

### Erfolgsfall

- Für die Rolle besteht ein Fragenkatalog mit dem Status Entwurf, dessen Fragen je einem Handlungsfeld zugeordnet sind.
- Jede Frage des Entwurfs hat ein Entscheidungsmodell mit genau einer Vorgabe je Stufe von 0 bis 5.
- Die freigegebene Version der Rolle ist unverändert.

### Fehlerfall

- Es wird kein Entwurf mit einer Frage gespeichert, deren Entscheidungsmodell nicht für jede Stufe von 0 bis 5 eine Vorgabe enthält.
- Der freigegebene Fragenkatalog und archivierte Fragenkataloge bleiben unverändert.
- Ein vorhandener gespeicherter Entwurf bleibt unverändert, solange der Administrator nicht erfolgreich speichert.

## Geschäftsregeln

### GR-001: Entscheidungsmodell je Frage

Jede Frage hat genau eine Vorgabe je Stufe von 0 bis 5. Jede Vorgabe beschreibt, welche Antwort der Stufe entspricht, ist eine Pflichtangabe und hat höchstens 500 Zeichen.

### GR-002: Frage

Eine Frage gehört zu genau einem Handlungsfeld. Der Wortlaut ist eine Pflichtangabe mit höchstens 500 Zeichen. Die Position einer Frage im Fragenkatalog ist eindeutig.

### GR-003: Angaben für den KI-Entwurf

Die Zahl der Fragen je Handlungsfeld ist eine ganze Zahl von 1 bis 10. Vorgeschlagen werden 3. Die Schwerpunkte sind optional und haben höchstens 2000 Zeichen.

### GR-004: Versionen und Entwurf

Ein neuer Fragenkatalog einer Rolle erhält die um 1 höhere Nummer als die höchste Version dieser Rolle. Eine Rolle hat höchstens einen Entwurf; besteht ein Entwurf, setzt der Administrator ihn fort, statt einen weiteren zu beginnen. Die freigegebene Version als Ausgangspunkt steht nur zur Verfügung, wenn die Rolle eine freigegebene Version hat.

### GR-005: Nur der Entwurf ändert sich

Der Use Case ändert nur den Entwurf. Freigegebene und archivierte Fragenkataloge bleiben unverändert.

### GR-006: KI-Entwurf ist ein Vorschlag

Der Entwurf des KI-Modells ist ein Vorschlag. Er gilt erst nach der Prüfung und Freigabe durch den Administrator als Fragenkatalog in Assessments. Ein Entwurf darf Handlungsfelder ohne Frage enthalten; die Vollständigkeit für die Freigabe prüft UC-007.

### GR-007: Anfrage an das KI-Modell ohne Personenbezug

Die Anfrage an das KI-Modell enthält nur die Rolle, die Handlungsfelder, die angegebenen Schwerpunkte und die Zahl der Fragen. Sie enthält keine Angabe zu einer Person.

### GR-008: Alltagsrelevanz

Der Fragenkatalog nimmt Technologien und Grundlagen nach Relevanz im Alltag der Teams auf, nicht nach Eintrag in der Kompetenzmatrix.

### GR-009: Nur für Administratoren

Nur der angemeldete Administrator erstellt Fragenkataloge und Entscheidungsmodelle. Aufrufe ohne Anmeldung und passende Berechtigung werden abgewiesen.

### GR-010: KI-Entwurf ergänzt den Entwurf

Der KI-Entwurf ändert und entfernt keine vorhandene Frage. Er ergänzt je Handlungsfeld so viele Fragen, bis die angegebene Zahl erreicht ist. Hat ein Handlungsfeld bereits mindestens so viele Fragen, ergänzt er dort keine.

### GR-011: Unvollständige Frage aus dem KI-Entwurf

Eine Frage des KI-Entwurfs, deren Entscheidungsmodell nicht für jede Stufe von 0 bis 5 eine Vorgabe enthält, erscheint im Entwurf und muss vor dem Speichern ergänzt werden. Als nicht verwertbar gilt nur ein KI-Entwurf ohne Frage.
