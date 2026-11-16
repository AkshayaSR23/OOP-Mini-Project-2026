package com.dept.budget.service;

import com.dept.budget.entity.Allocation;
import com.dept.budget.entity.BudgetYear;
import com.dept.budget.entity.Category;
import com.dept.budget.entity.Expense;
import com.dept.budget.exception.InvalidInputException;
import com.dept.budget.repository.BudgetYearRepository;
import com.dept.budget.repository.CategoryRepository;
import com.dept.budget.repository.ExpenseRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Member 3: the dashboard, the unused funds suggestion and the reports.
 */
@Service
public class ReportService {

    private final BudgetYearRepository budgetYearRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;

    public ReportService(BudgetYearRepository budgetYearRepository,
                         CategoryRepository categoryRepository,
                         ExpenseRepository expenseRepository) {
        this.budgetYearRepository = budgetYearRepository;
        this.categoryRepository = categoryRepository;
        this.expenseRepository = expenseRepository;
    }

    // ---------- Dashboard ----------

    public Map<String, Object> getDashboard(Integer budgetYearId) {
        BudgetYear year = findYear(budgetYearId);
        Map<String, Object> map = year.toMap(true);   // year details + the 7 category cards

        BigDecimal total = year.getTotalBudget();
        BigDecimal spent = year.getTotalSpent();
        map.put("totalRemaining", total.subtract(spent));
        double utilisation = total.signum() == 0 ? 0
                : spent.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP).doubleValue();
        map.put("utilisation", utilisation);
        return map;
    }

    // ---------- Unused funds ----------

    public Map<String, Object> getUnusedFunds(Integer budgetYearId) {
        BudgetYear year = findYear(budgetYearId);
        LocalDate today = LocalDate.now();
        // January to March is the last quarter of an April-March year
        boolean lastQuarter = year.containsDate(today) && today.getMonthValue() <= 3;

        List<Map<String, Object>> unused = new ArrayList<>();
        for (Allocation a : year.getUnusedFunds()) {
            unused.add(a.toMap());
        }
        List<Map<String, Object>> runningLow = new ArrayList<>();
        for (Allocation a : year.getAllocations()) {
            if (a.getHealth().equals("RED")) {
                runningLow.add(a.toMap());
            }
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("lastQuarter", lastQuarter);
        map.put("unused", unused);
        map.put("runningLow", runningLow);
        return map;
    }

    // ---------- Reports ----------

    /** Creates the right kind of report. The caller just uses it as a Report (polymorphism). */
    public Report buildReport(String reportType, String categoryIdText, String from, String to, String kind) {
        LocalDate fromDate = parseDate(from, "start date");
        LocalDate toDate = parseDate(to, "end date");
        if ("category".equals(reportType)) {
            if (categoryIdText == null || !categoryIdText.trim().matches("\\d+")) {
                throw new InvalidInputException("Choose a category");
            }
            Category category = categoryRepository.findById(Integer.parseInt(categoryIdText.trim()))
                    .orElseThrow(() -> new InvalidInputException("Category not found"));
            return new CategoryReport(fromDate, toDate, kind, category, expenseRepository);
        }
        return new PeriodReport(fromDate, toDate, kind, expenseRepository);
    }

    public Map<String, Object> runReport(Report report) {
        List<Expense> expenses = report.generate();

        List<Map<String, Object>> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        Map<String, BigDecimal> byMonth = new TreeMap<>();      // "2026-05" -> total, in date order
        Map<String, BigDecimal> byCategory = new TreeMap<>();   // "B: Software" -> total, in code order

        for (Expense e : expenses) {
            rows.add(e.toMap());
            total = total.add(e.getAmount());
            byMonth.merge(YearMonth.from(e.getExpenseDate()).toString(), e.getAmount(), BigDecimal::add);
            byCategory.merge(e.getAllocation().getCategory().getLabel(), e.getAmount(), BigDecimal::add);
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", report.getTitle());
        map.put("fromDate", report.getFromDate().toString());
        map.put("toDate", report.getToDate().toString());
        map.put("count", expenses.size());
        map.put("total", total);
        map.put("rows", rows);
        map.put("byMonth", toList(byMonth, "month"));
        map.put("byCategory", toList(byCategory, "category"));
        if (report instanceof CategoryReport) {
            map.put("summary", categorySummary(((CategoryReport) report).getCategory(), report.getFromDate()));
        }
        return map;
    }

    public byte[] exportReport(Report report, boolean pdf) {
        return pdf ? report.exportToPdf() : report.exportToExcel();
    }

    // ---------- Helpers ----------

    /** Allocated, spent and remaining for the category in the year the report starts in. */
    private Map<String, Object> categorySummary(Category category, LocalDate date) {
        for (BudgetYear year : budgetYearRepository.findAll()) {
            if (year.containsDate(date)) {
                for (Allocation a : year.getAllocations()) {
                    if (a.getCategory().equals(category)) {
                        Map<String, Object> summary = a.toMap();
                        summary.put("academicYear", year.getAcademicYear());
                        return summary;
                    }
                }
            }
        }
        return null;
    }

    private List<Map<String, Object>> toList(Map<String, BigDecimal> totals, String keyName) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : totals.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put(keyName, entry.getKey());
            item.put("total", entry.getValue());
            list.add(item);
        }
        return list;
    }

    private LocalDate parseDate(String text, String label) {
        if (text == null || text.isBlank()) {
            throw new InvalidInputException("Choose the " + label);
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidInputException("Enter a valid " + label);
        }
    }

    private BudgetYear findYear(Integer budgetYearId) {
        if (budgetYearId == null) {
            throw new InvalidInputException("Choose a budget year");
        }
        return budgetYearRepository.findById(budgetYearId)
                .orElseThrow(() -> new InvalidInputException("Budget year not found"));
    }
}
