-- V007__create_stufenvorgabe_table.sql

CREATE SEQUENCE stufenvorgabe_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE stufenvorgabe
(
    id           BIGINT DEFAULT nextval('stufenvorgabe_seq') PRIMARY KEY,
    frage_id     BIGINT       NOT NULL REFERENCES frage (id) ON DELETE CASCADE,
    -- UC-006 BR-001: Jede Frage hat genau eine Vorgabe je Stufe von 0 bis 5.
    stufe        INTEGER      NOT NULL CHECK (stufe BETWEEN 0 AND 5),
    -- UC-006 BR-001: Jede Vorgabe ist eine Pflichtangabe mit höchstens 500 Zeichen.
    beschreibung VARCHAR(500) NOT NULL CHECK (length(trim(beschreibung)) > 0),
    CONSTRAINT stufenvorgabe_frage_stufe_uk UNIQUE (frage_id, stufe)
);
