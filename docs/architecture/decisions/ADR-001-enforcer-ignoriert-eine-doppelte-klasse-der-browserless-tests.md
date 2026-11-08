# ADR-001: Der Enforcer ignoriert eine doppelte Klasse der Browserless-Tests

**Status:** Accepted  
**Date:** 2026-11-08  
**Requirements:** NFR-005

## Context

Die Oberflächentests laufen mit Browserless Testing (`browserless-test-spring`, Test-Scope). Sie prüfen unter anderem
NFR-005: Aufrufe ohne Anmeldung und passende Berechtigung werden abgewiesen. Die Enforcer-Regel `banDuplicateClasses`
lässt den Build scheitern, weil `browserless-test-shared` 25.3.2, die neueste verfügbare Version, die Klasse
`com.vaadin.flow.component.geolocation.package-info` zusätzlich zu `flow-server` enthält. Projektregeln verbieten es,
Enforcer-Regeln ohne ADR zu lockern.

## Decision

`banDuplicateClasses` ignoriert genau die Klasse `com.vaadin.flow.component.geolocation.package-info`. Alle anderen
doppelten Klassen bleiben verboten. Die doppelten Klassen der Servlet-API (`jakarta.servlet-api` neben dem eingebetteten
Tomcat) werden nicht ignoriert, sondern durch eine Exclusion an `browserless-test-spring` beseitigt.

## Consequences

- Der Build bleibt mit den Browserless-Tests grün, die Regel gilt sonst unverändert.
- Die Ausnahme betrifft nur eine `package-info`-Klasse, die keinen Code enthält.
- Sie ist bei jedem Update von Vaadin oder Browserless zu prüfen und zu entfernen, sobald `browserless-test-shared` die Klasse nicht mehr enthält.
- Alternative: Karibu Testing statt Browserless. Verworfen, weil es eine weitere Test-Bibliothek einführt und vom Test-Skill des Projekts abweicht.
