-- Testdaten für UC-005 Radar-Charts für die Schulungsplanung auswerten (wiederholbar, ändert keine Produktionsdaten).
-- Feste Ids ab 9701; die Sequenzen der Produktionstabellen vergeben sie nicht.
-- uc005-schulung meldet sich im Browser-Test mit dem Testpasswort an, kein echtes Passwort.

INSERT INTO benutzer (id, benutzername, passwort_hash, berechtigung)
VALUES (9701, 'uc005-schulung', '{noop}uc005-test', 'Schulungsverantwortliche')
ON CONFLICT DO NOTHING;

-- Die MAGs haben keine Teamleitung: BR-002 zeigt der Schulungsverantwortlichen alle MAGs, unabhängig von einer
-- Zuordnung. Keine weiteren Teamleitungs-Konten, damit die Auswahllisten anderer Use Cases nicht länger werden.
INSERT INTO mag (id, scrum_team, linienteam, teamleitung_id)
VALUES (9701, 'UC005 Alpha', 'UC005 Nord', NULL),
       (9702, 'UC005 Beta', 'UC005 Süd', NULL),
       (9703, 'UC005 Gamma', 'UC005 West', NULL)
ON CONFLICT DO NOTHING;

-- 9701 und 9702 mit Scores; 9703 ohne Score (BR-004)
INSERT INTO rolle (id, name)
VALUES (9701, 'UC005 Entwicklung'),
       (9702, 'UC005 Test'),
       (9703, 'UC005 Ohne Scores')
ON CONFLICT DO NOTHING;

-- archiviert: die Rollen erscheinen in keinem anderen Use Case als Rolle mit freigegebenem Fragenkatalog
INSERT INTO fragenkatalog (id, rolle_id, version, status, erstellt_am, freigegeben_am, freigegeben_von_id)
VALUES (9701, 9701, 1, 'Archiviert', TIMESTAMP '2026-01-01 10:00:00', TIMESTAMP '2026-01-02 10:00:00', 9701),
       (9702, 9702, 1, 'Archiviert', TIMESTAMP '2026-01-01 10:00:00', TIMESTAMP '2026-01-02 10:00:00', 9701),
       (9703, 9703, 1, 'Archiviert', TIMESTAMP '2026-01-01 10:00:00', TIMESTAMP '2026-01-02 10:00:00', 9701)
ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status;

-- Rolle 9701:
--   MAG 9701: Handlungsfeld 1: 3.0, 4.0 -> 3.5; Handlungsfeld 2: 2.0, 2.5 -> 2.25, kaufmännisch gerundet 2.3;
--             Handlungsfeld 3 bis 6: kein Score (BR-006)
--   MAG 9702: nur Handlungsfeld 1 mit 1.0 (BR-003)
--   MAG 9703: kein Score der Rolle 9701 (erscheint nicht, BR-004)
-- Rolle 9702: MAG 9703 mit Handlungsfeld 1 = 5.0 (A2)
INSERT INTO score (id, mag_id, rolle_id, handlungsfeld_id, fragenkatalog_id, wert, erfasst_am)
VALUES (9701, 9701, 9701, (SELECT id FROM handlungsfeld WHERE nummer = 1), 9701, 3.0, DATE '2026-04-01'),
       (9702, 9701, 9701, (SELECT id FROM handlungsfeld WHERE nummer = 1), 9701, 4.0, DATE '2026-04-02'),
       (9703, 9701, 9701, (SELECT id FROM handlungsfeld WHERE nummer = 2), 9701, 2.0, DATE '2026-04-01'),
       (9704, 9701, 9701, (SELECT id FROM handlungsfeld WHERE nummer = 2), 9701, 2.5, DATE '2026-04-02'),
       (9705, 9702, 9701, (SELECT id FROM handlungsfeld WHERE nummer = 1), 9701, 1.0, DATE '2026-04-01'),
       (9706, 9703, 9702, (SELECT id FROM handlungsfeld WHERE nummer = 1), 9702, 5.0, DATE '2026-04-01')
ON CONFLICT DO NOTHING;
