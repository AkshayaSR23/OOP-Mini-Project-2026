package com.dept.budget.repository;

import com.dept.budget.entity.ExpenseType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseTypeRepository extends JpaRepository<ExpenseType, Integer> {

    List<ExpenseType> findAllByOrderByNameAsc();
}
