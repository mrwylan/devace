-- V013__create_score_table.sql
-- UC-003 BR-002: Ein Score verweist auf keine Einladung, kein Assessment, keine Antwort und keine Person.

CREATE SEQUENCE score_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE score
(
    id               BIGINT DEFAULT nextval('score_seq') PRIMARY KEY,
    mag_id           BIGINT        NOT NULL REFERENCES mag (id),
    rolle_id         BIGINT        NOT NULL REFERENCES rolle (id),
    handlungsfeld_id BIGINT        NOT NULL REFERENCES handlungsfeld (id),
    fragenkatalog_id BIGINT        NOT NULL REFERENCES fragenkatalog (id),
    -- UC-003 BR-001: zwischen 0 und 5, kaufmännisch auf eine Nachkommastelle gerundet
    wert             NUMERIC(10, 1) NOT NULL CHECK (wert BETWEEN 0 AND 5),
    erfasst_am       DATE          NOT NULL
);
