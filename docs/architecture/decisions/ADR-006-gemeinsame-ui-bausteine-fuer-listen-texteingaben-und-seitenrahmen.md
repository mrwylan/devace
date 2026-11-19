# ADR-006: Gemeinsame UI-Bausteine für Listen, Texteingaben und Seitenrahmen

**Status:** Accepted  
**Date:** 2026-11-19  
**Requirements:** NFR-010

## Context

Alle Oberflächen sind in deutscher Sprache und erfüllen WCAG 2.1 AA (NFR-010). Dazu gehört, dass Eingaben ihre Grenzen
vorab nennen (WCAG 3.3.2) und Inhalte ohne Abschneiden darstellbar sind (WCAG 1.4.10).

Ein UI-Review am 2026-11-19 hat die Umsetzung mit den Figma-Mockups verglichen (Figma-Seite „UI-Review 2026-11-19“).
Befunde:

- Listen sind weder sortierbar noch filterbar und zeigen alle Zeilen auf einmal (zehn Grids in sechs Ansichten). Die
  Aktion zum Anlegen steht unter Listen mit 15 bis 20 Zeilen.
- Längenbegrenzte Texteingaben (50 bis 2000 Zeichen nach UC-002, UC-006, UC-008, UC-009, UC-010) nennen die Grenze nur
  im Hilfetext. Sie zeigen keinen Zähler, der Hinweis erscheint erst nach dem Speichern.
- Im Assessment (UC-002) liegen die aktuelle Frage und das Antwortfeld nach wenigen Antworten unterhalb des sichtbaren
  Bereichs. Der Senden-Knopf hat keinen Abstand zum Antwortfeld.
- Das Radar-Chart (UC-004) ist abgeschnitten. Eine Fußnote (UC-008) ist abgeschnitten.
- Karte, Hinweis und Seitentitel sind zwölfmal in sechs Ansichten kopiert.
- Der Seitenrahmen weicht vom Mockup ab: Navigation, Profilbereich, Fußzeile und Farben.

Die Spezifikationen verlangen bei zu langen Angaben eigene Hinweise, zum Beispiel UC-002 A4 „Die Antwort ist zu lang:
<Zahl der Zeichen> von höchstens 500 Zeichen.“ Eine Mindestlänge für Antworten ist nicht spezifiziert.

Überschneidung: ADR-005 regelt den Wartehinweis bei KI-Anfragen. Er bleibt unverändert und wird in die neue Anordnung
des Assessments übernommen.

## Decision

- Die Ansichten nutzen gemeinsame Bausteine im Paket `info.wylan.devace.ui.bausteine`, statt sie je Ansicht nachzubauen:
  - **`DevaceListe`**: Die Liste füllt ihre Karte und scrollt in sich selbst, statt alle Zeilen anzuzeigen. Darüber
    stehen ein Suchfeld, das über alle Textspalten filtert, und die Anzeige „x von y“. Die Spaltenköpfe sind sortierbar.
    Zeilenaktionen sind Symbolknöpfe mit Tooltip und zugänglichem Namen („Ändern“, „Entfernen“ …).
  - **`Zeichenfeld`**: Das Textfeld zeigt beim Tippen einen Zähler „<Zahl> / <Grenze> Zeichen“. Die Grenze ist weich:
    Man kann über sie hinaus tippen oder einfügen, dann ist das Feld ungültig und zeigt den Hinweis der Spezifikation.
    Die auslösende Aktion (Speichern, Senden) ist gesperrt. Die Felder setzen kein `maxLength`, damit die
    Alternativabläufe „zu lang“ erreichbar bleiben.
  - **`Seitenkopf`, `Karte`, `Hinweis`, `Aktionsleiste`**: Die Hauptaktion einer Karte steht in ihrem Kopf rechts oben.
    Knopfleisten haben einen festen Abstand zum Inhalt und bleiben in langen Formularen und Dialogen sichtbar.
- Der Seitenrahmen folgt dem Mockup:
  - Navigationsgruppen „Auswertung“, „Einladungen“ und „Administration“ mit den bisherigen Bezeichnungen. Einträge ohne
    Berechtigung bleiben deaktiviert sichtbar.
  - Ein Profilbereich mit Benutzername, Berechtigung und „Abmelden“.
  - Eine Fußzeile mit den Platzhalterseiten „Datenschutz“ und „Hinweise zur KI-Nutzung“.
  - Die Farben des Mockups (Akzent Cyan, Hintergrund Navy) werden über die Tokens des Aura-Themas in `styles.css`
    gesetzt.
- Das Assessment zeigt den Fortschritt oben und klappt frühere Fragen und Antworten unter „Bisherige Antworten (n)“ ein.
  Die aktuelle Frage und das Antwortfeld sind immer sichtbar.

## Consequences

- Einfacher: Neue Ansichten verhalten sich gleich (Suche, Sortierung, Zähler, Platz der Aktionen), ohne eigenen Code
  dafür.
- Einfacher: Die Grenzen der Spezifikation sind vor dem Speichern sichtbar. Die Hinweise der Alternativabläufe bleiben
  unverändert.
- Schwerer: Alle Ansichten und ihre Browserless- und Browser-Tests werden angepasst. Zeilenaktionen findet ein Test über
  ihren zugänglichen Namen, nicht über einen sichtbaren Text.
- Schwerer: Die Platzhalterseiten der Fußzeile brauchen später fachlichen Inhalt und eine Spezifikation.
- Verworfen: harte Grenze mit `maxLength`. Sie schneidet eingefügten Text ohne Hinweis ab und macht die Alternativabläufe
  „zu lang“ unerreichbar.
- Verworfen: Filter je Spalte. Er ist genauer, aber in schmalen Karten wie „Rollen“ zu unruhig.
- Verworfen: Profilmenü mit „Profil“ und „Einstellungen“ wie im Mockup. Diese Funktionen sind nicht spezifiziert.
