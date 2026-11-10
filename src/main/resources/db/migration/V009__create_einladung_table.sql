CREATE SEQUENCE einladung_seq START WITH 1 INCREMENT BY 1 CACHE 50;

-- Die E-Mail-Adresse einer Teilnehmerin oder eines Teilnehmers hat keine Spalte (UC-001 BR-004).
CREATE TABLE einladung
(
    id               BIGINT DEFAULT nextval('einladung_seq') PRIMARY KEY,
    referenz         VARCHAR(36) NOT NULL,
    mag_id           BIGINT      NOT NULL REFERENCES mag (id),
    rolle_id         BIGINT      NOT NULL REFERENCES rolle (id),
    erstellt_von_id  BIGINT      NOT NULL REFERENCES benutzer (id),
    status           VARCHAR(20) NOT NULL,
    erstellt_am      TIMESTAMP   NOT NULL,
    gueltigkeit_tage INTEGER     NOT NULL,
    -- UC-001 BR-001: Die Referenz einer Einladung ist eindeutig.
    CONSTRAINT einladung_referenz_uk UNIQUE (referenz),
    CONSTRAINT einladung_status_ck CHECK (status IN ('Erstellt', 'Versendet', 'Eingelöst')),
    -- UC-001 BR-008: Die Gültigkeitsdauer ist eine ganze Zahl von 1 bis 30 Tagen.
    CONSTRAINT einladung_gueltigkeit_tage_ck CHECK (gueltigkeit_tage BETWEEN 1 AND 30)
);
