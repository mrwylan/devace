# ADR-005: KI-Anfragen laufen im Hintergrund, die Ansicht zeigt einen Wartehinweis und erhält das Ergebnis per Server-Push

**Status:** Accepted  
**Date:** 2026-11-15  
**Requirements:** NFR-011, NFR-012, NFR-006, NFR-008, FR-006, FR-007, FR-012

## Context

Drei Ansichten warten auf eine Antwort des KI-Modells: die Stufenzuordnung einer Antwort (UC-002 Schritt 6, höchstens
10 Sekunden nach NFR-006 und ADR-004), der Textreport zum Abschluss (UC-003 Schritt 3, höchstens 45 Sekunden nach
NFR-011) und der KI-Entwurf eines Fragenkatalogs (UC-006 Schritt 6 und A6). Ein Textreport braucht mit dem heutigen
Modell rund 17 Sekunden. Solange das System wartet, zeigt es innerhalb von 1 Sekunde einen Wartehinweis, der nennt,
worauf gewartet wird (NFR-012). Das System unterstützt 50 gleichzeitige Assessments (NFR-008).

Bisher ruft jede Ansicht das KI-Modell synchron in der Anfrage des Browsers auf. Dabei ist die Vaadin-Sitzung bis zur
Antwort gesperrt, und der Browser zeigt nur den schmalen Ladebalken von Vaadin. Ein eigener Wartehinweis erscheint so
erst, wenn die Antwort schon da ist. Die Anwendung nutzt bisher keinen Server-Push.

Die Zeitlimits je Aufruf legt der Client-Layer fest (ADR-004 für Jev, NFR-011 für den Textreport). Diese Entscheidung
ändert sie nicht, sie regelt nur, wie die Ansicht auf das Ergebnis wartet.

## Decision

- Die Anwendung aktiviert Server-Push von Vaadin mit `@Push` auf der `AppShell`.
- Eine Ansicht, die das KI-Modell aufruft (UC-002 Schritt 6, UC-003 Schritt 3, UC-006 Schritt 6 und A6), zeigt in der
  Anfrage des Browsers sofort den Wartehinweis aus der Spezifikation und sperrt die auslösende Aktion.
- Der Aufruf des Service läuft danach in einem Hintergrund-Thread aus einem von Spring verwalteten `TaskExecutor`,
  nicht im Thread der Anfrage und nicht in einem selbst erzeugten Thread.
- Das Ergebnis, eine fachliche Ablehnung (zum Beispiel UC-003 A1) und jeder andere Fehler kommen mit
  `UI.accessLater(...)` zurück in die Ansicht. Erst dort ersetzt die Ansicht den Wartehinweis durch das Ergebnis oder
  den Hinweis des Alternativablaufs.
- Ist die Ansicht beim Ende des Aufrufs nicht mehr geöffnet, verwirft sie das Ergebnis. Was der Service gespeichert
  hat, bleibt gespeichert, so wie bei einem synchronen Aufruf.
- Service, Transaktionen und Geschäftsregeln bleiben unverändert. Sie kennen weder Push noch Hintergrund-Thread.

## Consequences

- Einfacher: Der Wartehinweis erscheint sofort (NFR-012). Die Sitzung bleibt während des Wartens bedienbar, und eine
  lange Anfrage des Browsers kann nicht in das Zeitlimit eines Proxys oder Ingress laufen (NFR-011).
- Einfacher: Wartende Teilnehmer belegen keinen Thread des Webservers. Die Zahl gleichzeitiger KI-Aufrufe begrenzt der
  `TaskExecutor` an einer Stelle (NFR-008).
- Schwerer: Push braucht eine dauerhafte Verbindung zum Browser, im Betrieb über WebSocket mit Long Polling als
  Ausweichweg. Ein Proxy vor der Anwendung muss das durchlassen.
- Schwerer: Die Tests der Ansichten warten auf das Ergebnis aus dem Hintergrund, statt es direkt nach dem Klick zu
  prüfen. Browserless- und Browser-Tests brauchen dafür ein Warten auf den neuen Zustand der Ansicht.
- Schwerer: Der Hinweis zu UC-003 A3 in `AssessmentView`, der bewusst ohne Push auskommt, ist nicht mehr nötig. Er darf
  bleiben, solange er korrekt ist.
- Verworfen: zwei Anfragen ohne Push. Die erste zeigt den Wartehinweis, die zweite ruft das KI-Modell synchron auf. Das
  ist weniger Code, sperrt aber die Sitzung bis zu 45 Sekunden und riskiert Zeitlimits von Proxys im Betrieb.
- Verworfen: nur den globalen Ladebalken von Vaadin anpassen. Er kann nicht nennen, worauf gewartet wird (NFR-012).
