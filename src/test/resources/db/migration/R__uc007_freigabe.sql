-- Testdaten für UC-007 Fragenkatalog freigeben (wiederholbar, ändert keine Produktionsdaten).
-- Feste Ids ab 9501; die Sequenzen der Produktionstabellen vergeben sie nicht.
-- Kein Administrator (den legt der Start der Anwendung an); die Tests legen ihn selbst an.
-- Freigegeben von Benutzer 9301 (Teamleitung aus R__uc002_assessment.sql, läuft vorher); das laufende Assessment
-- für BR-006 legt der Test in seiner Transaktion an.

-- Rolle mit vollständigem Entwurf ohne freigegebene Version, Rolle mit freigegebener Version 1 und vollständigem
-- Entwurf Version 2, Rolle mit unvollständigem Entwurf, Rolle nur mit freigegebener Version (kein Entwurf)
INSERT INTO rolle (id, name)
VALUES (9501, 'UC007 Erstfreigabe'),
       (9502, 'UC007 Nachfolger'),
       (9503, 'UC007 Unvollständig'),
       (9504, 'UC007 Ohne Entwurf')
ON CONFLICT DO NOTHING;

INSERT INTO fragenkatalog (id, rolle_id, version, status, erstellt_am, freigegeben_am, freigegeben_von_id)
VALUES (9501, 9501, 1, 'Entwurf', TIMESTAMP '2026-03-01 10:00:00', NULL, NULL),
       (9502, 9502, 1, 'Freigegeben', TIMESTAMP '2026-01-01 10:00:00', TIMESTAMP '2026-01-02 10:00:00', 9301),
       (9503, 9502, 2, 'Entwurf', TIMESTAMP '2026-03-01 10:00:00', NULL, NULL),
       (9504, 9503, 1, 'Entwurf', TIMESTAMP '2026-03-01 10:00:00', NULL, NULL),
       (9505, 9504, 1, 'Freigegeben', TIMESTAMP '2026-01-01 10:00:00', TIMESTAMP '2026-01-02 10:00:00', 9301)
ON CONFLICT DO NOTHING;

-- vollständige Kataloge 9501, 9502, 9503, 9505: je eine Frage in jedem der sechs Handlungsfelder;
-- Frage-Id = Katalog-Id * 10 + Nummer des Handlungsfelds
INSERT INTO frage (id, fragenkatalog_id, handlungsfeld_id, text, position)
SELECT k.id * 10 + h.nummer, k.id, h.id, 'UC007 Frage ' || h.nummer || ' in Katalog ' || k.id, h.nummer
FROM (VALUES (9501), (9502), (9503), (9505)) AS k(id)
         CROSS JOIN handlungsfeld h
ON CONFLICT DO NOTHING;

-- unvollständiger Entwurf 9504: Handlungsfelder 1 bis 4 mit je einer Frage, 5 und 6 ohne Frage (BR-001)
INSERT INTO frage (id, fragenkatalog_id, handlungsfeld_id, text, position)
SELECT 9504 * 10 + h.nummer, 9504, h.id, 'UC007 Frage ' || h.nummer || ' in Katalog 9504', h.nummer
FROM handlungsfeld h
WHERE h.nummer <= 4
ON CONFLICT DO NOTHING;

-- jede Frage mit einer Vorgabe je Stufe 0 bis 5; Ausnahme: Frage 2 des Entwurfs 9504 ohne Vorgabe für Stufe 5
INSERT INTO stufenvorgabe (id, frage_id, stufe, beschreibung)
SELECT f.id * 10 + s, f.id, s, 'UC007 Vorgabe ' || s || ' zu Frage ' || f.id
FROM frage f
         CROSS JOIN generate_series(0, 5) AS s
WHERE f.fragenkatalog_id IN (9501, 9502, 9503, 9504, 9505)
  AND NOT (f.id = 95042 AND s = 5)
ON CONFLICT DO NOTHING;
