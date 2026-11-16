package com.dept.budget.service;

import com.dept.budget.entity.Expense;
import com.dept.budget.repository.ExpenseRepository;

import java.time.LocalDate;
import java.util.List;

/** All expenses between two dates, across every category. */
public class PeriodReport extends Report {

    public PeriodReport(LocalDate fromDate, LocalDate toDate, String kindFilter, ExpenseRepository expenseRepository) {
        super(fromDate, toDate, kindFilter, expenseRepository);
    }

    @Override
    public List<Expense> generate() {
        return applyKindFilter(expenseRepository.findByExpenseDateBetweenOrderByExpenseDateAsc(fromDate, toDate));
    }

    @Override
    public String getTitle() {
        return "Period Report: " + describePeriod();
    }
}
