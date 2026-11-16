package com.dept.budget.exception;

/**
 * Thrown for any wrong input or broken rule,
 * e.g. a negative amount, a future date, or editing a fixed proposal.
 */
public class InvalidInputException extends BudgetAppException {

    public InvalidInputException(String message) {
        super(message);
    }
}
