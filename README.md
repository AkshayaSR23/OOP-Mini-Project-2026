# Department Budget Management System

A web app that replaces the department's Excel budget sheet.
One staff member plans the yearly budget for categories A to G, fixes it after the
Principal approves it, records every bill, and gets reports in Excel or PDF.

Built with **Java (Spring Boot)**, **MySQL**, and plain **HTML, CSS and JavaScript**.

---

## 1. What you need on the laptop

| Tool | Notes |
| --- | --- |
| **JDK 17 or newer** | Check with `java -version` |
| **MySQL Server 8** + **MySQL Workbench** | Remember the root password you set while installing |
| **IntelliJ IDEA Community** | It includes Maven, so nothing else is needed |
| Internet (first run only) | IntelliJ downloads the libraries once |

---

## 2. Set up the database (once)

1. Open **MySQL Workbench** and connect as `root`.
2. **File → Open SQL Script** → choose `schema.sql` from this folder.
3. Click the **lightning bolt** (Execute). You should see green ticks.

This creates the `budget_db` database, the 8 tables, the 7 categories, the expense types,
and a MySQL user `budget_app` (password `budget123`) that the app uses.

---

## 3. Run the app

1. Open **IntelliJ IDEA** → **File → Open** → choose this folder (the one with `pom.xml`) → **Open as Project**.
2. Wait until the progress bar at the bottom finishes (it downloads the libraries the first time; this can take a few minutes).
3. Open `src/main/java/com/dept/budget/BudgetApplication.java`.
4. Click the **green ▶ arrow** next to `public class BudgetApplication` → **Run**.
5. Wait for `Started BudgetApplication` in the console at the bottom.
6. Open **http://localhost:8080** in Chrome or Edge.

**Login:** username `budgetstaff`, password `admin123`

To stop the app, click the red square in IntelliJ.

---

## 4. Optional: load demo data

For testing and the viva, you can load a ready-made 2026-27 budget with 22 expenses
(one category nearly full, a few barely used):

1. Start the app **once** (step 3), so the staff login exists.
2. In MySQL Workbench, open and run `demo_data.sql`.
3. Refresh the browser.

Run it only once. To start again from an empty database, run `DROP DATABASE budget_db;`
in Workbench, then run `schema.sql` again and restart the app.

---

## 5. Common problems

| Message | Fix |
| --- | --- |
| `Access denied for user 'budget_app'` | `schema.sql` was not run, or not fully. Run it again in Workbench. |
| `Communications link failure` | MySQL is not running. Start the MySQL service (Windows: Services → MySQL80 → Start). |
| `Port 8080 was already in use` | Another program uses port 8080. Close it, or change `server.port` in `application.properties`. |
| Red errors in IntelliJ / `java: release version 17 not supported` | **File → Project Structure → Project → SDK**: choose JDK 17 or newer. |
| Libraries not found (red imports) | Right-click `pom.xml` → **Maven → Reload project**. |
| Page shows "Please log in again" | The session ended (60 minutes idle). Log in again. |

---

## 6. How to use it

1. **Budget Proposal** → **+ New Proposal** → enter the academic year (e.g. `2026-27`) →
   enter the estimated amount for each category → **Save draft**. Download it as Excel or PDF for the Principal.
2. After the Principal approves it → **Fix proposal**. The amounts are now locked.
3. **Expenses** → **+ Add Expense** → choose the expense type and amount. The panel shows
   which categories can take the bill and recommends one. Save. Use **Edit** to correct an expense.
4. **Dashboard** shows each category's spending. Click a category to open its report.
5. **Reports** → choose dates (and a category) → **Show report**, or download Excel / PDF.
6. If money must be moved later, use **Move funds** on the Budget Proposal page. Every move is recorded.

---

## 7. Project structure

```
budget-management-system/
├── README.md, schema.sql, demo_data.sql, pom.xml
└── src/main/
    ├── java/com/dept/budget/
    │   ├── BudgetApplication.java   starts the app, creates the login, checks login on every /api call
    │   ├── entity/       9 classes from the UML diagram (the data)
    │   ├── repository/   7 interfaces that talk to MySQL
    │   ├── service/      the logic: BudgetService, ExpenseService, ReportService,
    │   │                 Report + PeriodReport + CategoryReport, ExportUtil
    │   ├── controller/   the URLs the pages call, + ExpenseRequest
    │   └── exception/    custom exceptions + one error handler
    └── resources/
        ├── application.properties   settings
        └── static/                  the 5 pages, style.css, script.js
```

| Member | Owns |
| --- | --- |
| Member 1 | Login, Budget Proposal, moving funds: `Staff`, `BudgetYear`, `Allocation`, `AllocationHistory`, `BudgetService`, `BudgetController`, `exception/`, `index.html`, `proposal.html` |
| Member 2 | Expenses: `ExpenseType`, `Expense`, `RecurringExpense`, `NonRecurringExpense`, `ExpenseService`, `ExpenseController`, `ExpenseRequest`, `expenses.html` |
| Member 3 | Dashboard and reports: `Report`, `PeriodReport`, `CategoryReport`, `ReportService`, `ReportController`, `ExportUtil`, `dashboard.html`, `reports.html`, `style.css`, `script.js` |

---

## 8. Where the OOP concepts are

| Concept | Where to look |
| --- | --- |
| Inheritance | `RecurringExpense` and `NonRecurringExpense` extend `Expense`; `PeriodReport` and `CategoryReport` extend `Report`; `InvalidInputException` and `DuplicateReceiptException` extend `BudgetAppException` |
| Abstraction | `Expense`, `Report` and `BudgetAppException` are abstract |
| Polymorphism | `getKind()`, `isRecurring()` and `addDetails()` in the expense classes; `generate()` and `getTitle()` in the report classes |
| Encapsulation | All fields are private; `Staff` never exposes the password hash, only `login(password)` |
| Composition | `BudgetYear` owns its 7 `Allocation`s |
| Collections | `List<Allocation>`, `List<Expense>`, `Set<Category>`; sorting in `suggestCategories()`, grouping by month in `ReportService` |

---

## 9. Before real use by the department

The app already hashes passwords (BCrypt), checks the login on the server for every request,
and blocks SQL injection (JPA). Before the staff relies on it:

- Run it on a college server (ask the IT department), not a student laptop.
- Set your own passwords as environment variables instead of the defaults:
  `DB_PASSWORD` (and change it in `schema.sql`), `STAFF_PASSWORD` (before the first start).
- Use HTTPS, and back up the database (`mysqldump`) and the `uploads/` folder every day.
- Keep using Excel alongside it for a month or two and compare the totals.
