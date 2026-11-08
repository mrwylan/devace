-- V005__create_handlungsfeld_table.sql
-- Die sechs Handlungsfelder sind feste Stammdaten (UC-006 Vorbedingung, Vision §6).

CREATE SEQUENCE handlungsfeld_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE handlungsfeld
(
    id     BIGINT DEFAULT nextval('handlungsfeld_seq') PRIMARY KEY,
    nummer INTEGER      NOT NULL UNIQUE CHECK (nummer BETWEEN 1 AND 6),
    name   VARCHAR(100) NOT NULL UNIQUE,
    fokus  VARCHAR(200)
);

INSERT INTO handlungsfeld (nummer, name, fokus)
VALUES (1, 'Domänenwissen & Business Alignment', 'Verstehen, wie Transgourmet arbeitet'),
       (2, 'Architektur & Engineering', 'Zertifikatswissen (iSAQB Foundation) im Alltag anwenden'),
       (3, 'Qualität & Testing', 'iSTQB Foundation anwenden'),
       (4, 'Security & DevSecOps', 'Momentum aus dem CTF-Labor halten'),
       (5, 'Methodik & Agile Delivery', 'Effizient und transparent Wert liefern'),
       (6, 'Future Skills & AI', 'Blick Richtung 2030');
