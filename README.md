# devace

KI-gestütztes Kompetenz-Assessment für Entwicklungsteams. Ein Administrator erstellt je Rolle einen Fragenkatalog mit
Entscheidungsmodell, Teamleitungen laden Teilnehmer ein, die Teilnehmer beantworten die Fragen im Dialog, die KI ordnet die
Antworten Stufen von 0 bis 5 zu. Das Ergebnis ist ein Radar-Chart je Mitarbeitergruppe (MAG).

Fachliche Grundlagen stehen in [`docs/`](docs/): [Vision](docs/vision.md), [Anforderungen](docs/requirements.md),
[Entity-Modell](docs/entity_model.md), [Use Cases](docs/use_cases/) und die
[offenen Fragen](docs/open_questions.md). Die Spielregeln für die Umsetzung stehen in [`CLAUDE.md`](CLAUDE.md).

**Stand:** umgesetzt und getestet sind UC-006 (Fragenkatalog und Entscheidungsmodell erstellen) und UC-008 (MAGs und
Rollen verwalten). Die Oberfläche folgt den Figma-Mockups, die in den Use Cases verlinkt sind.

## Stack

Java 25, Spring Boot 4.1.2, Vaadin 25 (Flow, Aura-Theme, dunkel), PostgreSQL 17 mit Flyway, jOOQ, Spring AI über
OpenRouter, Maven (`./mvnw`). Container-Image mit Spring-Boot-Buildpacks, CI mit GitHub Actions.

## Voraussetzungen

- JDK 25
- Docker (für die Datenbank und für die Tests, die Testcontainers nutzen)
- Ein OpenRouter-Schlüssel, nur wenn die KI-Entwürfe wirklich ein Modell aufrufen sollen

Maven ist nicht nötig, das Projekt bringt den Wrapper `./mvnw` mit.

## Umgebungsvariablen

Geheimnisse und umgebungsabhängige Werte kommen ausschließlich aus Umgebungsvariablen. Es wird nichts davon
committet, auch keine `.env`-Datei mit echten Werten.

| Variable | Pflicht | Bedeutung |
|---|---|---|
| `DEVACE_DB_URL` | ja | JDBC-URL der Datenbank, z. B. `jdbc:postgresql://localhost:5432/devace` |
| `DEVACE_DB_USER` | ja | Datenbankbenutzer |
| `DEVACE_DB_PASSWORD` | ja | Passwort des Datenbankbenutzers |
| `DEVACE_ADMIN_USERNAME` | nein | Benutzername des ersten Administrators |
| `DEVACE_ADMIN_PASSWORD` | nein | Passwort des ersten Administrators |
| `OPENROUTER_API_KEY` | für KI | Schlüssel für OpenRouter, das einzige Tor zu den KI-Modellen |
| `DEVACE_KI_MODELL_FRAGENKATALOG` | für KI | Modell-ID für den KI-Entwurf des Fragenkatalogs, z. B. `anthropic/claude-haiku-5.5` |
| `DEVACE_KI_MODELL_BEWERTUNG` | nein | Version des Entscheidungsmodells Jev für die Zuordnung der Antworten zu Stufen (UC-002, ADR-003), Standard `~typesafe/jev-latest`; zum Pinnen z. B. `typesafe/jev-1.14-20261014`. Ohne `OPENROUTER_API_KEY` gilt jede Antwort als Stufe 0 |
| `DEVACE_MAIL_HOST` | für Einladungen | SMTP-Server für den Versand der Einladungen (UC-001); ohne ihn scheitert jeder Versand und die Einladung wird verworfen |
| `DEVACE_MAIL_PORT` | nein | Port des SMTP-Servers, Standard `587` |
| `DEVACE_MAIL_USER`, `DEVACE_MAIL_PASSWORD` | nein | Anmeldung am SMTP-Server |
| `DEVACE_MAIL_AUTH`, `DEVACE_MAIL_STARTTLS` | nein | `true`, wenn der Server Anmeldung oder STARTTLS verlangt, Standard `false` |
| `DEVACE_MAIL_ABSENDER` | für Einladungen | Absenderadresse der Einladungen |
| `DEVACE_BASIS_URL` | nein | Öffentliche Adresse der Anwendung für den Link in der E-Mail, Standard `http://localhost:8080` |
| `SERVER_PORT` | nein | Port der Anwendung, Standard `8080` |

Hinweise:

- **Erster Administrator:** Der Start legt den Administrator nur an, wenn beide Variablen gesetzt sind und noch keiner
  existiert. Ohne Administrator kann sich niemand an der Verwaltung anmelden.
- **KI:** Fehlt der Schlüssel oder die Modell-ID, bleibt die Anwendung benutzbar. Der KI-Entwurf meldet dann, dass kein
  Entwurf entstanden ist. Es gibt bewusst keine Standard-Modell-ID. Pro Aufruf sind es wenige Cent, setze trotzdem
  beim Schlüssel ein Ausgabelimit bei OpenRouter.
- **Schlüssel ablegen:** Am einfachsten in einer Datei außerhalb des Repositorys, nur für dich lesbar:

  ```bash
  umask 077
  cat > ~/.devace-env <<'EOF'
  DEVACE_DB_URL=jdbc:postgresql://localhost:5432/devace
  DEVACE_DB_USER=devace
  DEVACE_DB_PASSWORD=<dein Passwort>
  DEVACE_ADMIN_USERNAME=admin
  DEVACE_ADMIN_PASSWORD=<ein Passwort>
  OPENROUTER_API_KEY=<dein Schlüssel>
  DEVACE_KI_MODELL_FRAGENKATALOG=anthropic/claude-haiku-5.5
  EOF
  chmod 600 ~/.devace-env
  ```

  Laden mit `set -a; . ~/.devace-env; set +a`.

