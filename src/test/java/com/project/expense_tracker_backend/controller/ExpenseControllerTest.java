package com.project.expense_tracker_backend.controller;

import com.project.expense_tracker_backend.dto.ExpenseRequestDto;
import com.project.expense_tracker_backend.dto.UserExpensesResponse;
import com.project.expense_tracker_backend.dto.ExpenseResponseDto;
import com.project.expense_tracker_backend.service.IExpenseService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class ExpenseControllerTest {

    @Mock
    private IExpenseService expenseService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private ExpenseController expenseController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetUserExpenses() {
        long userId = 1L;
        List<ExpenseResponseDto> mockExpenses = Arrays.asList(
                new ExpenseResponseDto(1L,"Lunch", 20.0, LocalDate.now(), "Food"),
                new ExpenseResponseDto(2L,"Groceries", 50.0, LocalDate.now(), "Shopping")
        );

        when(httpServletRequest.getAttribute("userId")).thenReturn(String.valueOf(userId));
        when(expenseService.getUserExpenses(null, userId)).thenReturn(mockExpenses);

        ResponseEntity<UserExpensesResponse<List<ExpenseResponseDto>>> response =
                expenseController.getUserExpenses(null, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, Objects.requireNonNull(response.getBody()).getUserExpenses().size());
        assertEquals("Lunch", response.getBody().getUserExpenses().getFirst().getDescription());
    }

    @Test
    void testSaveUserExpenses() {
        long userId = 2L;

        List<ExpenseRequestDto> newExpenses = List.of(
                new ExpenseRequestDto("Iphone 16 Pro Max", 150000.0, LocalDate.now(), "Shopping"),
                new ExpenseRequestDto("Saree", 2000.0, LocalDate.now(), "Shopping")
        );

        List<ExpenseResponseDto> savedExpenses = List.of(
                new ExpenseResponseDto(1L,"Iphone 16 Pro Max", 150000.0, LocalDate.now(), "Shopping"),
                new ExpenseResponseDto(2L,"Saree", 2000.0, LocalDate.now(), "Shopping")
        );

        when(httpServletRequest.getAttribute("userId")).thenReturn(String.valueOf(userId));
        when(expenseService.saveUserExpenses(userId, newExpenses)).thenReturn(savedExpenses);

        ResponseEntity<List<ExpenseResponseDto>> response = expenseController.saveUserExpenses(httpServletRequest, newExpenses);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(savedExpenses);
        assertEquals(2, response.getBody().size());
        assertEquals("Saree", response.getBody().getLast().getDescription());
    }
}
