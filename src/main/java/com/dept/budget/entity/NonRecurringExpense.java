package com.dept.budget.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** A one-time purchase, e.g. a new oscilloscope. */
@Entity
@DiscriminatorValue("NON_RECURRING")
public class NonRecurringExpense extends Expense {

    @Override
    public String getKind() {
        return "Non-recurring";
    }

    @Override
    public boolean isRecurring() {
        return false;
    }
}