## Datenbank mit Docker

devace braucht PostgreSQL in der Hauptversion der Produktion (17). Das Schema legt Flyway beim Start an, du musst
nichts von Hand einspielen.

### Starten

```bash
docker run -d --name devace-db \
  -e POSTGRES_USER=devace \
  -e POSTGRES_PASSWORD='<dein Passwort>' \
  -e POSTGRES_DB=devace \
  -p 5432:5432 \
  -v devace-pgdata:/var/lib/postgresql/data \
  --restart unless-stopped \
  postgres:17-alpine
```

Dazu passen die Variablen `DEVACE_DB_URL=jdbc:postgresql://localhost:5432/devace`, `DEVACE_DB_USER=devace` und das
gewählte Passwort in `DEVACE_DB_PASSWORD`. Das benannte Volume `devace-pgdata` hält die Daten über Neustarts und das
Löschen des Containers hinweg.

Ist Port 5432 belegt, mappe einen anderen (`-p 5433:5432`) und passe die URL an.

### Alternativ: Docker Compose

Lege die Datei `compose.yaml` an einem Ort deiner Wahl an (sie gehört nicht ins Repository, sie enthält das Passwort):

```yaml
services:
  db:
    image: postgres:17-alpine
    container_name: devace-db
    environment:
      POSTGRES_USER: devace
      POSTGRES_PASSWORD: ${DEVACE_DB_PASSWORD}
      POSTGRES_DB: devace
    ports:
      - "5432:5432"
    volumes:
      - devace-pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U devace -d devace"]
      interval: 5s
      retries: 10
    restart: unless-stopped

volumes:
  devace-pgdata:
```

Start mit `docker compose up -d`, der Passwort-Wert kommt aus deiner Umgebung (`DEVACE_DB_PASSWORD`).

### Betrieb

```bash
docker ps                                   # läuft der Container?
docker logs -f devace-db                    # Logs
docker exec -it devace-db psql -U devace    # SQL-Konsole
docker stop devace-db && docker start devace-db

# Sicherung und Wiederherstellung
docker exec devace-db pg_dump -U devace devace > devace-backup.sql
docker exec -i devace-db psql -U devace devace < devace-backup.sql

# Alles auf null: Container und Daten löschen (unwiderruflich)
docker rm -f devace-db && docker volume rm devace-pgdata
```

Nach dem Zurücksetzen legt der nächste Start das Schema über Flyway neu an.

## Anwendung starten

```bash
set -a; . ~/.devace-env; set +a        # Umgebungsvariablen laden
./mvnw spring-boot:run -Dspring-boot.run.useTestClasspath=true
```

Danach ist die Anwendung unter <http://localhost:8080> erreichbar und fragt nach der Anmeldung. Der Administrator
aus `DEVACE_ADMIN_USERNAME` und `DEVACE_ADMIN_PASSWORD` sieht die Verwaltung unter *MAGs und Rollen* und
*Fragenkatalog*.

Der Zusatz `useTestClasspath` ist für den Entwicklungsbetrieb nötig: Der Vaadin-Dev-Server liegt nur im Test-Scope.

## Bauen und testen

```bash
./mvnw -B verify
```

`verify` kompiliert, prüft die Enforcer-Regeln und führt die Unit-, Integrations- und Browser-Tests (Failsafe,
Playwright) aus.

- Die Tests starten ihre eigene PostgreSQL-Datenbank per Testcontainers. Docker muss dafür laufen.
- Unter WSL setze `TESTCONTAINERS_RYUK_DISABLED=true`.
- Der Test `DevaceApplicationTests` startet den ganzen Kontext gegen die in `DEVACE_DB_*` konfigurierte Datenbank.
  Setze die drei Variablen also auch für `verify`, zum Beispiel auf den Docker-Container von oben.
- Kein Test ruft ein echtes KI-Modell auf, das KI-Modell ist in den Tests durch einen Fake ersetzt. Der Schlüssel wird
  für die Tests nicht gebraucht.

Einzelne Tests: `./mvnw -B test -Dtest='UC006*Test'`, Browser-Tests mit
`./mvnw -B verify -Dtest=NoSuchTest -Dit.test='UC006*IT' -Dsurefire.failIfNoSpecifiedTests=false`.

## Container-Image

Das Image entsteht mit den Spring-Boot-Buildpacks, nicht mit einem Dockerfile:

```bash
./mvnw -B -Pproduction spring-boot:build-image -DskipTests -Dspring-boot.build-image.imageName=devace:local
```

Das Profil `production` bündelt das Vaadin-Frontend. Ohne es startet der Container im Entwicklungsmodus und bricht ab.
Beim Start des Containers gelten dieselben Umgebungsvariablen wie oben.

## Arbeiten am Projekt

Das Projekt folgt dem AI Unified Process (AIUP): Spezifikation, Review, Umsetzung, Tests, Coverage. Der Ablauf, die
verbindlichen Technologie-Entscheidungen und was ohne ADR verboten ist, stehen in [`CLAUDE.md`](CLAUDE.md). Die
Architekturentscheidungen liegen in [`docs/architecture/decisions/`](docs/architecture/decisions/), die technischen
Schichten in [`docs/architecture/stack.md`](docs/architecture/stack.md).
