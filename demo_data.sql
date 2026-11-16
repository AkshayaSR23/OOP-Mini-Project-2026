-- =====================================================================
--  OPTIONAL demo data for testing and the viva.
--  Run AFTER schema.sql AND after starting the app once
--  (the app creates the staff login that this data needs).
--  Creates a FIXED 2026-27 budget with 22 expenses.
-- =====================================================================

USE budget_db;

SET @staff = (SELECT staff_id FROM staff ORDER BY staff_id LIMIT 1);

INSERT INTO budget_years (academic_year, status, fixed_on, staff_id)
VALUES ('2026-27', 'FIXED', '2026-04-10', @staff);
SET @year = LAST_INSERT_ID();

INSERT INTO allocations (budget_year_id, category_id, amount)
SELECT @year, category_id,
       CASE code WHEN 'A' THEN 600000 WHEN 'B' THEN 250000 WHEN 'C' THEN 150000
                 WHEN 'D' THEN 60000  WHEN 'E' THEN 300000 WHEN 'F' THEN 120000
                 ELSE 40000 END
FROM categories;

-- Shortcuts: allocation id for each category in this year
SET @A = (SELECT a.allocation_id FROM allocations a JOIN categories c ON a.category_id = c.category_id WHERE a.budget_year_id = @year AND c.code = 'A');
SET @B = (SELECT a.allocation_id FROM allocations a JOIN categories c ON a.category_id = c.category_id WHERE a.budget_year_id = @year AND c.code = 'B');
SET @C = (SELECT a.allocation_id FROM allocations a JOIN categories c ON a.category_id = c.category_id WHERE a.budget_year_id = @year AND c.code = 'C');
SET @D = (SELECT a.allocation_id FROM allocations a JOIN categories c ON a.category_id = c.category_id WHERE a.budget_year_id = @year AND c.code = 'D');
SET @E = (SELECT a.allocation_id FROM allocations a JOIN categories c ON a.category_id = c.category_id WHERE a.budget_year_id = @year AND c.code = 'E');
SET @F = (SELECT a.allocation_id FROM allocations a JOIN categories c ON a.category_id = c.category_id WHERE a.budget_year_id = @year AND c.code = 'F');
SET @G = (SELECT a.allocation_id FROM allocations a JOIN categories c ON a.category_id = c.category_id WHERE a.budget_year_id = @year AND c.code = 'G');

-- Shortcuts: expense type ids
SET @instrument = (SELECT type_id FROM expense_types WHERE name = 'Lab instrument');
SET @software   = (SELECT type_id FROM expense_types WHERE name = 'Software licence');
SET @spare      = (SELECT type_id FROM expense_types WHERE name = 'Spare part');
SET @chemicals  = (SELECT type_id FROM expense_types WHERE name = 'Chemicals and consumables');
SET @repair     = (SELECT type_id FROM expense_types WHERE name = 'Equipment repair / AMC');
SET @research   = (SELECT type_id FROM expense_types WHERE name = 'Research material');
SET @workshop   = (SELECT type_id FROM expense_types WHERE name = 'Workshop / conference fee');
SET @travel     = (SELECT type_id FROM expense_types WHERE name = 'Travel');
SET @stationery = (SELECT type_id FROM expense_types WHERE name = 'Stationery');
SET @other      = (SELECT type_id FROM expense_types WHERE name = 'Other');

INSERT INTO expenses (allocation_id, type_id, expense_kind, frequency, expense_date, amount, description, receipt_number) VALUES
-- A: Laboratory Equipment (about 71% used)
(@A, @instrument, 'NON_RECURRING', NULL, '2026-05-14', 185000.00, 'Digital storage oscilloscopes (2 units)', 'INV-A101'),
(@A, @instrument, 'NON_RECURRING', NULL, '2026-08-02', 142000.00, 'Function generators and power supplies', 'INV-A102'),
(@A, @instrument, 'NON_RECURRING', NULL, '2026-10-10',  98000.00, 'Microcontroller trainer kits (10)', 'INV-A103'),
-- B: Software (about 76% used, amber)
(@B, @software, 'RECURRING', 'YEARLY', '2026-04-20', 85000.00, 'MATLAB campus licence renewal', 'SW-2041'),
(@B, @software, 'RECURRING', 'YEARLY', '2026-06-05', 42000.00, 'Antivirus licences for lab PCs', 'SW-2077'),
(@B, @software, 'NON_RECURRING', NULL, '2026-09-18', 63000.00, 'CAD software seats (5)', 'SW-2110'),
-- C: Laboratory Consumables (about 46% used)
(@C, @chemicals, 'NON_RECURRING', NULL, '2026-04-28', 18500.00, 'Soldering consumables and flux', 'LC-301'),
(@C, @chemicals, 'NON_RECURRING', NULL, '2026-06-30', 22400.00, 'Breadboards, resistors and ICs', 'LC-318'),
(@C, @spare,     'NON_RECURRING', NULL, '2026-08-25', 12600.00, 'Replacement oscilloscope probes', 'LC-342'),
(@C, @chemicals, 'NON_RECURRING', NULL, '2026-10-22', 15800.00, 'Lab consumables restock', 'LC-377'),
-- D: Maintenance & Spare (about 92% used, red)
(@D, @repair, 'RECURRING', 'HALF_YEARLY', '2026-05-08', 24000.00, 'Projector AMC for April to September', 'MS-501'),
(@D, @spare,  'NON_RECURRING', NULL,      '2026-07-19', 11500.00, 'UPS battery replacement', 'MS-519'),
(@D, @repair, 'RECURRING', 'QUARTERLY',   '2026-09-03',  9800.00, 'Air conditioner servicing', 'MS-533'),
(@D, @spare,  'NON_RECURRING', NULL,      '2026-10-29',  9900.00, 'Network switch replacement', 'MS-548'),
-- E: R&D (about 21% used)
(@E, @research, 'NON_RECURRING', NULL, '2026-07-07', 46000.00, 'Sensors for IoT research project', 'RD-601'),
(@E, @workshop, 'NON_RECURRING', NULL, '2026-09-27', 18000.00, 'Research workshop registration', 'RD-622'),
-- F: Training & Travel (about 63% used)
(@F, @workshop, 'NON_RECURRING', NULL, '2026-06-12', 24000.00, 'Faculty development programme fees', 'TT-701'),
(@F, @travel,   'NON_RECURRING', NULL, '2026-08-16', 31500.00, 'Conference travel to Bengaluru', 'TT-715'),
(@F, @travel,   'NON_RECURRING', NULL, '2026-10-05', 19800.00, 'Industrial visit transport', 'TT-731'),
-- G: Miscellaneous (about 28% used)
(@G, @stationery, 'NON_RECURRING', NULL, '2026-05-22', 6400.00, 'Department stationery', 'MI-801'),
(@G, @other,      'NON_RECURRING', NULL, '2026-09-09', 4800.00, 'Seminar refreshments', 'MI-812'),
(@G, @stationery, 'NON_RECURRING', NULL, '2026-11-02', 2900.00, 'Printer paper and toner', 'MI-826');
