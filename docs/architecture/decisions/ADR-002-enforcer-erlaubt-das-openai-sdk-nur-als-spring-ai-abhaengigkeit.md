# ADR-002: Der Enforcer erlaubt das OpenAI-SDK nur als Abhängigkeit von Spring AI

**Status:** Accepted  
**Date:** 2026-11-08
**Requirements:** NFR-001

## Context

Die KI-Anfragen (UC-006) laufen über Spring AI und OpenRouter als einziges Gateway. OpenRouter ist OpenAI-kompatibel;
in Spring AI 2.0.2 gibt es dafür nur das Modul `spring-ai-openai`, das auf dem OpenAI-Java-SDK
(`com.openai:openai-java-core`) beruht. Die Enforcer-Regel `bannedDependencies` verbietet direkte Modell-Provider-SDKs
(`com.openai:*`). NFR-001 verlangt, dass keine KI-Anfrage Angaben zu einer Person enthält; dafür muss der Weg zum
Modell an einer Stelle liegen, die Anfrage selbst baut devace. Projektregeln verbieten es, Enforcer-Regeln ohne ADR zu
lockern.

## Decision

`bannedDependencies` lässt genau `com.openai:openai-java-core` (Scope compile) zu, und nur weil es Spring AI
transitiv mitbringt. Der Code von devace ruft das SDK nicht direkt auf; Zugriff auf das Modell hat nur Spring AI
(`ChatModel`/`ChatClient`) im Paket `info.wylan.devace.client`. Alle anderen `com.openai`-Artefakte und alle anderen
Provider-SDKs bleiben verboten. Zusätzlich wird `io.swagger.core.v3:swagger-annotations` (reine Annotationen, doppelt
zu `swagger-annotations-jakarta`) von `spring-ai-openai` ausgeschlossen, damit `banDuplicateClasses` ohne Ausnahme
bestanden wird.

## Consequences

- Der Build bleibt grün, die Regel gilt sonst unverändert.
- Ein Review-Punkt bleibt: kein Import aus `com.openai` im Code von devace.
- Die Ausnahme ist beim Wechsel der Spring-AI-Version zu prüfen und zu entfernen, wenn Spring AI das SDK nicht mehr braucht.
- Alternative: Spring AI 1.1.x ohne SDK. Verworfen, weil es auf Spring Boot 3.5 ausgelegt ist und Spring Boot 4.1.2 fest vorgegeben ist.
