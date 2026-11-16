package com.dept.budget.repository;

import com.dept.budget.entity.AllocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AllocationHistoryRepository extends JpaRepository<AllocationHistory, Integer> {

    // All changes in one budget year, newest first
    List<AllocationHistory> findByAllocation_BudgetYear_BudgetYearIdOrderByChangedAtDesc(Integer budgetYearId);
}
