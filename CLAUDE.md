# CLAUDE.md — Project Harness

This file is the harness for this project. It is loaded into every agent session and defines
(1) the enforced technology stack, (2) how work flows through the AI Unified Process (AIUP),
and (3) what "verified" means before work is reported as done.

If a request conflicts with this file, stop and ask. Do not silently deviate.

---

## 1. Enforced technology stack

These choices are fixed. Do not add an alternative, a second framework for the same job, or an
older version. Changing any of them requires a new accepted ADR (see §2.4) and an update to this file.

| Concern | Required | Forbidden / notes |
|---|---|---|
| Language | **Java 25** (`maven.compiler.release` = 25) | Java 21/17/11, Kotlin, Groovy in production code |
| Framework | **Spring Boot 4.1.2** (`spring-boot-starter-parent` 4.1.2, the only Boot version) | Spring Boot 3.x or any other 4.x patch; pulling Spring dependencies with their own versions |
| Build | **Maven** with the committed wrapper (`./mvnw`) | Gradle, Ant, a globally installed Maven |
| Web UI | **Vaadin 25** (Flow, server-side Java views) via `vaadin-spring-boot-starter` | Angular, React, Vue, Thymeleaf pages, hand-written SPA frontends |
| Persistence | **PostgreSQL** (runtime and dev), schema managed by **Flyway** migrations under `src/main/resources/db/migration` | H2 or any in-memory DB for tests; `ddl-auto=update|create` in any profile; hand-edited schema |
| Data access | **Decision pending:** Spring Data JPA *or* jOOQ. Pick one in an ADR before the first entity is implemented. Do not mix both. | Mixing JPA and jOOQ for the same aggregate |
| LLM access | **Spring AI** only, routed through **OpenRouter** as the single model gateway | Direct provider SDKs (OpenAI, Anthropic, Google, etc.) for production calls; hard-coded API keys |
| Deployment artifact | **A container image** built from the Maven build (Jib or Spring Boot buildpacks; pick one in an ADR) | Shipping a bare JAR, or a hand-maintained Dockerfile that duplicates the Maven build without an ADR |
| CI/CD | **GitHub Actions** (`.github/workflows/`) | Other CI systems, manual deploys from a laptop |
| Database for tests | **Testcontainers** with a PostgreSQL image matching production major version | Embedded/in-memory databases |

### 1.1 Secrets and configuration

- The OpenRouter key is read from the environment variable `OPENROUTER_API_KEY`. It is never committed.
- Application config uses `application.yml` with `${ENV_VAR}` placeholders for every secret and
  environment-specific value. Do not commit `.env` files containing real values.
- Database credentials come from environment variables in every profile except local tests
  (which use Testcontainers).

### 1.2 Enforcement in the build

The POM must enforce the stack, not just document it. The following must be configured and must
fail the build on violation:

- `maven-enforcer-plugin`: `requireJavaVersion` ≥ 25, `requireMavenVersion`, `requireUpperBoundDeps`,
  `banDuplicateClasses`, and `bannedDependencies` for the forbidden persistence and LLM SDKs.
- `spring-boot-starter-parent` pinned to `4.1.2`; no version overrides for Spring-managed artifacts
  unless documented in an ADR.
- Vaadin BOM pinned to a single explicit version property, shared with the Vaadin Maven plugin.
- Spring AI BOM pinned to a single explicit version property.

If the enforcer configuration is missing or relaxed, treat that as a defect and fix it before any
feature work.

---

## 2. AI Unified Process (AIUP) workflow

This project follows the AI Unified Process from https://github.com/AI-Unified-Process. Use the
AIUP plugins for the process steps; do not invent a parallel process.

### 2.1 Installation (once per machine)

```bash
# Claude Code
/plugin marketplace add ai-unified-process/marketplace
/plugin install aiup-core@ai-unified-process
/plugin install aiup-spring-boot@ai-unified-process
/plugin install aiup-vaadin@ai-unified-process
/plugin install aiup-flyway@ai-unified-process
/plugin install aiup-playwright@ai-unified-process
# plus aiup-jpa or aiup-jooq, matching the persistence decision in §1
```

