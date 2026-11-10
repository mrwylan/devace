-- V003__create_rolle_table.sql

CREATE SEQUENCE rolle_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE rolle
(
    id   BIGINT DEFAULT nextval('rolle_seq') PRIMARY KEY,
    name VARCHAR(50) NOT NULL
);

/* [jooq ignore start] */
-- UC-008 BR-003: Der Name einer Rolle gehört zu höchstens einer Rolle, ohne Groß-/Kleinschreibung.
CREATE UNIQUE INDEX rolle_name_uk ON rolle (lower(name));
/* [jooq ignore stop] */
