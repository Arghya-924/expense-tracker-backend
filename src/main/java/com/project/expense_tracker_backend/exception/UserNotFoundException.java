package com.project.expense_tracker_backend.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message, long userId) {

        super(String.format(message, userId));
    }
}
