-- =====================================================================
--  Department Budget Management System - database setup
--  Run this whole file once in MySQL Workbench (logged in as root).
--  It is safe to run again: nothing is deleted.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS budget_db;
USE budget_db;

-- The app logs in with this user, not root.
-- If you change the password here, change it in application.properties too.
CREATE USER IF NOT EXISTS 'budget_app'@'localhost' IDENTIFIED BY 'budget123';
GRANT SELECT, INSERT, UPDATE, DELETE ON budget_db.* TO 'budget_app'@'localhost';
FLUSH PRIVILEGES;

-- ---------- Tables ----------

CREATE TABLE IF NOT EXISTS staff (
    staff_id      INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS budget_years (
    budget_year_id INT AUTO_INCREMENT PRIMARY KEY,
    academic_year  VARCHAR(9)  NOT NULL UNIQUE,
    status         VARCHAR(10) NOT NULL DEFAULT 'DRAFT',
    fixed_on       DATE,
    staff_id       INT NOT NULL,
    FOREIGN KEY (staff_id) REFERENCES staff(staff_id),
    CHECK (status IN ('DRAFT', 'FIXED'))
);

CREATE TABLE IF NOT EXISTS categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    code        CHAR(1)     NOT NULL UNIQUE,
    name        VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS allocations (
    allocation_id  INT AUTO_INCREMENT PRIMARY KEY,
    budget_year_id INT NOT NULL,
    category_id    INT NOT NULL,
    amount         DECIMAL(12,2) NOT NULL DEFAULT 0,
    FOREIGN KEY (budget_year_id) REFERENCES budget_years(budget_year_id),
    FOREIGN KEY (category_id)    REFERENCES categories(category_id),
    UNIQUE (budget_year_id, category_id),
    CHECK (amount >= 0)
);

CREATE TABLE IF NOT EXISTS allocation_history (
    history_id    INT AUTO_INCREMENT PRIMARY KEY,
    allocation_id INT NOT NULL,
    old_amount    DECIMAL(12,2) NOT NULL,
    new_amount    DECIMAL(12,2) NOT NULL,
    reason        VARCHAR(255)  NOT NULL,
    changed_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (allocation_id) REFERENCES allocations(allocation_id)
);

CREATE TABLE IF NOT EXISTS expense_types (
    type_id INT AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(50) NOT NULL UNIQUE
);

-- Which categories each expense type is allowed under (many-to-many)
CREATE TABLE IF NOT EXISTS expense_type_categories (
    type_id     INT NOT NULL,
    category_id INT NOT NULL,
    PRIMARY KEY (type_id, category_id),
    FOREIGN KEY (type_id)     REFERENCES expense_types(type_id),
    FOREIGN KEY (category_id) REFERENCES categories(category_id)
);

-- Both RecurringExpense and NonRecurringExpense are stored here;
-- expense_kind says which one each row is.
CREATE TABLE IF NOT EXISTS expenses (
    expense_id     INT AUTO_INCREMENT PRIMARY KEY,
    allocation_id  INT NOT NULL,
    type_id        INT NOT NULL,
    expense_kind   VARCHAR(15) NOT NULL,
    frequency      VARCHAR(15),
    expense_date   DATE NOT NULL,
    amount         DECIMAL(12,2) NOT NULL,
    description    VARCHAR(255)  NOT NULL,
    receipt_number VARCHAR(50)   NOT NULL,
    receipt_file   VARCHAR(255),
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME,
    FOREIGN KEY (allocation_id) REFERENCES allocations(allocation_id),
    FOREIGN KEY (type_id)       REFERENCES expense_types(type_id),
    CHECK (amount > 0),
    CHECK (expense_kind IN ('RECURRING', 'NON_RECURRING')),
    INDEX idx_expense_date (expense_date),
    INDEX idx_receipt_number (receipt_number)
);

-- ---------- Starting data ----------

INSERT IGNORE INTO categories (code, name) VALUES
('A', 'Laboratory Equipment'),
('B', 'Software'),
('C', 'Laboratory Consumables'),
('D', 'Maintenance & Spare'),
('E', 'R&D'),
('F', 'Training & Travel'),
('G', 'Miscellaneous');

INSERT IGNORE INTO expense_types (name) VALUES
('Lab instrument'),
('Software licence'),
('Spare part'),
('Chemicals and consumables'),
('Equipment repair / AMC'),
('Research material'),
('Workshop / conference fee'),
('Travel'),
('Stationery'),
('Other');

-- Allowed categories for each expense type. Adjust with the staff if needed.
INSERT IGNORE INTO expense_type_categories (type_id, category_id)
SELECT t.type_id, c.category_id
FROM expense_types t JOIN categories c
WHERE (t.name, c.code) IN (
    ('Lab instrument', 'A'),
    ('Software licence', 'A'),
    ('Software licence', 'B'),
    ('Spare part', 'C'),
    ('Spare part', 'D'),
    ('Chemicals and consumables', 'C'),
    ('Equipment repair / AMC', 'D'),
    ('Research material', 'E'),
    ('Research material', 'C'),
    ('Workshop / conference fee', 'E'),
    ('Workshop / conference fee', 'F'),
    ('Travel', 'F'),
    ('Stationery', 'G'),
    ('Other', 'G')
);

-- The staff login is created by the Java app the first time it starts
-- (the password has to be BCrypt-hashed first).
