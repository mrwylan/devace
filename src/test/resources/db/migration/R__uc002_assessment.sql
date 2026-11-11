-- Testdaten für UC-002 Assessment durchführen (wiederholbar, ändert keine Produktionsdaten).
-- Feste Ids ab 9301; die Einladungen legen die Tests selbst an, weil der Start eine Einladung einlöst.

INSERT INTO benutzer (id, benutzername, passwort_hash, berechtigung)
VALUES (9301, 'uc002-teamleitung', 'unused', 'Teamleitung')
ON CONFLICT DO NOTHING;

INSERT INTO mag (id, scrum_team, linienteam, teamleitung_id)
VALUES (9301, 'UC002 Alpha', 'UC002 Nord', 9301)
ON CONFLICT DO NOTHING;

INSERT INTO rolle (id, name)
VALUES (9301, 'UC002 Entwicklung')
ON CONFLICT DO NOTHING;

INSERT INTO fragenkatalog (id, rolle_id, version, status, erstellt_am, freigegeben_am, freigegeben_von_id)
VALUES (9301, 9301, 1, 'Freigegeben', TIMESTAMP '2026-01-01 08:00:00', TIMESTAMP '2026-01-01 09:00:00', 9301)
ON CONFLICT DO NOTHING;

-- zwei Fragen; die Position legt die Reihenfolge fest (BR-003), absichtlich nicht in der Reihenfolge der Ids
INSERT INTO frage (id, fragenkatalog_id, handlungsfeld_id, text, position)
VALUES (9302, 9301, (SELECT min(id) FROM handlungsfeld), 'UC002 Wie gehen Sie mit technischen Schulden um?', 2),
       (9301, 9301, (SELECT min(id) FROM handlungsfeld), 'UC002 Wie begründen Sie eine Architekturentscheidung?', 1)
ON CONFLICT DO NOTHING;

INSERT INTO stufenvorgabe (frage_id, stufe, beschreibung)
SELECT f, s, 'UC002 Vorgabe ' || f || '.' || s
FROM (VALUES (9301), (9302)) AS fragen(f), generate_series(0, 5) AS s
ON CONFLICT DO NOTHING;

INSERT INTO starthinweis (id, version, text, ki_modelle, gueltig_ab)
VALUES (9301, 9301, 'UC002 Nennen Sie keine Namen von Personen und keine Kundenprojekte. Es gelten die Firmenweisungen zum Umgang mit KI.',
        'UC002 Jev (Bewertung)', DATE '2000-01-01')
ON CONFLICT DO NOTHING;

-- ein Entwurf derselben Rolle mit einer eigenen Frage: er darf im Assessment nicht erscheinen (BR-003)
INSERT INTO fragenkatalog (id, rolle_id, version, status, erstellt_am)
VALUES (9302, 9301, 2, 'Entwurf', TIMESTAMP '2026-02-01 08:00:00')
ON CONFLICT DO NOTHING;

INSERT INTO frage (id, fragenkatalog_id, handlungsfeld_id, text, position)
VALUES (9303, 9302, (SELECT min(id) FROM handlungsfeld), 'UC002 Entwurfsfrage, nicht freigegeben', 1)
ON CONFLICT DO NOTHING;

INSERT INTO stufenvorgabe (frage_id, stufe, beschreibung)
SELECT 9303, s, 'UC002 Entwurfsvorgabe ' || s
FROM generate_series(0, 5) AS s
ON CONFLICT DO NOTHING;
