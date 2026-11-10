-- V008__add_fragenkatalog_entwurf_index.sql

/* [jooq ignore start] */
-- UC-006 BR-004: Eine Rolle hat höchstens einen Entwurf (Annahme OQ-027).
CREATE UNIQUE INDEX fragenkatalog_entwurf_uk ON fragenkatalog (rolle_id) WHERE status = 'Entwurf';
/* [jooq ignore stop] */
