package com.dept.budget.controller;

/**
 * The data the expense form sends when adding or editing an expense.
 * Amount and date arrive as text so the server can give clear error messages.
 */
public class ExpenseRequest {

    private Integer allocationId;
    private Integer typeId;
    private boolean recurring;
    private String frequency;
    private String expenseDate;
    private String amount;
    private String description;
    private String receiptNumber;
    private boolean confirmedDuplicate;   // true after the staff says "yes, it's a different bill"

    public Integer getAllocationId() { return allocationId; }
    public void setAllocationId(Integer allocationId) { this.allocationId = allocationId; }
    public Integer getTypeId() { return typeId; }
    public void setTypeId(Integer typeId) { this.typeId = typeId; }
    public boolean isRecurring() { return recurring; }
    public void setRecurring(boolean recurring) { this.recurring = recurring; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public String getExpenseDate() { return expenseDate; }
    public void setExpenseDate(String expenseDate) { this.expenseDate = expenseDate; }
    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
    public boolean isConfirmedDuplicate() { return confirmedDuplicate; }
    public void setConfirmedDuplicate(boolean confirmedDuplicate) { this.confirmedDuplicate = confirmedDuplicate; }
}
