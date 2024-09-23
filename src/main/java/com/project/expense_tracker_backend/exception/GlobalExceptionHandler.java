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

        return switch (ex) {
            case EmailNotFoundException emailNotFoundException ->
                    handleEmailNotFoundException(emailNotFoundException, request);
            case BadCredentialsException badCredentialsException ->
                    handleBadCredentialException(badCredentialsException, request);
            case UserNotFoundException userNotFoundException ->
                    handleUserNotFoundException(userNotFoundException, request);
            case DataIntegrityViolationException dataIntegrityViolationException ->
                    handleDataIntegrityViolationException(dataIntegrityViolationException, request);
            case YearMonthParseException yearMonthParseException ->
                    handleYearMonthParseException(yearMonthParseException, request);
            case ExpenseNotFoundException expenseNotFoundException ->
                    handleExpenseNotFoundException(expenseNotFoundException, request);
            case null, default -> throw ex;
        };
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleAllExceptions(Exception ex, WebRequest request) {
        ex.printStackTrace();
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                ApplicationConstants.STATUS_FAILURE,
                HttpStatus.INTERNAL_SERVER_ERROR,
                request.getDescription(false),
                List.of(ex.getLocalizedMessage())
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponseDto);

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
