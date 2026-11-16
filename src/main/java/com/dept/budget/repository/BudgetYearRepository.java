package com.dept.budget.repository;

import com.dept.budget.entity.BudgetYear;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BudgetYearRepository extends JpaRepository<BudgetYear, Integer> {

    boolean existsByAcademicYear(String academicYear);

    List<BudgetYear> findAllByOrderByAcademicYearDesc();
}
