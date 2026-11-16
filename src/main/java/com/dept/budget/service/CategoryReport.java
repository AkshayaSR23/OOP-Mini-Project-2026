package com.dept.budget.service;

import com.dept.budget.entity.Category;
import com.dept.budget.entity.Expense;
import com.dept.budget.repository.ExpenseRepository;

import java.time.LocalDate;
import java.util.List;

/** Expenses of one chosen category between two dates. */
public class CategoryReport extends Report {

    private final Category category;

    public CategoryReport(LocalDate fromDate, LocalDate toDate, String kindFilter,
                          Category category, ExpenseRepository expenseRepository) {
        super(fromDate, toDate, kindFilter, expenseRepository);
        this.category = category;
    }

    @Override
    public List<Expense> generate() {
        return applyKindFilter(expenseRepository
                .findByAllocation_Category_CategoryIdAndExpenseDateBetweenOrderByExpenseDateAsc(
                        category.getCategoryId(), fromDate, toDate));
    }

    @Override
    public String getTitle() {
        return "Category Report: " + category.getLabel() + ", " + describePeriod();
    }

    public Category getCategory() { return category; }
}
