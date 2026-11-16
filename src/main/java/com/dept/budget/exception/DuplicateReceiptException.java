package com.dept.budget.exception;

/**
 * Thrown when a receipt number has already been entered.
 * Kept separate because the page treats it differently:
 * it asks "Is this a different bill?" instead of just showing an error.
 */
public class DuplicateReceiptException extends BudgetAppException {

    public DuplicateReceiptException(String message) {
        super(message);
    }
}
