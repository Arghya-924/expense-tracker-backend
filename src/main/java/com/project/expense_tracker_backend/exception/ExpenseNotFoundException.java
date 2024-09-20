package com.project.expense_tracker_backend.exception;

import com.project.expense_tracker_backend.constants.ApplicationConstants;

public class ExpenseNotFoundException extends RuntimeException{

    public ExpenseNotFoundException(long expenseId) {
        super(String.format(ApplicationConstants.EXPENSE_NOT_FOUND, expenseId));
    }

    public ExpenseNotFoundException(long expenseId, long userId) {
        super(String.format(ApplicationConstants.EXPENSE_USER_NOT_MATCH, expenseId, userId));
    }
}
