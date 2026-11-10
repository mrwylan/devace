-- Testdaten für UC-001 Einladung erstellen und versenden (wiederholbar, ändert keine Produktionsdaten).
-- Feste Ids ab 9201; die Sequenzen der Produktionstabellen vergeben sie nicht.
-- Kein Administrator (den legt der Start der Anwendung an). Das Kennwort der Teamleitung ist ein Platzhalter nur
-- für die Browser-Tests.

INSERT INTO benutzer (id, benutzername, passwort_hash, berechtigung)
VALUES (9201, 'uc001-teamleitung', '{noop}uc001-test', 'Teamleitung'),
       (9202, 'uc001-andere-teamleitung', 'unused', 'Teamleitung'),
       (9203, 'uc001-ohne-mag', 'unused', 'Teamleitung')
ON CONFLICT DO NOTHING;

-- eine MAG der Teamleitung, eine MAG einer anderen Teamleitung
INSERT INTO mag (id, scrum_team, linienteam, teamleitung_id)
VALUES (9201, 'UC001 Alpha', 'UC001 Nord', 9201),
       (9202, 'UC001 Beta', 'UC001 Süd', 9202)
ON CONFLICT DO NOTHING;

-- eine Rolle mit freigegebenem Fragenkatalog, eine nur mit Entwurf
INSERT INTO rolle (id, name)
VALUES (9201, 'UC001 Entwicklung'),
       (9202, 'UC001 Ohne Freigabe')
ON CONFLICT DO NOTHING;

INSERT INTO fragenkatalog (id, rolle_id, version, status, erstellt_am, freigegeben_am, freigegeben_von_id)
VALUES (9201, 9201, 1, 'Freigegeben', TIMESTAMP '2026-01-01 10:00:00', TIMESTAMP '2026-01-02 10:00:00', 9201),
       (9202, 9202, 1, 'Entwurf', TIMESTAMP '2026-02-01 10:00:00', NULL, NULL)
ON CONFLICT DO NOTHING;
