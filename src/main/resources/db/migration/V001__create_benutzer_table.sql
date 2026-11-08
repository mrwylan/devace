-- V001__create_benutzer_table.sql
-- Benutzerkonten für die Anmeldung (OQ-023, OQ-031). Das Entity-Modell nennt BENUTZER nur als
-- externe Systemgrenze; die Spalten sind in OQ-031 festgelegt.

CREATE SEQUENCE benutzer_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE benutzer
(
    id            BIGINT DEFAULT nextval('benutzer_seq') PRIMARY KEY,
    benutzername  VARCHAR(100) NOT NULL UNIQUE,
    passwort_hash VARCHAR(255) NOT NULL,
    berechtigung  VARCHAR(30)  NOT NULL
        CHECK (berechtigung IN ('Teamleitung', 'Schulungsverantwortliche', 'Administrator'))
);
