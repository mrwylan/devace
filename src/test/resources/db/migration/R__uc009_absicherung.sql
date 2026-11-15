-- Testdaten für UC-009 KI-Bewertung mit Testfällen absichern (wiederholbar, ändert keine Produktionsdaten).
-- Feste Ids ab 9801; die Sequenzen der Produktionstabellen vergeben sie nicht.
-- uc009-admin ist der angemeldete Benutzer der Browserless-Tests (@WithMockUser mit Rolle ADMINISTRATOR). Das Konto
-- trägt bewusst nicht die Berechtigung Administrator: sonst legte der Start der Anwendung in den Browser-Tests kein
-- Administratorkonto mehr an.

INSERT INTO benutzer (id, benutzername, passwort_hash, berechtigung)
VALUES (9801, 'uc009-admin', 'unused', 'Schulungsverantwortliche')
ON CONFLICT DO NOTHING;

-- 9801: freigegebene Version 1 und Entwurf 2; 9802: nur archiviert (BR-002); 9803: Entwurf ohne Testfall (A3);
-- 9804: nur freigegebene Version mit Testfall (BR-010, neue Version)
INSERT INTO rolle (id, name)
VALUES (9801, 'UC009 Entwicklung'),
       (9802, 'UC009 Archiv'),
       (9803, 'UC009 Ohne Testfall'),
       (9804, 'UC009 Neue Version')
ON CONFLICT DO NOTHING;

INSERT INTO fragenkatalog (id, rolle_id, version, status, erstellt_am, freigegeben_am, freigegeben_von_id)
VALUES (9801, 9801, 1, 'Freigegeben', TIMESTAMP '2026-01-01 10:00:00', TIMESTAMP '2026-01-02 10:00:00', 9801),
       (9802, 9801, 2, 'Entwurf', TIMESTAMP '2026-02-01 10:00:00', NULL, NULL),
       (9803, 9802, 1, 'Archiviert', TIMESTAMP '2026-01-01 10:00:00', TIMESTAMP '2026-01-02 10:00:00', 9801),
       (9804, 9803, 1, 'Entwurf', TIMESTAMP '2026-02-01 10:00:00', NULL, NULL),
       (9805, 9804, 1, 'Freigegeben', TIMESTAMP '2026-01-01 10:00:00', TIMESTAMP '2026-01-02 10:00:00', 9801)
ON CONFLICT DO NOTHING;

INSERT INTO frage (id, fragenkatalog_id, handlungsfeld_id, text, position)
VALUES (9801, 9801, (SELECT id FROM handlungsfeld WHERE nummer = 1), 'UC009 Frage A?', 1),
       (9802, 9801, (SELECT id FROM handlungsfeld WHERE nummer = 2), 'UC009 Frage B?', 2),
       (9803, 9802, (SELECT id FROM handlungsfeld WHERE nummer = 1), 'UC009 Frage A?', 1),
       (9804, 9802, (SELECT id FROM handlungsfeld WHERE nummer = 2), 'UC009 Frage B?', 2),
       (9805, 9803, (SELECT id FROM handlungsfeld WHERE nummer = 1), 'UC009 Archivfrage?', 1),
       (9806, 9804, (SELECT id FROM handlungsfeld WHERE nummer = 1), 'UC009 Frage ohne Testfall?', 1),
       (9807, 9805, (SELECT id FROM handlungsfeld WHERE nummer = 1), 'UC009 Frage der neuen Version?', 1)
ON CONFLICT DO NOTHING;

INSERT INTO stufenvorgabe (frage_id, stufe, beschreibung)
SELECT f.id, s.stufe, 'UC009 Vorgabe ' || s.stufe
FROM frage f
         CROSS JOIN generate_series(0, 5) AS s(stufe)
WHERE f.id BETWEEN 9801 AND 9807
  AND NOT EXISTS (SELECT 1 FROM stufenvorgabe v WHERE v.frage_id = f.id AND v.stufe = s.stufe);

-- Entwurf 9802: drei Testfälle in der Reihenfolge der Ausführung (Frage A: 9801, 9802; Frage B: 9803)
INSERT INTO testfall (id, frage_id, antworttext, erwartete_stufe)
VALUES (9801, 9803, 'UC009 Antwort eins', 3),
       (9802, 9803, 'UC009 Antwort zwei', 1),
       (9803, 9804, 'UC009 Antwort drei', 4),
       (9804, 9801, 'UC009 Antwort der freigegebenen Version', 2),
       (9805, 9807, 'UC009 Antwort der neuen Version', 5)
ON CONFLICT DO NOTHING;

-- Kein Testlauf in den Testdaten: Tests anderer Use Cases löschen alle Entwürfe (UC-007) oder alle freigegebenen
-- Kataloge (UC-001), und ein Testlauf hält seinen Fragenkatalog fest (BR-007). Die Tests legen frühere Testläufe in
-- ihrer eigenen Transaktion an.
