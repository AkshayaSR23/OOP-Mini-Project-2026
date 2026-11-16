package com.dept.budget.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One bill entered by the staff.
 * Abstract: only a RecurringExpense or a NonRecurringExpense can be created.
 * Both are stored in the one "expenses" table; the expense_kind column says which.
 */
@Entity
@Table(name = "expenses")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "expense_kind", discriminatorType = DiscriminatorType.STRING)
public abstract class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer expenseId;

    @ManyToOne
    @JoinColumn(name = "allocation_id")
    private Allocation allocation;

    @ManyToOne
    @JoinColumn(name = "type_id")
    private ExpenseType expenseType;

    private LocalDate expenseDate;

    private BigDecimal amount;

    private String description;

    private String receiptNumber;

    private String receiptFile;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    /** "Recurring" or "Non-recurring": each child class gives its own answer. */
    public abstract String getKind();

    public abstract boolean isRecurring();

    /** Children can add their own extra details to the map (polymorphism). */
    protected void addDetails(Map<String, Object> map) {
        // nothing extra by default
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("expenseId", expenseId);
        map.put("allocationId", allocation.getAllocationId());
        map.put("categoryId", allocation.getCategory().getCategoryId());
        map.put("categoryCode", allocation.getCategory().getCode());
        map.put("categoryName", allocation.getCategory().getName());
        map.put("typeId", expenseType.getTypeId());
        map.put("typeName", expenseType.getName());
        map.put("recurring", isRecurring());
        map.put("kind", getKind());
        map.put("frequency", null);
        map.put("expenseDate", expenseDate.toString());
        map.put("amount", amount);
        map.put("description", description);
        map.put("receiptNumber", receiptNumber);
        map.put("hasReceiptFile", receiptFile != null);
        map.put("createdAt", createdAt == null ? null : createdAt.toString());
        map.put("updatedAt", updatedAt == null ? null : updatedAt.toString());
        addDetails(map);
        return map;
    }

    // ---------- Getters and setters ----------

    public Integer getExpenseId() { return expenseId; }
    public Allocation getAllocation() { return allocation; }
    public void setAllocation(Allocation allocation) { this.allocation = allocation; }
    public ExpenseType getExpenseType() { return expenseType; }
    public void setExpenseType(ExpenseType expenseType) { this.expenseType = expenseType; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
    public String getReceiptFile() { return receiptFile; }
    public void setReceiptFile(String receiptFile) { this.receiptFile = receiptFile; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
