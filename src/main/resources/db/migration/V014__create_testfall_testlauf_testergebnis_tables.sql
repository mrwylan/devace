-- V014__create_testfall_testlauf_testergebnis_tables.sql

CREATE SEQUENCE testfall_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE testfall
(
    id              BIGINT DEFAULT nextval('testfall_seq') PRIMARY KEY,
    -- UC-009 BR-001: Ein Testfall gehört zu genau einer Frage.
    -- UC-009 BR-010: Entfernt UC-006 eine Frage, entfallen ihre Testfälle mit.
    frage_id        BIGINT       NOT NULL REFERENCES frage (id) ON DELETE CASCADE,
    -- UC-009 BR-001: Die vorgegebene Antwort ist Pflicht und hat höchstens 500 Zeichen.
    antworttext     VARCHAR(500) NOT NULL CHECK (length(trim(antworttext)) > 0),
    -- UC-009 BR-001: Die erwartete Stufe ist eine ganze Zahl von 0 bis 5.
    erwartete_stufe INTEGER      NOT NULL CHECK (erwartete_stufe BETWEEN 0 AND 5)
);

CREATE INDEX testfall_frage_idx ON testfall (frage_id);

CREATE SEQUENCE testlauf_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE testlauf
(
    id                 BIGINT DEFAULT nextval('testlauf_seq') PRIMARY KEY,
    fragenkatalog_id   BIGINT        NOT NULL REFERENCES fragenkatalog (id),
    ausgefuehrt_von_id BIGINT        NOT NULL REFERENCES benutzer (id),
    ausgefuehrt_am     TIMESTAMP     NOT NULL,
    ki_modell          VARCHAR(100)  NOT NULL,
    -- UC-009 BR-005: Anteil in Prozent mit höchstens zwei Nachkommastellen.
    trefferquote       NUMERIC(5, 2) NOT NULL CHECK (trefferquote BETWEEN 0 AND 100)
);

CREATE INDEX testlauf_fragenkatalog_idx ON testlauf (fragenkatalog_id);

CREATE SEQUENCE testergebnis_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE testergebnis
(
    id               BIGINT DEFAULT nextval('testergebnis_seq') PRIMARY KEY,
    testlauf_id      BIGINT       NOT NULL REFERENCES testlauf (id),
    -- UC-009 BR-007: Der Testlauf bleibt mit seinen Abweichungen erhalten, auch wenn der Testfall entfällt.
    testfall_id      BIGINT REFERENCES testfall (id) ON DELETE SET NULL,
    -- UC-009 BR-007: Kopie des Testfalls zum Zeitpunkt des Testlaufs (OQ-083).
    frage_text       VARCHAR(500) NOT NULL,
    antworttext      VARCHAR(500) NOT NULL,
    erwartete_stufe  INTEGER      NOT NULL CHECK (erwartete_stufe BETWEEN 0 AND 5),
    ermittelte_stufe INTEGER      NOT NULL CHECK (ermittelte_stufe BETWEEN 0 AND 5),
    CONSTRAINT testergebnis_testlauf_testfall_uk UNIQUE (testlauf_id, testfall_id)
);
