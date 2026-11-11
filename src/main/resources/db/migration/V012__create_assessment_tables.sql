-- V012__create_assessment_tables.sql
-- Assessment, Antwort und Bewertung tragen keine E-Mail-Adresse und keinen Namen (UC-002 BR-007).

CREATE SEQUENCE assessment_seq START WITH 1 INCREMENT BY 1 CACHE 50;
CREATE SEQUENCE antwort_seq START WITH 1 INCREMENT BY 1 CACHE 50;
CREATE SEQUENCE bewertung_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE assessment
(
    id                BIGINT DEFAULT nextval('assessment_seq') PRIMARY KEY,
    -- UC-002 BR-001: Eine Einladung berechtigt zu genau einem Assessment.
    einladung_id      BIGINT      NOT NULL REFERENCES einladung (id),
    mag_id            BIGINT      NOT NULL REFERENCES mag (id),
    rolle_id          BIGINT      NOT NULL REFERENCES rolle (id),
    fragenkatalog_id  BIGINT      NOT NULL REFERENCES fragenkatalog (id),
    -- UC-002 BR-002: Das Assessment beginnt erst mit dem bestätigten Starthinweis.
    starthinweis_id   BIGINT      NOT NULL REFERENCES starthinweis (id),
    status            VARCHAR(20) NOT NULL,
    gestartet_am      TIMESTAMP   NOT NULL,
    letzte_eingabe_am TIMESTAMP   NOT NULL,
    CONSTRAINT assessment_einladung_uk UNIQUE (einladung_id),
    CONSTRAINT assessment_status_ck CHECK (status IN ('Laufend', 'Abgeschlossen', 'Abgebrochen'))
);

CREATE TABLE antwort
(
    id             BIGINT DEFAULT nextval('antwort_seq') PRIMARY KEY,
    assessment_id  BIGINT       NOT NULL REFERENCES assessment (id) ON DELETE CASCADE,
    frage_id       BIGINT       NOT NULL REFERENCES frage (id),
    -- UC-002 BR-004: Eine Antwort ist nicht leer und hat höchstens 500 Zeichen.
    text           VARCHAR(500) NOT NULL CHECK (length(trim(text)) > 0),
    beantwortet_am TIMESTAMP    NOT NULL,
    -- UC-002 BR-005: Jede Frage wird genau einmal beantwortet.
    CONSTRAINT antwort_assessment_frage_uk UNIQUE (assessment_id, frage_id)
);

CREATE TABLE bewertung
(
    id          BIGINT DEFAULT nextval('bewertung_seq') PRIMARY KEY,
    antwort_id  BIGINT    NOT NULL REFERENCES antwort (id) ON DELETE CASCADE,
    -- UC-002 BR-006: Eine Stufe von 0 bis 5.
    stufe       INTEGER   NOT NULL CHECK (stufe BETWEEN 0 AND 5),
    bewertet_am TIMESTAMP NOT NULL,
    CONSTRAINT bewertung_antwort_uk UNIQUE (antwort_id)
);
