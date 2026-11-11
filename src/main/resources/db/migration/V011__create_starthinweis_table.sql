-- V011__create_starthinweis_table.sql

CREATE SEQUENCE starthinweis_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE starthinweis
(
    id         BIGINT DEFAULT nextval('starthinweis_seq') PRIMARY KEY,
    -- UC-011 BR-002: Jede Änderung ist eine neue Version mit einer um 1 höheren Nummer.
    version    INTEGER      NOT NULL CHECK (version BETWEEN 1 AND 9999),
    -- UC-011 BR-001: Der Text ist eine Pflichtangabe mit höchstens 500 Zeichen.
    text       VARCHAR(500) NOT NULL CHECK (length(trim(text)) > 0),
    -- UC-011 BR-001: Die eingesetzten KI-Modelle sind eine Pflichtangabe mit höchstens 500 Zeichen.
    ki_modelle VARCHAR(500) NOT NULL CHECK (length(trim(ki_modelle)) > 0),
    gueltig_ab DATE         NOT NULL,
    CONSTRAINT starthinweis_version_uk UNIQUE (version)
);
