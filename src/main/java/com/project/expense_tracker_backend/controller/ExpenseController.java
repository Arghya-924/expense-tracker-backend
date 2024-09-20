package com.project.expense_tracker_backend.controller;

import com.project.expense_tracker_backend.constants.ApplicationConstants;
import com.project.expense_tracker_backend.dto.ExpenseRequestDto;
import com.project.expense_tracker_backend.dto.ExpenseResponseDto;
import com.project.expense_tracker_backend.dto.UserExpensesResponse;
import com.project.expense_tracker_backend.exception.ExpenseNotFoundException;
import com.project.expense_tracker_backend.exception.UserNotFoundException;
import com.project.expense_tracker_backend.exception.YearMonthParseException;
import com.project.expense_tracker_backend.service.IExpenseService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
@Slf4j
public class ExpenseController {

    private IExpenseService expenseService;

    @GetMapping("/expenses")
    public ResponseEntity<UserExpensesResponse<List<ExpenseResponseDto>>> getUserExpenses(
            @RequestParam(required = false) String yearMonth, HttpServletRequest request) {

        try {
            long userId = Long.parseLong(request.getAttribute(ApplicationConstants.REQUEST_USER_ID_ATTRIBUTE).toString());

            List<ExpenseResponseDto> userExpenses = expenseService.getUserExpenses(yearMonth, userId);

            return new ResponseEntity<>(new UserExpensesResponse<>(userExpenses), HttpStatus.OK);

        } catch (DateTimeParseException dateTimeParseException) {
            log.error("ExpenseController | getUserExpenses | Exception : {}", dateTimeParseException.getLocalizedMessage());
            throw new YearMonthParseException(String.format(ApplicationConstants.YEAR_MONTH_NOT_VALID, yearMonth));
        }
    }

    @PostMapping("/expenses")
    public ResponseEntity<List<ExpenseResponseDto>> saveUserExpenses(HttpServletRequest request,
                                                                     @RequestBody List<ExpenseRequestDto> userExpenses) {

        long userId = Long.parseLong(request.getAttribute(ApplicationConstants.REQUEST_USER_ID_ATTRIBUTE).toString());
        try {
            List<ExpenseResponseDto> savedUserExpenses = expenseService.saveUserExpenses(userId, userExpenses);

            return ResponseEntity.status(HttpStatus.CREATED).body(savedUserExpenses);
        } catch (UserNotFoundException userNotFoundException) {

            log.error("saveUserExpensesController | Exception occured: {}", userNotFoundException.getLocalizedMessage());
            throw userNotFoundException;
        }
    }

    @PutMapping("/expenses/{expenseId}")
    public ResponseEntity<ExpenseResponseDto> updateUserExpense(
            @PathVariable long expenseId, @RequestBody ExpenseRequestDto expenseRequestDto,HttpServletRequest request) {

        long userId = Long.parseLong(request.getAttribute(ApplicationConstants.REQUEST_USER_ID_ATTRIBUTE).toString());

        try{
            ExpenseResponseDto updatedUserExpense = expenseService.updateUserExpense(userId, expenseId, expenseRequestDto);

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(updatedUserExpense);
        }
        catch (ExpenseNotFoundException exception) {
            log.error("ExpenseController | UpdateUserExpense | Exception Occurred | {}", exception.getLocalizedMessage());
            throw exception;
        }
    }

    @DeleteMapping("/expenses/{expenseId}")
    public ResponseEntity<Void> deleteUserExpense(@PathVariable long expenseId, HttpServletRequest request) {

        long userId = Long.parseLong(request.getAttribute(ApplicationConstants.REQUEST_USER_ID_ATTRIBUTE).toString());

        try{
            expenseService.deleteUserExpense(expenseId, userId);

            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        catch (ExpenseNotFoundException exception) {
            log.error("ExpenseController | deleteUserExpense | Exception Occurred | {}", exception.getLocalizedMessage());
            throw exception;
        }
    }
}
