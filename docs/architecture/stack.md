# Stack

**For the agent:** read before writing code. This file is the AI Unified Process record of the technical layers,
independent of how the team documents its architecture. The Layers table says which layer skill builds and tests
each technical layer: `/use-case-implement` runs the Skill column bottom-up, `/use-case-test` runs the Test skill
column from the integration layer up, and `/use-case-coverage` searches for the markers in the last table.

**Last updated:** 2026-11-08

Die Technologien folgen `CLAUDE.md` §1 (verbindlicher Stack). Die Spalten Skill und Test skill bleiben `–`, bis die Layer-Plugins installiert sind (`aiup-flyway`, `aiup-jooq`, `aiup-spring-boot`, `aiup-vaadin`, `aiup-playwright`, siehe `CLAUDE.md` §2.1).

## Layers

| Layer       | Technology                                   | Skill | Test skill | Location                                  |
|-------------|----------------------------------------------|-------|------------|-------------------------------------------|
| Migration   | Flyway, PostgreSQL                           | –     | –          | `src/main/resources/db/migration`         |
| Persistence | jOOQ (generated from the migrated schema)    | –     | –          | `info.wylan.devace.persistence`                  |
| Client      | Spring AI via OpenRouter; e-mail dispatch    | –     | –          | `info.wylan.devace.client`                       |
| Service     | Spring Boot services                         | –     | –          | `info.wylan.devace.service`                      |
| API         | –                                            | –     | –          | –                                         |
| UI          | Vaadin 25 Flow                               | –     | –          | `info.wylan.devace.ui`                           |
| E2E         | Playwright                                   | –     | –          | `src/test/java/info/wylan/devace/e2e`            |

**Integration layer:**

| Use case offered through | Tested in | Test skill |
|--------------------------|-----------|------------|
| UI                       | UI        | –          |
| Service                  | Service   | –          |

A layer the project does not have is marked `–` in every column (a UI layer in a REST service). The Integration
layer table keeps only the rows of the entry layers the project has — a REST service drops the UI row — and its
Test skill column names what the Layers table names for that layer (`–` while the skill is not installed). A use
case is tested in the first row its `**Layers:**` line names, or in the first row when it has no such line. That layer's tests run in process against the real database
and exercise every layer below it, so `/use-case-test` runs its test skill and the ones above it and writes no tests
of their own for the layers below unless a run names them (`/use-case-test UC-001 persistence`).

**E2E scope:** Use cases and test cases

`Use cases and test cases`: `/use-case-test` adds a browser test to every use case offered through the UI, and
`/test-case-implement` automates the TC-* journeys. `Test cases only`: browser tests are written for the journeys
alone, and a use case is tested in its integration layer. A project without an E2E layer drops the line.

## External systems

| System        | Contract | Location            |
|---------------|----------|---------------------|
| KI-Modell     | –        | `info.wylan.devace.client` |
| E-Mail-System | –        | `info.wylan.devace.client` |

The systems our system calls, by the name the use cases write on their `**Secondary Actors:**` line: the Client
layer builds an adapter for each one from its contract (OpenAPI or AsyncAPI; `–` while there is none). A secondary
actor without a row is a person or a role. Without a Client layer the section is dropped.

## Module layout

Flat single-module Maven project (`./mvnw`), Maven coordinates `info.wylan:devace` (groupId:artifactId), root package `info.wylan.devace`, one sub-package per layer: `ui`, `service`, `persistence`, `client`. Tests live under `src/test/java/info/wylan/devace`, E2E tests under `info.wylan.devace.e2e`.

## Commands

| Purpose            | Command            |
|--------------------|--------------------|
| Build              | `./mvnw -B verify` |
| Unit tests         | `./mvnw -B test`   |
| Integration tests  | `./mvnw -B verify` |
| End-to-end tests   | `./mvnw -B verify` |

## Markers

Each layer skill keeps its own rows here: on every run it checks that the rows for its layer match its marker
reference and corrects them. Do not edit the rows by hand; rerun the layer skill instead.

| Layer | Skill | Kind | Marker to grep for |
|-------|-------|------|--------------------|
| All   | –     | Code | `// UC-XXX BR-YYY:` directly above the method, query condition, or validator that enforces the rule |
