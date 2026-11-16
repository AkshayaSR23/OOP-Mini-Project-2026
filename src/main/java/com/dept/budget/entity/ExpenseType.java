package com.dept.budget.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A kind of purchase, e.g. "Spare part", and the categories it is allowed under.
 * Used by the category suggestion feature.
 */
@Entity
@Table(name = "expense_types")
public class ExpenseType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer typeId;

    private String name;

    @ManyToMany
    @JoinTable(name = "expense_type_categories",
            joinColumns = @JoinColumn(name = "type_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> eligibleCategories = new HashSet<>();

    protected ExpenseType() {
        // needed by JPA
    }

    public boolean isEligible(Category category) {
        return eligibleCategories.contains(category);
    }

    public Map<String, Object> toMap() {
        List<Integer> categoryIds = new ArrayList<>();
        for (Category c : eligibleCategories) {
            categoryIds.add(c.getCategoryId());
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("typeId", typeId);
        map.put("name", name);
        map.put("categoryIds", categoryIds);
        return map;
    }

    public Integer getTypeId() { return typeId; }
    public String getName() { return name; }
}
