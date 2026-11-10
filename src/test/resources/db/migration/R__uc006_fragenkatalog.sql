-- Testdaten für UC-006 Fragenkatalog und Entscheidungsmodell erstellen (wiederholbar, ändert keine Produktionsdaten).
-- Feste Ids ab 9101; die Sequenzen der Produktionstabellen vergeben sie nicht.
-- Kein Administrator (den legt der Start der Anwendung an); eigener Benutzer, weil die Datei vor R__uc008_* läuft und dessen Zeilen noch nicht kennt.

INSERT INTO benutzer (id, benutzername, passwort_hash, berechtigung)
VALUES (9101, 'uc006-teamleitung', 'unused', 'Teamleitung')
ON CONFLICT DO NOTHING;

-- Rolle ohne Fragenkatalog, Rolle mit freigegebenem Katalog (Version 2), Rolle mit Entwurf (Version 1)
INSERT INTO rolle (id, name)
VALUES (9101, 'UC006 Leer'),
       (9102, 'UC006 Freigegeben'),
       (9103, 'UC006 Entwurf')
ON CONFLICT DO NOTHING;

INSERT INTO fragenkatalog (id, rolle_id, version, status, erstellt_am, freigegeben_am, freigegeben_von_id)
VALUES (9101, 9102, 2, 'Freigegeben', TIMESTAMP '2026-01-01 10:00:00', TIMESTAMP '2026-01-02 10:00:00', 9101),
       (9102, 9103, 1, 'Entwurf', TIMESTAMP '2026-02-01 10:00:00', NULL, NULL)
ON CONFLICT DO NOTHING;

INSERT INTO frage (id, fragenkatalog_id, handlungsfeld_id, text, position)
VALUES (9101, 9101, (SELECT id FROM handlungsfeld WHERE nummer = 1), 'UC006 Wie gehen Sie mit Anforderungen um?', 1),
       (9102, 9101, (SELECT id FROM handlungsfeld WHERE nummer = 2), 'UC006 Wie begründen Sie eine Entscheidung?', 2),
       (9103, 9102, (SELECT id FROM handlungsfeld WHERE nummer = 3), 'UC006 Wie testen Sie im Alltag?', 1)
ON CONFLICT DO NOTHING;

INSERT INTO stufenvorgabe (id, frage_id, stufe, beschreibung)
SELECT f.id * 10 + s, f.id, s, 'Vorgabe ' || s || ' zu Frage ' || f.id
FROM frage f
         CROSS JOIN generate_series(0, 5) AS s
WHERE f.id IN (9101, 9102, 9103)
ON CONFLICT DO NOTHING;
