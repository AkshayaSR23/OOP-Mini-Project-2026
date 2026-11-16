package com.dept.budget.repository;

import com.dept.budget.entity.Allocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllocationRepository extends JpaRepository<Allocation, Integer> {
}
