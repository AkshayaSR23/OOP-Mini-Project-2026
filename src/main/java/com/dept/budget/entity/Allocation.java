package com.dept.budget.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The money one category gets in one budget year,
 * e.g. "B: Software gets Rs. 2,50,000 in 2026-27".
 * Spent and remaining are calculated from its expenses, never stored.
 */
@Entity
@Table(name = "allocations")
public class Allocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer allocationId;

    @ManyToOne
    @JoinColumn(name = "budget_year_id")
    private BudgetYear budgetYear;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    private BigDecimal amount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "allocation")
    private List<Expense> expenses = new ArrayList<>();

    protected Allocation() {
        // needed by JPA
    }

    public Allocation(BudgetYear budgetYear, Category category, BigDecimal amount) {
        this.budgetYear = budgetYear;
        this.category = category;
        this.amount = amount;
    }

    public BigDecimal getTotalSpent() {
        BigDecimal total = BigDecimal.ZERO;
        for (Expense e : expenses) {
            total = total.add(e.getAmount());
        }
        return total;
    }

    public BigDecimal getRemaining() {
        return amount.subtract(getTotalSpent());
    }

    /** Percentage used, e.g. 76.4 */
    public double getUtilisation() {
        if (amount.signum() == 0) {
            return 0;
        }
        return getTotalSpent().multiply(BigDecimal.valueOf(100))
                .divide(amount, 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /** GREEN under 75%, AMBER 75 to 90%, RED over 90% */
    public String getHealth() {
        double used = getUtilisation();
        if (used > 90) return "RED";
        if (used >= 75) return "AMBER";
        return "GREEN";
    }

    public boolean canAccommodate(BigDecimal extra) {
        return getRemaining().compareTo(extra) >= 0;
    }

    /**
     * Changes the allocated amount after the budget is fixed
     * and returns the history record of the change.
     */
    public AllocationHistory changeAmount(BigDecimal newAmount, String reason) {
        AllocationHistory history = new AllocationHistory(this, amount, newAmount, reason);
        this.amount = newAmount;
        return history;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("allocationId", allocationId);
        map.put("categoryId", category.getCategoryId());
        map.put("code", category.getCode());
        map.put("name", category.getName());
        map.put("amount", amount);
        map.put("spent", getTotalSpent());
        map.put("remaining", getRemaining());
        map.put("utilisation", getUtilisation());
        map.put("health", getHealth());
        return map;
    }

    public Integer getAllocationId() { return allocationId; }
    public BudgetYear getBudgetYear() { return budgetYear; }
    public Category getCategory() { return category; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public List<Expense> getExpenses() { return expenses; }
}
