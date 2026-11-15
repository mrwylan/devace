-- Testdaten für UC-004 Radar-Chart einer MAG auswerten (wiederholbar, ändert keine Produktionsdaten).
-- Feste Ids ab 9601; die Sequenzen der Produktionstabellen vergeben sie nicht.
-- uc004-teamleitung meldet sich im Browser-Test mit dem Testpasswort an, kein echtes Passwort.

INSERT INTO benutzer (id, benutzername, passwort_hash, berechtigung)
VALUES (9601, 'uc004-teamleitung', '{noop}uc004-test', 'Teamleitung'),
       (9602, 'uc004-ohne-scores', 'unused', 'Teamleitung'),
       (9603, 'uc004-andere-teamleitung', 'unused', 'Teamleitung')
ON CONFLICT DO NOTHING;

-- 9601 mit Scores, 9602 ohne Scores (beide uc004-teamleitung); 9603 ohne Scores (uc004-ohne-scores);
-- 9604 mit Scores, aber einer anderen Teamleitung
INSERT INTO mag (id, scrum_team, linienteam, teamleitung_id)
VALUES (9601, 'UC004 Alpha', 'UC004 Nord', 9601),
       (9602, 'UC004 Beta', 'UC004 Süd', 9601),
       (9603, 'UC004 Gamma', 'UC004 West', 9602),
       (9604, 'UC004 Fremd', 'UC004 Ost', 9603)
ON CONFLICT DO NOTHING;

-- 9601 und 9602 mit Scores der MAG 9601; 9603 ohne Score
INSERT INTO rolle (id, name)
VALUES (9601, 'UC004 Entwicklung'),
       (9602, 'UC004 Test'),
       (9603, 'UC004 Ohne Scores')
ON CONFLICT DO NOTHING;

-- zwei Versionen für Rolle 9601: BR-001 mittelt unabhängig vom Fragenkatalog. Alle archiviert, damit die Rollen
-- in keinem anderen Use Case als Rolle mit freigegebenem Fragenkatalog erscheinen und Tests, die freigegebene
-- Kataloge löschen, nicht an den Scores hängen bleiben.
INSERT INTO fragenkatalog (id, rolle_id, version, status, erstellt_am, freigegeben_am, freigegeben_von_id)
VALUES (9601, 9601, 1, 'Archiviert', TIMESTAMP '2026-01-01 10:00:00', TIMESTAMP '2026-01-02 10:00:00', 9601),
       (9602, 9601, 2, 'Archiviert', TIMESTAMP '2026-03-01 10:00:00', TIMESTAMP '2026-03-02 10:00:00', 9601),
       (9603, 9602, 1, 'Archiviert', TIMESTAMP '2026-01-01 10:00:00', TIMESTAMP '2026-01-02 10:00:00', 9601),
       (9604, 9603, 1, 'Archiviert', TIMESTAMP '2026-01-01 10:00:00', TIMESTAMP '2026-01-02 10:00:00', 9601)
ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status;

-- MAG 9601, Rolle 9601:
--   Handlungsfeld 1: 3.0, 4.0 (Version 2), 2.5  -> Mittelwert 3.1666…, gerundet 3.2
--   Handlungsfeld 2: 2.0, 2.5                    -> 2.25, kaufmännisch gerundet 2.3
--   Handlungsfeld 3: 4.5                         -> 4.5
--   Handlungsfeld 4: kein Score                  -> kein Wert (BR-006)
--   Handlungsfeld 5: 1.0                         -> 1.0
--   Handlungsfeld 6: 5.0                         -> 5.0
-- MAG 9601, Rolle 9602: nur Handlungsfeld 1 mit 1.0 (BR-003, ein einziges Assessment)
-- MAG 9604 (fremd), Rolle 9601: Handlungsfeld 1 mit 0.0, fließt nicht in MAG 9601 ein
INSERT INTO score (id, mag_id, rolle_id, handlungsfeld_id, fragenkatalog_id, wert, erfasst_am)
VALUES (9601, 9601, 9601, (SELECT id FROM handlungsfeld WHERE nummer = 1), 9601, 3.0, DATE '2026-02-01'),
       (9602, 9601, 9601, (SELECT id FROM handlungsfeld WHERE nummer = 1), 9602, 4.0, DATE '2026-04-01'),
       (9603, 9601, 9601, (SELECT id FROM handlungsfeld WHERE nummer = 1), 9601, 2.5, DATE '2026-02-02'),
       (9604, 9601, 9601, (SELECT id FROM handlungsfeld WHERE nummer = 2), 9601, 2.0, DATE '2026-02-01'),
       (9605, 9601, 9601, (SELECT id FROM handlungsfeld WHERE nummer = 2), 9602, 2.5, DATE '2026-04-01'),
       (9606, 9601, 9601, (SELECT id FROM handlungsfeld WHERE nummer = 3), 9602, 4.5, DATE '2026-04-01'),
       (9607, 9601, 9601, (SELECT id FROM handlungsfeld WHERE nummer = 5), 9602, 1.0, DATE '2026-04-01'),
       (9608, 9601, 9601, (SELECT id FROM handlungsfeld WHERE nummer = 6), 9602, 5.0, DATE '2026-04-01'),
       (9609, 9601, 9602, (SELECT id FROM handlungsfeld WHERE nummer = 1), 9603, 1.0, DATE '2026-04-01'),
       (9610, 9604, 9601, (SELECT id FROM handlungsfeld WHERE nummer = 1), 9602, 0.0, DATE '2026-04-01')
ON CONFLICT DO NOTHING;
