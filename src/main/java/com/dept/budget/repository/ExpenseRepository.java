package com.dept.budget.repository;

import com.dept.budget.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/** Spring writes the SQL for each method from its name. */
public interface ExpenseRepository extends JpaRepository<Expense, Integer> {

    // Duplicate receipt check
    List<Expense> findByReceiptNumber(String receiptNumber);

    // Expenses page: all expenses of one budget year, newest first
    List<Expense> findByAllocation_BudgetYear_BudgetYearIdOrderByExpenseDateDescExpenseIdDesc(Integer budgetYearId);

    // Period report
    List<Expense> findByExpenseDateBetweenOrderByExpenseDateAsc(LocalDate from, LocalDate to);

    // Category report
    List<Expense> findByAllocation_Category_CategoryIdAndExpenseDateBetweenOrderByExpenseDateAsc(
            Integer categoryId, LocalDate from, LocalDate to);
}
