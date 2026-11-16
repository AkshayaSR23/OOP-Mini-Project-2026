package com.dept.budget.exception;

/**
 * Parent of all the app's own exceptions.
 * Abstract, so only its specific children are ever thrown.
 */
public abstract class BudgetAppException extends RuntimeException {

    public BudgetAppException(String message) {
        super(message);
    }
}
