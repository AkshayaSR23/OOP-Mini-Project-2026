package com.dept.budget.service;

import com.dept.budget.entity.Expense;
import com.dept.budget.exception.InvalidInputException;
import com.dept.budget.repository.ExpenseRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Any report covering a date range.
 * Abstract: PeriodReport and CategoryReport each decide which expenses to include
 * by writing their own generate() (polymorphism).
 */
public abstract class Report {

    private static final List<String> HEADERS =
            List.of("Date", "Category", "Expense type", "Kind", "Description", "Receipt No.", "Amount (Rs.)");

    protected final LocalDate fromDate;
    protected final LocalDate toDate;
    protected final String kindFilter;   // ALL, RECURRING or NON_RECURRING
    protected final ExpenseRepository expenseRepository;

    protected Report(LocalDate fromDate, LocalDate toDate, String kindFilter, ExpenseRepository expenseRepository) {
        if (fromDate == null || toDate == null) {
            throw new InvalidInputException("Choose both a start date and an end date");
        }
        if (fromDate.isAfter(toDate)) {
            throw new InvalidInputException("The start date cannot be after the end date");
        }
        if (toDate.isAfter(LocalDate.now())) {
            throw new InvalidInputException("The end date cannot be in the future");
        }
        String kind = (kindFilter == null || kindFilter.isBlank()) ? "ALL" : kindFilter.trim().toUpperCase();
        if (!List.of("ALL", "RECURRING", "NON_RECURRING").contains(kind)) {
            throw new InvalidInputException("Choose All, Recurring or Non-recurring");
        }
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.kindFilter = kind;
        this.expenseRepository = expenseRepository;
    }

    /** The expenses that belong in this report. */
    public abstract List<Expense> generate();

    public abstract String getTitle();

    protected List<Expense> applyKindFilter(List<Expense> expenses) {
        if (kindFilter.equals("ALL")) {
            return expenses;
        }
        boolean wantRecurring = kindFilter.equals("RECURRING");
        List<Expense> filtered = new ArrayList<>();
        for (Expense e : expenses) {
            if (e.isRecurring() == wantRecurring) {
                filtered.add(e);
            }
        }
        return filtered;
    }

    protected String describePeriod() {
        String text = fromDate + " to " + toDate;
        if (kindFilter.equals("RECURRING")) text += ", recurring only";
        if (kindFilter.equals("NON_RECURRING")) text += ", non-recurring only";
        return text;
    }

    public byte[] exportToExcel() {
        return ExportUtil.toExcel(getTitle(), HEADERS, toRows(generate()));
    }

    public byte[] exportToPdf() {
        return ExportUtil.toPdf(getTitle(), HEADERS, toRows(generate()));
    }

    private List<List<String>> toRows(List<Expense> expenses) {
        List<List<String>> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Expense e : expenses) {
            rows.add(List.of(
                    e.getExpenseDate().toString(),
                    e.getAllocation().getCategory().getLabel(),
                    e.getExpenseType().getName(),
                    e.getKind(),
                    e.getDescription(),
                    e.getReceiptNumber(),
                    e.getAmount().toPlainString()));
            total = total.add(e.getAmount());
        }
        rows.add(List.of("", "", "", "", "Total", "", total.toPlainString()));
        return rows;
    }

    public LocalDate getFromDate() { return fromDate; }
    public LocalDate getToDate() { return toDate; }
}
