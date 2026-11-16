package com.dept.budget.entity;

import com.dept.budget.exception.InvalidInputException;
import com.dept.budget.service.ExportUtil;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One academic year's budget, e.g. "2026-27" (1 April 2026 to 31 March 2027).
 * It starts as a DRAFT proposal and becomes FIXED after the Principal approves it.
 * It owns exactly 7 allocations, one per category (composition).
 */
@Entity
@Table(name = "budget_years")
public class BudgetYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer budgetYearId;

    private String academicYear;

    private String status = "DRAFT";   // "DRAFT" or "FIXED"

    private LocalDate fixedOn;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @OneToMany(mappedBy = "budgetYear", cascade = CascadeType.ALL)
    @OrderBy("allocationId ASC")
    private List<Allocation> allocations = new ArrayList<>();

    protected BudgetYear() {
        // needed by JPA
    }

    public BudgetYear(String academicYear, Staff staff) {
        this.academicYear = academicYear;
        this.staff = staff;
    }

    // ---------- Proposal ----------

    public boolean isFixed() {
        return "FIXED".equals(status);
    }

    /** Locks the planned amounts. Called after the Principal approves the proposal. */
    public void fixProposal() {
        if (isFixed()) {
            throw new InvalidInputException("This proposal is already fixed");
        }
        if (getTotalBudget().signum() == 0) {
            throw new InvalidInputException("Enter the budget amounts before fixing the proposal");
        }
        status = "FIXED";
        fixedOn = LocalDate.now();
    }

    // ---------- Dates ----------

    private int getStartYear() {
        return Integer.parseInt(academicYear.substring(0, 4));
    }

    public LocalDate getStartDate() {
        return LocalDate.of(getStartYear(), 4, 1);
    }

    public LocalDate getEndDate() {
        return LocalDate.of(getStartYear() + 1, 3, 31);
    }

    public boolean containsDate(LocalDate date) {
        return !date.isBefore(getStartDate()) && !date.isAfter(getEndDate());
    }

    // ---------- Totals ----------

    public BigDecimal getTotalBudget() {
        BigDecimal total = BigDecimal.ZERO;
        for (Allocation a : allocations) {
            total = total.add(a.getAmount());
        }
        return total;
    }

    public BigDecimal getTotalSpent() {
        BigDecimal total = BigDecimal.ZERO;
        for (Allocation a : allocations) {
            total = total.add(a.getTotalSpent());
        }
        return total;
    }

    // ---------- Smart features ----------

    /**
     * Category suggestion: the categories this expense type can go under,
     * with the most money left first.
     */
    public List<Allocation> suggestCategories(ExpenseType type) {
        List<Allocation> eligible = new ArrayList<>();
        for (Allocation a : allocations) {
            if (type.isEligible(a.getCategory())) {
                eligible.add(a);
            }
        }
        eligible.sort((x, y) -> y.getRemaining().compareTo(x.getRemaining()));
        return eligible;
    }

    /** Unused funds: categories that have used less than half of their money. */
    public List<Allocation> getUnusedFunds() {
        List<Allocation> unused = new ArrayList<>();
        for (Allocation a : allocations) {
            if (a.getAmount().signum() > 0 && a.getUtilisation() < 50) {
                unused.add(a);
            }
        }
        unused.sort((x, y) -> y.getRemaining().compareTo(x.getRemaining()));
        return unused;
    }

    // ---------- Download ----------

    public byte[] exportToExcel() {
        return ExportUtil.toExcel(getExportTitle(), getExportHeaders(), getExportRows());
    }

    public byte[] exportToPdf() {
        return ExportUtil.toPdf(getExportTitle(), getExportHeaders(), getExportRows());
    }

    private String getExportTitle() {
        String state = isFixed() ? "Fixed on " + fixedOn : "Draft";
        return "Budget Proposal " + academicYear + " (" + state + ")";
    }

    private List<String> getExportHeaders() {
        return List.of("Code", "Category", "Amount (Rs.)");
    }

    private List<List<String>> getExportRows() {
        List<List<String>> rows = new ArrayList<>();
        for (Allocation a : allocations) {
            rows.add(List.of(a.getCategory().getCode(), a.getCategory().getName(), a.getAmount().toPlainString()));
        }
        rows.add(List.of("", "Total", getTotalBudget().toPlainString()));
        return rows;
    }

    // ---------- For the pages ----------

    public Map<String, Object> toMap(boolean withAllocations) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("budgetYearId", budgetYearId);
        map.put("academicYear", academicYear);
        map.put("status", status);
        map.put("fixedOn", fixedOn == null ? null : fixedOn.toString());
        map.put("startDate", getStartDate().toString());
        map.put("endDate", getEndDate().toString());
        map.put("totalBudget", getTotalBudget());
        if (withAllocations) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (Allocation a : allocations) {
                list.add(a.toMap());
            }
            map.put("allocations", list);
            map.put("totalSpent", getTotalSpent());
        }
        return map;
    }

    // ---------- Getters ----------

    public Integer getBudgetYearId() { return budgetYearId; }
    public String getAcademicYear() { return academicYear; }
    public String getStatus() { return status; }
    public LocalDate getFixedOn() { return fixedOn; }
    public List<Allocation> getAllocations() { return allocations; }
}
