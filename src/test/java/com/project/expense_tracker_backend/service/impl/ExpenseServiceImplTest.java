package com.project.expense_tracker_backend.service.impl;


import com.project.expense_tracker_backend.dto.ExpenseRequestDto;
import com.project.expense_tracker_backend.dto.ExpenseResponseDto;
import com.project.expense_tracker_backend.exception.UserNotFoundException;
import com.project.expense_tracker_backend.mapper.ExpenseMapper;
import com.project.expense_tracker_backend.model.Category;
import com.project.expense_tracker_backend.model.Expense;
import com.project.expense_tracker_backend.model.User;
import com.project.expense_tracker_backend.repository.CategoryRepository;
import com.project.expense_tracker_backend.repository.ExpenseRepository;
import com.project.expense_tracker_backend.repository.UserRepository;
import com.project.expense_tracker_backend.util.DateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ExpenseServiceImplTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ExpenseMapper expenseMapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ExpenseServiceImpl expenseService;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetUserExpenses() {
        long userId = 1L;

        List<Expense> mockUserExpenses = List.of(new Expense(1L, "Lunch", 500.0, LocalDate.now(), getCategory("Food"), getUser(userId)),
                new Expense(2L, "Iphone", 90000.0, LocalDate.now(), getCategory("Shopping"), getUser(userId)));

        List<ExpenseResponseDto> mockResponseDto = List.of(
                new ExpenseResponseDto(1L, "Lunch", 500.0, LocalDate.now(), "Food"),
                new ExpenseResponseDto(1L, "Iphone", 90000.0, LocalDate.now(), "Shopping")
        );

        LocalDate[] firstAndLastDate = DateUtil.getFirstAndLastDateOfMonth(null);

        when(expenseRepository.findByUserUserIdAndDateBetween(userId, firstAndLastDate[0], firstAndLastDate[1]))
                .thenReturn(mockUserExpenses);

        when(expenseMapper.expenseToExpenseResponseMapper(mockUserExpenses)).thenReturn(mockResponseDto);

        List<ExpenseResponseDto> expenseResponseDto = expenseService.getUserExpenses(null, userId);


        assertEquals(2, expenseResponseDto.size());
        assertEquals("Lunch", expenseResponseDto.getFirst().getDescription());
        assertEquals("Shopping", expenseResponseDto.getLast().getCategory());
        assertNotEquals(4, expenseResponseDto.size());
    }

    @Test
    void testSaveExpense_invalid_user_id() {
        long userId = 1L;

        List<Expense> mockUserExpenses = List.of(new Expense(1L, "Lunch", 500.0, LocalDate.now(), getCategory("Food"), getUser(userId)),
                new Expense(2L, "Iphone", 90000.0, LocalDate.now(), getCategory("Shopping"), getUser(userId)));

        Optional<User> nullUser = Optional.empty();
        List<ExpenseRequestDto> mockRequestDto = List.of(
                new ExpenseRequestDto("Lunch", 500.0, LocalDate.now(), "Food"),
                new ExpenseRequestDto("Iphone", 90000.0, LocalDate.now(), "Shopping")
        );

        when(userRepository.findById(userId)).thenReturn(nullUser);

        assertThrows(UserNotFoundException.class, () -> expenseService.saveUserExpenses(userId, mockRequestDto));

    }

    private Category getCategory(String categoryName) {

        return new Category(0L, categoryName);
    }

    private User getUser(long userId) {
        return new User(userId, "Arghya", "test@gmail.com", "encrypted", "1234567890");
    }
}