Use layer plugins. Do not also install a combined stack plugin (`aiup-vaadin-jooq`,
`aiup-angular-jpa`), which are deprecated.

For other agents (Codex CLI, Cursor, Copilot, Gemini CLI, OpenCode), install `aiup-core` via Tessl
per the marketplace README.

### 2.2 Project context — read before deciding

Before making product, domain, or architecture decisions, read in this order:

1. `docs/vision.md`
2. `docs/requirements.md`
3. `docs/entity_model.md`
4. Relevant `docs/use_cases/UC-*.md` and `docs/test_cases/`
5. Accepted ADRs in `docs/architecture/decisions/`
6. `docs/architecture/stack.md` (recorded by `/stack`; must agree with §1 of this file)

If any of these files are missing, create them through the AIUP skills. Do not write them by hand
from memory.

### 2.3 Workflow rules

1. Derive requirements from the product vision (`/requirements`).
2. Derive the entity model and use case diagram from requirements (`/entity-model`); reconcile both
   whenever requirements change.
3. **Do not implement a use case until its `UC-*.md` specification exists and has been reviewed**
   (`/use-case-spec`, `/spec-review`).
4. Keep requirement, use case, and test case IDs stable and traceable in code and tests
   (e.g. `@UseCase("UC-012")` on tests, IDs in commit messages).
5. Implement through the installed layer plugins: `/use-case-implement` for code, `/use-case-test`
   for tests, `/use-case-coverage` for coverage. Migrations go through `aiup-flyway`.
6. Follow conventions already established in the codebase before introducing new ones.
7. Use `/next` to pick the next use case; do not choose work ad hoc.

### 2.4 Architecture decisions

Record every decision that changes the stack, a non-functional requirement, or a cross-cutting
pattern with `/architecture-decision`. The ADR must link to the requirement or constraint it serves.
Accepted ADRs override conflicting code. Decisions still open in §1 (persistence layer, container
build tool) are the first ADRs to write.

---

## 3. Build pipeline (GitHub Actions)

The pipeline lives in `.github/workflows/` and is the only path to a deployable artifact.

Required jobs, in order:

1. **verify** — on every push and pull request:
   - `./mvnw -B verify` (compiles with `release 25`, runs unit and integration tests against a
     PostgreSQL Testcontainer, runs enforcer rules and static analysis).
   - Fail on enforcer violations, failing tests, or dependency-convergence errors.
   - Upload test reports and JaCoCo coverage as artifacts.
2. **image** — on pushes to the default branch only, after `verify` passes:
   - Build the container image with the tool chosen in the container ADR.
   - Tag with the commit SHA and the version from the POM. Never publish `latest` alone.
   - Push to GitHub Container Registry (`ghcr.io`) using the built-in `GITHUB_TOKEN` with
     `packages: write`.

Pipeline rules:

- Pin third-party actions to a full commit SHA, not a tag.
- Use `actions/setup-java` with Java 25 and the Maven cache enabled.
- Set minimal `permissions:` per job. Default to `contents: read`.
- Never print secrets. Never use `pull_request_target` to run untrusted PR code.
- Do not add a deploy job without an accepted ADR.

---

## 4. Forbidden without an ADR

- Any version of Java other than 25, Spring Boot other than 4.1.2 (once the first ADR is accepted,
  upgrades go through an ADR), or Maven replaced by another build tool.
- A second UI technology, or Vaadin Flow replaced by Vaadin Fusion-only or a JavaScript SPA.
- An in-memory database in any profile, including tests.
- Direct calls to a model provider's SDK bypassing OpenRouter.
- Disabling tests, enforcer rules, NullAway, ErrorProne, or static analysis to get a build green.
- Committing secrets, API keys, or `.env` files with real values.

---

## 5. Verification before reporting done

Before reporting work as complete:

1. Run `./mvnw -B verify` locally and report the result.
2. Run the relevant AIUP coverage skill for the use case you touched.
3. If you could not run something, say so explicitly and name what is unverified. Do not claim a
   build passes without having run it.
4. Confirm the change respects §1 (stack) and §4 (forbidden). If it does not, the change is not done.
