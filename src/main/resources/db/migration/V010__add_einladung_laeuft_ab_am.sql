ALTER TABLE einladung ADD COLUMN laeuft_ab_am DATE;

-- bereits vorhandene Einladungen: der Erstellungstag ist der erste Tag der Gültigkeit
UPDATE einladung SET laeuft_ab_am = CAST(erstellt_am AS DATE) + gueltigkeit_tage - 1;

ALTER TABLE einladung ALTER COLUMN laeuft_ab_am SET NOT NULL;

-- UC-001 BR-008: Die Einladung gilt bis 24 Uhr des letzten Tages der Gültigkeitsdauer; der Erstellungstag ist der erste Tag.
ALTER TABLE einladung ADD CONSTRAINT einladung_laeuft_ab_am_ck
    CHECK (laeuft_ab_am = CAST(erstellt_am AS DATE) + gueltigkeit_tage - 1);
