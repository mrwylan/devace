-- Testdaten für UC-008 MAGs und Rollen verwalten (wiederholbar, ändert keine Produktionsdaten).
-- Feste Ids ab 9001; die Sequenzen der Produktionstabellen vergeben sie nicht.
-- Die Passwort-Hashes sind Platzhalter ohne Bedeutung; die Tests melden sich nicht mit ihnen an.

INSERT INTO benutzer (id, benutzername, passwort_hash, berechtigung)
VALUES (9001, 'uc008-teamleitung-a', 'unused', 'Teamleitung'),
       (9002, 'uc008-teamleitung-b', 'unused', 'Teamleitung'),
       (9003, 'uc008-administrator', 'unused', 'Administrator')
ON CONFLICT DO NOTHING;

INSERT INTO mag (id, scrum_team, linienteam, teamleitung_id)
VALUES (9001, 'UC008 Alpha', 'UC008 Nord', 9001),
       (9002, 'UC008 Beta', 'UC008 Süd', NULL)
ON CONFLICT DO NOTHING;

INSERT INTO rolle (id, name)
VALUES (9001, 'UC008 Entwickler'),
       (9002, 'UC008 Umbenennbar')
ON CONFLICT DO NOTHING;

INSERT INTO fragenkatalog (id, rolle_id, version, status, erstellt_am, freigegeben_am, freigegeben_von_id)
VALUES (9001, 9001, 1, 'Freigegeben', TIMESTAMP '2026-01-01 10:00:00', TIMESTAMP '2026-01-02 10:00:00', 9003),
       (9002, 9001, 2, 'Entwurf', TIMESTAMP '2026-02-01 10:00:00', NULL, NULL),
       (9003, 9002, 1, 'Entwurf', TIMESTAMP '2026-02-01 10:00:00', NULL, NULL)
ON CONFLICT DO NOTHING;
