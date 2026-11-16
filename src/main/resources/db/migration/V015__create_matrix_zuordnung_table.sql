-- V015__create_matrix_zuordnung_table.sql

CREATE SEQUENCE matrix_zuordnung_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE matrix_zuordnung
(
    id               BIGINT DEFAULT nextval('matrix_zuordnung_seq') PRIMARY KEY,
    handlungsfeld_id BIGINT       NOT NULL REFERENCES handlungsfeld (id),
    -- UC-010 BR-001: Der Name der Spalte ist Pflichtangabe mit höchstens 200 Zeichen.
    matrix_spalte    VARCHAR(200) NOT NULL CONSTRAINT matrix_zuordnung_matrix_spalte_ck CHECK (length(trim(matrix_spalte)) > 0),
    -- UC-010 BR-001: Die Begründung ist optional mit höchstens 500 Zeichen.
    begruendung      VARCHAR(500)
);

/* [jooq ignore start] */
-- UC-010 BR-002: Eine Spalte ist höchstens einem Handlungsfeld zugeordnet (ohne Schreibweise, ohne Leerzeichen).
CREATE UNIQUE INDEX matrix_zuordnung_matrix_spalte_uk ON matrix_zuordnung (lower(trim(matrix_spalte)));
/* [jooq ignore stop] */
