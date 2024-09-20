package com.project.expense_tracker_backend.exception;

import com.project.expense_tracker_backend.constants.ApplicationConstants;
import com.project.expense_tracker_backend.dto.ErrorResponseDto;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({
            EmailNotFoundException.class,
            BadCredentialsException.class,
            UserNotFoundException.class,
            DataIntegrityViolationException.class,
            YearMonthParseException.class,
            ExpenseNotFoundException.class
    })
    public ResponseEntity<Object> handleEmailNotFoundOrBadCredentialsException(Exception ex, WebRequest request) throws Exception {

        if (ex instanceof EmailNotFoundException emailNotFoundException) {
            return handleEmailNotFoundException(emailNotFoundException, request);
        } else if (ex instanceof BadCredentialsException badCredentialsException) {
            return handleBadCredentialException(badCredentialsException, request);
        } else if (ex instanceof UserNotFoundException userNotFoundException) {
            return handleUserNotFoundException(userNotFoundException, request);
        } else if (ex instanceof DataIntegrityViolationException dataIntegrityViolationException) {
            return handleDataIntegrityViolationException(dataIntegrityViolationException, request);
        } else if (ex instanceof YearMonthParseException yearMonthParseException) {
            return handleYearMonthParseException(yearMonthParseException, request);
        } else if (ex instanceof ExpenseNotFoundException expenseNotFoundException) {
            return handleExpenseNotFoundException(expenseNotFoundException, request);
        } else throw ex;
    }

    private ResponseEntity<Object> handleExpenseNotFoundException(ExpenseNotFoundException expenseNotFoundException, WebRequest request) {

        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                ApplicationConstants.STATUS_FAILURE,
                HttpStatus.BAD_REQUEST,
                request.getDescription(false),
                List.of(expenseNotFoundException.getLocalizedMessage())
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    private ResponseEntity<Object> handleYearMonthParseException(YearMonthParseException yearMonthParseException, WebRequest request) {

        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                ApplicationConstants.STATUS_FAILURE,
                HttpStatus.BAD_REQUEST,
                request.getDescription(false),
                List.of(yearMonthParseException.getLocalizedMessage())
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    private ResponseEntity<Object> handleDataIntegrityViolationException(
            DataIntegrityViolationException dataIntegrityViolationException, WebRequest request) {

        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                ApplicationConstants.STATUS_FAILURE,
                HttpStatus.BAD_REQUEST,
                request.getDescription(false),
                List.of(dataIntegrityViolationException.getLocalizedMessage())
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);

    }

    private ResponseEntity<Object> handleUserNotFoundException(UserNotFoundException userNotFoundException, WebRequest request) {

        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                ApplicationConstants.STATUS_FAILURE,
                HttpStatus.BAD_REQUEST,
                request.getDescription(false),
                List.of(userNotFoundException.getLocalizedMessage())
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    private ResponseEntity<Object> handleBadCredentialException(BadCredentialsException badCredentialsException, WebRequest request) {

        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                ApplicationConstants.STATUS_FAILURE,
                HttpStatus.FORBIDDEN,
                request.getDescription(false),
                List.of(badCredentialsException.getLocalizedMessage())
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponseDto);
    }

    private ResponseEntity<Object> handleEmailNotFoundException(EmailNotFoundException emailNotFoundException, WebRequest request) {

        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                ApplicationConstants.STATUS_FAILURE,
                HttpStatus.BAD_REQUEST,
                request.getDescription(false),
                List.of(emailNotFoundException.getLocalizedMessage())
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }


}
