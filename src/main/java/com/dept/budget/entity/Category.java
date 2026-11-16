package com.dept.budget.entity;

import jakarta.persistence.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** One of the seven budget categories, A to G. */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer categoryId;

    private String code;

    private String name;

    protected Category() {
        // needed by JPA
    }

    public Integer getCategoryId() { return categoryId; }
    public String getCode() { return code; }
    public String getName() { return name; }

    /** e.g. "B: Software" */
    public String getLabel() {
        return code + ": " + name;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("categoryId", categoryId);
        map.put("code", code);
        map.put("name", name);
        return map;
    }

    // Two Category objects are equal if they have the same id (needed for Set.contains).
    // Getters are used on purpose: Hibernate sometimes gives a stand-in (proxy) object
    // whose fields are empty until a getter is called.
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Category)) return false;
        Integer otherId = ((Category) other).getCategoryId();
        return getCategoryId() != null && getCategoryId().equals(otherId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getCategoryId());
    }
}
