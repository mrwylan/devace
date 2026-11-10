# Use Case: Einladung erstellen und versenden

## Übersicht

**Use-Case-ID:** UC-001  
**Use-Case-Name:** Einladung erstellen und versenden  
**Primärer Akteur:** Teamleitung  
**Sekundäre Akteure:** E-Mail-System  
**Ziel:** Die Teamleitung lädt Teilnehmer ihrer MAG zu einem Assessment ein, ohne dass die Einladung eine Person erkennen lässt.  
**Auslösendes Ereignis:** Die Teamleitung fordert Einladungen für Teilnehmer ihrer MAG an.  
**Status:** Genehmigt  

**Anforderungen:** [FR-001, FR-002, NFR-001, NFR-002, NFR-005, NFR-010](../requirements.md)  
**Schichten:** UI  
**Mockup:** [Formular](https://www.figma.com/design/SSeuF968QrgtXzD5Wvna6x/devace-%E2%80%93-Application?node-id=21-532), [Eingabe prüfen (A1)](https://www.figma.com/design/SSeuF968QrgtXzD5Wvna6x/devace-%E2%80%93-Application?node-id=21-820), [Ergebnis](https://www.figma.com/design/SSeuF968QrgtXzD5Wvna6x/devace-%E2%80%93-Application?node-id=21-1064), [Keine Rolle (A2)](https://www.figma.com/design/SSeuF968QrgtXzD5Wvna6x/devace-%E2%80%93-Application?node-id=21-1295), [Bestätigen](https://www.figma.com/design/SSeuF968QrgtXzD5Wvna6x/devace-%E2%80%93-Application?node-id=21-1437)

## Vorbedingungen

- Die Teamleitung ist mit Benutzername und Passwort angemeldet.
- Der Teamleitung ist mindestens eine MAG zugeordnet.
- Die Rollen sind angelegt.

## Hauptablauf

1. Die Teamleitung öffnet die Erstellung von Einladungen.
2. Das System zeigt die MAGs der Teamleitung und die Rollen mit freigegebenem Fragenkatalog zur Auswahl an.
3. Die Teamleitung wählt eine MAG und eine Rolle.
4. Die Teamleitung gibt die E-Mail-Adressen der Teilnehmer und die Gültigkeitsdauer der Einladungen an.
5. Die Teamleitung fordert die Erstellung an.
6. Das System prüft die Angaben und zeigt eine Zusammenfassung mit der Anzahl der Einladungen (doppelte Adressen zählen nur einmal), der gewählten MAG und Rolle sowie der Gültigkeitsdauer.
7. Die Teamleitung bestätigt die Zusammenfassung.
8. Das System erstellt je Adresse eine Einladung mit eindeutiger Referenz, der angegebenen Gültigkeitsdauer und dem daraus berechneten Ablaufdatum, die der gewählten MAG und Rolle zugeordnet ist.
9. Das System versendet jede Einladung per E-Mail-System an die zugehörige Adresse.
10. Das System zeigt der Teamleitung die Anzahl der versendeten Einladungen und nur die Adressen, bei denen der Versand scheiterte.

## Alternativabläufe

### A1: Angaben fehlen oder sind ungültig

**Auslöser:** Die Angabe enthält keine Adresse, mehr als 50 Adressen, mindestens eine ungültige Adresse oder eine ungültige Gültigkeitsdauer (Schritt 6)  
**Ablauf:**

1. Das System weist auf die fehlenden oder ungültigen Angaben hin: „Bitte mindestens eine Adresse eingeben.“, „Höchstens 50 Adressen je Durchgang.“, „Mindestens eine Adresse ist ungültig: <Adresse>. Bitte korrigieren.“ oder „Die Zahl muss zwischen 1 und 30 liegen.“
2. Die Teamleitung korrigiert die Angaben.
3. Der Use Case wird bei Schritt 4 fortgesetzt.

### A2: Keine Rolle mit freigegebenem Fragenkatalog

**Auslöser:** Es gibt keine Rolle mit freigegebenem Fragenkatalog (Schritt 2)  
**Ablauf:**

1. Das System weist die Teamleitung darauf hin, dass noch keine Einladung möglich ist: „Noch keine Einladung möglich“ mit „Es gibt noch keine Rolle mit freigegebenem Fragenkatalog. Bitte wenden Sie sich an einen Administrator.“
2. Der Use Case endet.

### A3: Teamleitung bricht ab

**Auslöser:** Die Teamleitung bricht die Erstellung ab, vor der Anforderung oder bei der Zusammenfassung (Schritt 5 oder 7)  
**Ablauf:**

1. Das System verwirft die Angaben.
2. Der Use Case endet.

### A4: Versand scheitert

**Auslöser:** Das E-Mail-System lehnt den Versand ab oder ist nicht erreichbar (Schritt 9)  
**Ablauf:**

1. Das System verwirft die Einladung dieser Adresse.
2. Das System versendet die Einladungen der übrigen Adressen weiter.
3. Der Use Case wird bei Schritt 10 fortgesetzt.

## Nachbedingungen

### Erfolgsfall

- Für jede Adresse, deren Versand gelang, existiert eine Einladung mit eindeutiger Referenz, der angegebenen Gültigkeitsdauer, dem Ablaufdatum, der gewählten MAG und Rolle sowie dem Status Versendet.
- Jeder Teilnehmer hat eine E-Mail mit seiner Einladung erhalten.
- Keine E-Mail-Adresse ist gespeichert.

### Fehlerfall

- Für eine Adresse, deren Versand scheiterte, bleibt keine Einladung gespeichert.
- Keine E-Mail-Adresse ist gespeichert.
- Es entsteht keine Einladung für eine MAG, die der Teamleitung nicht zugeordnet ist.

## Geschäftsregeln

### BR-001: Anonyme Referenz

Die Referenz einer Einladung ist eindeutig und wird zufällig vergeben. Sie enthält keine Zeichenfolge aus dem Namen oder der E-Mail-Adresse einer Person.

### BR-002: Nur eigene MAGs

Die Teamleitung erstellt Einladungen nur für MAGs, die ihr zugeordnet sind.

### BR-003: Rolle mit freigegebenem Fragenkatalog

Die Teamleitung kann nur Rollen wählen, für die ein freigegebener Fragenkatalog besteht.

### BR-004: E-Mail-Adresse nur zum Versand

Das System verwendet die E-Mail-Adresse nur für den Versand der Einladung. Es speichert sie nicht und gibt sie an keine andere Stelle weiter, auch nicht an ein KI-Modell.

### BR-005: Doppelte Adressen

Führt die Teamleitung dieselbe Adresse mehrfach auf, erhält sie eine einzige Einladung. Adressen, die sich nur in Groß- und Kleinschreibung oder in umgebenden Leerzeichen unterscheiden, gelten als dieselbe Adresse; es gilt die erste Schreibweise.

### BR-006: Gültige E-Mail-Adresse

Eine Adresse ist gültig, wenn sie aus einem lokalen Teil, dem Zeichen @ und einem Domänenteil mit mindestens einem Punkt besteht und keine Leerzeichen enthält. Eine leere Angabe enthält keine Adresse.

### BR-007: Inhalt der E-Mail

Die E-Mail enthält nur die Einladung und keinen weiteren Inhalt: den Betreff "Einladung zum devace-Assessment", einen kurzen Text und den Link zur Einladung. Der Text lautet: „Sie sind zu einem devace-Assessment eingeladen. Über den folgenden Link starten Sie es:“, danach folgt der Link.

### BR-008: Gültigkeitsdauer

Die Gültigkeitsdauer ist eine ganze Zahl von Tagen von 1 bis 30. Vorgeschlagen werden 30 Tage. Sie gilt für alle Einladungen eines Durchgangs und beginnt mit der Erstellung der Einladung. Die Einladung ist bis zum Ablauf des letzten Tages der Gültigkeitsdauer (24 Uhr) gültig. Das System berechnet das Ablaufdatum bei der Erstellung und speichert es mit der Einladung.

### BR-009: Nur für angemeldete Teamleitungen

Nur die angemeldete Teamleitung erstellt Einladungen. Aufrufe ohne Anmeldung und passende Berechtigung werden abgewiesen. Die Navigation zeigt Einträge, für die die Teamleitung keine passende Berechtigung hat, deaktiviert.

### BR-010: Höchstzahl der Adressen

Ein Durchgang enthält höchstens 50 Adressen. Doppelte Adressen zählen nach BR-005 einmal.
