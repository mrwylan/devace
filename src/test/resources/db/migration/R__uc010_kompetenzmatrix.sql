-- Testdaten für UC-010 Kompetenzmatrix-Zuordnung dokumentieren (wiederholbar, ändert keine Produktionsdaten).
-- Feste Ids ab 10001; die Sequenz der Produktionstabelle vergibt sie nicht. Die Spalten beginnen mit „UC010 “,
-- Spalten, die Tests anlegen, mit „UC010T “.

-- 10001: Handlungsfeld 2 mit Begründung; 10002: Handlungsfeld 5 ohne Begründung (A2 beim Ändern);
-- 10003: zum Ändern; 10004: zum Entfernen
INSERT INTO matrix_zuordnung (id, handlungsfeld_id, matrix_spalte, begruendung)
VALUES (10001, (SELECT id FROM handlungsfeld WHERE nummer = 2), 'UC010 Docker', 'UC010 Container im Alltag'),
       (10002, (SELECT id FROM handlungsfeld WHERE nummer = 5), 'UC010 Scrum', NULL),
       (10003, (SELECT id FROM handlungsfeld WHERE nummer = 1), 'UC010 Zum Ändern', 'UC010 Alte Begründung'),
       (10004, (SELECT id FROM handlungsfeld WHERE nummer = 3), 'UC010 Zum Entfernen', NULL)
ON CONFLICT DO NOTHING;
