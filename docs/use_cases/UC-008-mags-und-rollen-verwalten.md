# Use Case: MAGs und Rollen verwalten

## Übersicht

**Use-Case-ID:** UC-008  
**Use-Case-Name:** MAGs und Rollen verwalten  
**Primärer Akteur:** Administrator  
**Ziel:** Der Administrator hält MAGs und Rollen aktuell, damit Einladungen, Fragenkataloge und Radar-Charts der Organisation entsprechen.  
**Auslösendes Ereignis:** Der Administrator fordert die Verwaltung von MAGs und Rollen an.  
**Status:** Genehmigt  

**Anforderungen:** [FR-015, NFR-005, NFR-010](../requirements.md)  
**Schichten:** UI  
**Mockup:** [Übersicht](https://www.figma.com/design/SSeuF968QrgtXzD5Wvna6x/devace-%E2%80%93-Application?node-id=23-918), [MAG anlegen](https://www.figma.com/design/SSeuF968QrgtXzD5Wvna6x/devace-%E2%80%93-Application?node-id=23-1267), [Eingabe prüfen (A1)](https://www.figma.com/design/SSeuF968QrgtXzD5Wvna6x/devace-%E2%80%93-Application?node-id=23-1452), [MAG besteht bereits (A2)](https://www.figma.com/design/SSeuF968QrgtXzD5Wvna6x/devace-%E2%80%93-Application?node-id=23-1635), [Rolle ändern](https://www.figma.com/design/SSeuF968QrgtXzD5Wvna6x/devace-%E2%80%93-Application?node-id=23-1817)

## Vorbedingungen

- Der Administrator ist angemeldet.

## Hauptablauf

1. Der Administrator öffnet die Verwaltung von MAGs und Rollen.
2. Das System zeigt die vorhandenen MAGs mit Scrum Team, Linienteam und Teamleitung sowie die vorhandenen Rollen an. Es bietet die vorhandenen Teamleitungen zur Zuordnung an.
3. Der Administrator wählt, ob er eine MAG oder eine Rolle anlegt oder einen vorhandenen Eintrag ändert.
4. Der Administrator gibt die Angaben ein: bei einer MAG Scrum Team, Linienteam und optional eine Teamleitung, bei einer Rolle den Namen.
5. Der Administrator bestätigt die Angaben.
6. Das System prüft die Angaben.
7. Das System speichert die MAG oder die Rolle.
8. Das System zeigt die aktualisierte Übersicht an.

## Alternativabläufe

### A1: Angaben fehlen oder sind ungültig

**Auslöser:** Eine Pflichtangabe fehlt oder eine Angabe ist zu lang (Schritt 6)  
**Ablauf:**

1. Das System weist am jeweiligen Feld auf die fehlenden oder ungültigen Angaben hin: „Pflichtangabe fehlt.“ oder „Höchstens 100 Zeichen.“ (bei einer Rolle „Höchstens 50 Zeichen.“)
2. Der Administrator korrigiert die Angaben.
3. Der Use Case wird bei Schritt 4 fortgesetzt.

### A2: MAG oder Rolle besteht bereits

**Auslöser:** Die Kombination aus Scrum Team und Linienteam oder der Name der Rolle gehört bereits zu einem anderen Eintrag (Schritt 6)  
**Ablauf:**

1. Das System weist darauf hin, dass die MAG oder die Rolle bereits besteht: „Diese MAG besteht bereits.“ bzw. „Diese Rolle besteht bereits.“
2. Der Administrator korrigiert die Angaben.
3. Der Use Case wird bei Schritt 4 fortgesetzt.

### A3: Administrator bricht ab

**Auslöser:** Der Administrator bricht die Eingabe ab (Schritt 5)  
**Ablauf:**

1. Das System verwirft die Angaben.
2. Der Use Case endet.

## Nachbedingungen

### Erfolgsfall

- Die MAG oder die Rolle ist mit den bestätigten Angaben gespeichert, neu angelegt oder geändert.
- Einladungen, Fragenkataloge und Scores, die auf die MAG oder die Rolle verweisen, sind ihr weiterhin zugeordnet.

### Fehlerfall

- Es entsteht keine MAG und keine Rolle mit unvollständigen oder ungültigen Angaben.
- Es entstehen nicht zwei MAGs mit derselben Kombination aus Scrum Team und Linienteam und nicht zwei Rollen mit demselben Namen.
- Bestehende MAGs und Rollen bleiben unverändert.

## Geschäftsregeln

### BR-001: Pflichtangaben und Länge

Scrum Team und Linienteam einer MAG sind Pflichtangaben mit höchstens 100 Zeichen. Der Name einer Rolle ist eine Pflichtangabe mit höchstens 50 Zeichen. Eine Angabe, die nur aus Leerzeichen besteht, gilt als fehlend.

### BR-002: Eindeutige MAG

Die Kombination aus Scrum Team und Linienteam gehört zu höchstens einer MAG. Beim Vergleich bleiben Groß- und Kleinschreibung sowie umgebende Leerzeichen unberücksichtigt.

### BR-003: Eindeutige Rolle

Der Name einer Rolle gehört zu höchstens einer Rolle. Beim Vergleich bleiben Groß- und Kleinschreibung sowie umgebende Leerzeichen unberücksichtigt.

### BR-004: Teamleitung einer MAG

Einer MAG ist höchstens eine Teamleitung zugeordnet, ausgewählt aus den vorhandenen Teamleitungen. Eine MAG ohne Teamleitung ist zulässig; für sie kann niemand Einladungen erstellen. Eine nicht vorhandene Teamleitung wird mit „Diese Teamleitung ist nicht vorhanden.“ abgewiesen. Die Verwaltung der Benutzerkonten gehört nicht zu diesem Use Case.

### BR-005: Anlegen und Ändern ohne Löschen

Der Use Case legt MAGs und Rollen an und ändert sie. Er löscht sie nicht.

### BR-006: Änderung wirkt rückwirkend

Ändert der Administrator Scrum Team, Linienteam oder Teamleitung einer MAG oder den Namen einer Rolle, bleiben bestehende Einladungen, Fragenkataloge und Scores der MAG bzw. Rolle zugeordnet und erscheinen mit den neuen Angaben.

### BR-007: Nur für Administratoren

Nur der angemeldete Administrator verwaltet MAGs und Rollen. Aufrufe ohne Anmeldung und passende Berechtigung werden abgewiesen. Die Navigation zeigt Einträge, für die der Administrator keine passende Berechtigung hat, deaktiviert.
