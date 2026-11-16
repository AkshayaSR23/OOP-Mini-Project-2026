package com.dept.budget.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.Map;

/** A bill paid regularly, e.g. a yearly software licence or a quarterly AMC. */
@Entity
@DiscriminatorValue("RECURRING")
public class RecurringExpense extends Expense {

    private String frequency;   // MONTHLY, QUARTERLY, HALF_YEARLY or YEARLY

    @Override
    public String getKind() {
        return "Recurring";
    }

    @Override
    public boolean isRecurring() {
        return true;
    }

    @Override
    protected void addDetails(Map<String, Object> map) {
        map.put("frequency", frequency);
    }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
}
