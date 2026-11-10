-- V002__create_mag_table.sql

CREATE SEQUENCE mag_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE mag
(
    id             BIGINT DEFAULT nextval('mag_seq') PRIMARY KEY,
    scrum_team     VARCHAR(100) NOT NULL,
    linienteam     VARCHAR(100) NOT NULL,
    teamleitung_id BIGINT REFERENCES benutzer (id)
);

/* [jooq ignore start] */
-- UC-008 BR-002: Scrum Team und Linienteam gehören zu höchstens einer MAG, ohne Groß-/Kleinschreibung.
CREATE UNIQUE INDEX mag_scrum_team_linienteam_uk ON mag (lower(scrum_team), lower(linienteam));
/* [jooq ignore stop] */
