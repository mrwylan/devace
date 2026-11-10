-- V006__create_frage_table.sql

CREATE SEQUENCE frage_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE frage
(
    id               BIGINT DEFAULT nextval('frage_seq') PRIMARY KEY,
    fragenkatalog_id BIGINT       NOT NULL REFERENCES fragenkatalog (id),
    handlungsfeld_id BIGINT       NOT NULL REFERENCES handlungsfeld (id),
    -- UC-006 GR-002: Der Wortlaut ist eine Pflichtangabe mit höchstens 500 Zeichen.
    text             VARCHAR(500) NOT NULL CHECK (length(trim(text)) > 0),
    -- UC-006 GR-002: Die Position einer Frage im Fragenkatalog ist eindeutig.
    position         INTEGER      NOT NULL CHECK (position BETWEEN 1 AND 999),
    CONSTRAINT frage_katalog_position_uk UNIQUE (fragenkatalog_id, position)
);
