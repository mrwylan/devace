-- V004__create_fragenkatalog_table.sql

CREATE SEQUENCE fragenkatalog_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE fragenkatalog
(
    id                 BIGINT DEFAULT nextval('fragenkatalog_seq') PRIMARY KEY,
    rolle_id           BIGINT      NOT NULL REFERENCES rolle (id),
    version            INTEGER     NOT NULL CHECK (version BETWEEN 1 AND 9999),
    status             VARCHAR(20) NOT NULL CHECK (status IN ('Entwurf', 'Freigegeben', 'Archiviert')),
    erstellt_am        TIMESTAMP   NOT NULL,
    freigegeben_am     TIMESTAMP,
    freigegeben_von_id BIGINT REFERENCES benutzer (id),
    CONSTRAINT fragenkatalog_rolle_version_uk UNIQUE (rolle_id, version),
    CONSTRAINT fragenkatalog_freigabe_ck CHECK (
        (status IN ('Freigegeben', 'Archiviert') AND freigegeben_am IS NOT NULL AND freigegeben_von_id IS NOT NULL)
        OR (status = 'Entwurf' AND freigegeben_am IS NULL AND freigegeben_von_id IS NULL))
);

/* [jooq ignore start] */
CREATE UNIQUE INDEX fragenkatalog_freigegeben_uk ON fragenkatalog (rolle_id) WHERE status = 'Freigegeben';
/* [jooq ignore stop] */
