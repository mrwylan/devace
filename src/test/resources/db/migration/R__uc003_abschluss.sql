-- Testdaten für UC-003 Assessment abschließen und Textreport herunterladen (wiederholbar, ändert keine Produktionsdaten).
-- Feste Ids ab 9401; Einladungen, Assessments und Antworten legen die Tests selbst an.

INSERT INTO benutzer (id, benutzername, passwort_hash, berechtigung)
VALUES (9401, 'uc003-teamleitung', 'unused', 'Teamleitung')
ON CONFLICT DO NOTHING;

INSERT INTO mag (id, scrum_team, linienteam, teamleitung_id)
VALUES (9401, 'UC003 Beta', 'UC003 Süd', 9401)
ON CONFLICT DO NOTHING;

INSERT INTO rolle (id, name)
VALUES (9401, 'UC003 Entwicklung')
ON CONFLICT DO NOTHING;

INSERT INTO fragenkatalog (id, rolle_id, version, status, erstellt_am, freigegeben_am, freigegeben_von_id)
VALUES (9401, 9401, 1, 'Freigegeben', TIMESTAMP '2026-01-01 08:00:00', TIMESTAMP '2026-01-01 09:00:00', 9401)
ON CONFLICT DO NOTHING;

-- vier Fragen im Handlungsfeld 1 (Score mit Rundung, BR-001), eine im Handlungsfeld 2; die übrigen vier
-- Handlungsfelder haben keine Frage und damit keinen Score
INSERT INTO frage (id, fragenkatalog_id, handlungsfeld_id, text, position)
VALUES (9401, 9401, (SELECT id FROM handlungsfeld WHERE nummer = 1), 'UC003 Wie verstehen Sie die Abläufe im Geschäft?', 1),
       (9402, 9401, (SELECT id FROM handlungsfeld WHERE nummer = 1), 'UC003 Wie klären Sie eine Anforderung?', 2),
       (9403, 9401, (SELECT id FROM handlungsfeld WHERE nummer = 1), 'UC003 Wie messen Sie den Nutzen?', 3),
       (9404, 9401, (SELECT id FROM handlungsfeld WHERE nummer = 1), 'UC003 Wie sprechen Sie mit dem Fachbereich?', 4),
       (9405, 9401, (SELECT id FROM handlungsfeld WHERE nummer = 2), 'UC003 Wie begründen Sie eine Architekturentscheidung?', 5)
ON CONFLICT DO NOTHING;

INSERT INTO stufenvorgabe (frage_id, stufe, beschreibung)
SELECT f, s, 'UC003 Vorgabe ' || f || '.' || s
FROM (VALUES (9401), (9402), (9403), (9404), (9405)) AS fragen(f), generate_series(0, 5) AS s
ON CONFLICT DO NOTHING;
