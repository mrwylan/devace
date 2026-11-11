# Use Case: KI-Bewertung mit Testfällen absichern

## Übersicht

**Use-Case-ID:** UC-009  
**Use-Case-Name:** KI-Bewertung mit Testfällen absichern  
**Primärer Akteur:** Administrator  
**Sekundäre Akteure:** KI-Modell  
**Ziel:** Der Administrator führt Testfälle aus und sieht deren Ergebnisse, damit er Fehler der KI-Bewertung eingrenzt und die Vorgaben des Entscheidungsmodells verfeinert.  
**Auslösendes Ereignis:** Der Administrator fordert die Absicherung der KI-Bewertung an.  
**Status:** Entwurf  

**Anforderungen:** [FR-016, NFR-001, NFR-005, NFR-009, NFR-010, C-005](../requirements.md)  
**Schichten:** UI  

## Vorbedingungen

- Der Administrator ist mit Benutzername und Passwort angemeldet.

## Hauptablauf

1. Der Administrator öffnet die Absicherung der KI-Bewertung.
2. Das System zeigt die prüfbaren Fragenkataloge der Rollen mit der Zahl ihrer Testfälle und dem Ergebnis des jüngsten Testlaufs.
3. Der Administrator wählt einen Fragenkatalog.
4. Das System zeigt die Testfälle je Frage und die bisherigen Testläufe mit Zeitpunkt und Trefferquote.
5. Der Administrator legt bei Bedarf Testfälle an, ändert oder entfernt sie, mit Frage, vorgegebener Antwort und erwarteter Stufe.
6. Der Administrator fordert die Ausführung der Testfälle an.
7. Das System legt jeden Testfall dem KI-Modell vor, das die Antwort nach dem Entscheidungsmodell der Frage einer Stufe zuordnet.
8. Das System speichert den Testlauf mit dem verwendeten KI-Modell, der ermittelten Stufe je Testfall und der Trefferquote.
9. Das System zeigt dem Administrator die Trefferquote und die Abweichungen, also die Testfälle, deren ermittelte Stufe von der erwarteten abweicht.

## Alternativabläufe

### A1: Kein Fragenkatalog prüfbar

**Auslöser:** Es gibt keinen prüfbaren Fragenkatalog (Schritt 2)  
**Ablauf:**

1. Das System weist den Administrator darauf hin, dass noch kein Fragenkatalog zur Prüfung vorliegt.
2. Der Use Case endet.

### A2: Angaben zum Testfall ungültig

**Auslöser:** Die Angaben zu einem Testfall sind unvollständig oder ungültig (Schritt 5)  
**Ablauf:**

1. Das System weist auf die fehlenden oder ungültigen Angaben hin.
2. Das System speichert den Testfall nicht.
3. Der Use Case wird bei Schritt 5 fortgesetzt.

### A3: Fragenkatalog ohne Testfall

**Auslöser:** Der gewählte Fragenkatalog hat keinen Testfall (Schritt 6)  
**Ablauf:**

1. Das System weist den Administrator darauf hin, dass es nichts auszuführen gibt.
2. Der Use Case wird bei Schritt 5 fortgesetzt.

### A4: KI-Modell liefert keine Stufe

**Auslöser:** Das KI-Modell ist nicht erreichbar oder liefert zu mindestens einem Testfall keine Stufe (Schritt 7)  
**Ablauf:**

1. Das System weist den Administrator darauf hin, dass der Testlauf nicht abgeschlossen werden konnte.
2. Das System speichert keinen Testlauf.
3. Der Use Case wird bei Schritt 6 fortgesetzt.

## Nachbedingungen

### Erfolgsfall

- Die Testfälle des Fragenkatalogs sind so gespeichert, wie der Administrator sie angegeben hat.
- Der Testlauf ist mit dem verwendeten KI-Modell, dem Zeitpunkt, dem Administrator, der ermittelten Stufe je Testfall und der Trefferquote gespeichert.
- Der Administrator kennt die Abweichungen.

### Fehlerfall

- Es ist kein unvollständiger Testlauf gespeichert.
- Fragen und Entscheidungsmodell des Fragenkatalogs sind unverändert.
- Das KI-Modell erhält keine Angabe, die auf eine Person schließen lässt.

## Geschäftsregeln

### BR-001: Testfall

Ein Testfall gehört zu genau einer Frage des Fragenkatalogs. Die vorgegebene Antwort ist eine Pflichtangabe mit höchstens 500 Zeichen. Die erwartete Stufe ist eine ganze Zahl von 0 bis 5.

### BR-002: Prüfbare Fragenkataloge

Prüfbar sind der Entwurf und die freigegebene Version einer Rolle. Archivierte Fragenkataloge sind nicht prüfbar.

### BR-003: Alle Testfälle eines Fragenkatalogs

Ein Testlauf führt alle Testfälle des gewählten Fragenkatalogs aus.

### BR-004: Bewertung wie im Assessment

Das KI-Modell ordnet die vorgegebene Antwort eines Testfalls nach dem Entscheidungsmodell der Frage einer Stufe zu, so wie bei der Antwort eines Teilnehmers (UC-002 BR-006).

### BR-005: Trefferquote

Die Trefferquote ist der Anteil der Testfälle eines Testlaufs, deren ermittelte Stufe der erwarteten entspricht, in Prozent mit höchstens zwei Nachkommastellen.

### BR-006: Zielwert der Trefferquote

Der Zielwert der Trefferquote ist 90 Prozent. Das System kennzeichnet einen Testlauf mit einer Trefferquote unter 90 Prozent als nicht bestanden und führt die Abweichungen auf.

### BR-007: Dokumentation der Testläufe

Das System hält jeden Testlauf mit seinen Abweichungen fest und löscht ihn nicht. Der Administrator sieht frühere Testläufe eines Fragenkatalogs.

### BR-008: Anfrage ohne Personenbezug

Die Anfrage an das KI-Modell enthält nur die Frage, das Entscheidungsmodell und die vorgegebene Antwort des Testfalls. Sie enthält keine Angabe zu einer Person.

### BR-009: Fragenkatalog bleibt unverändert

Die Ausführung der Testfälle ändert weder Fragen noch Entscheidungsmodell. Die Verfeinerung der Vorgaben erfolgt im Entwurf nach UC-006.

### BR-010: Testfälle folgen ihrer Frage

Beginnt der Administrator nach UC-006 eine neue Version aus der freigegebenen Version einer Rolle, übernimmt das System deren Testfälle zu den übernommenen Fragen in die neue Version. Entfernt der Administrator in UC-006 eine Frage aus dem Entwurf, entfernt das System ihre Testfälle mit.

### BR-011: Nur für Administratoren

Nur der angemeldete Administrator pflegt und führt Testfälle aus. Aufrufe ohne Anmeldung und passende Berechtigung werden abgewiesen. Die Navigation zeigt Einträge, für die der Administrator keine passende Berechtigung hat, deaktiviert.
