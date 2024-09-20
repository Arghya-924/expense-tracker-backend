package com.project.expense_tracker_backend.exception;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class EmailNotFoundException extends UsernameNotFoundException {


    public EmailNotFoundException(String message, String email) {

        super(String.format(message, email));
    }
}
