package com.project.expense_tracker_backend.mapper;

import com.project.expense_tracker_backend.dto.ExpenseRequestDto;
import com.project.expense_tracker_backend.dto.ExpenseResponseDto;
import com.project.expense_tracker_backend.model.Category;
import com.project.expense_tracker_backend.model.Expense;
import com.project.expense_tracker_backend.model.User;
import com.project.expense_tracker_backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExpenseMapperTest {

    private final ExpenseMapper expenseMapper = new ExpenseMapper();

    @Test
    void testExpenseToExpenseResponseMapper() {

        long userId = 1L;

        List<Expense> userExpenses = List.of(new Expense(1L, "Lunch", 500.0, LocalDate.now(), getCategory("Food"), getUser(userId)),
                new Expense(2L, "Iphone", 90000.0, LocalDate.now(), getCategory("Shopping"), getUser(userId)));

        List<ExpenseResponseDto> expenseResponseDto = expenseMapper.expenseToExpenseResponseMapper(userExpenses);

        assertEquals(2, expenseResponseDto.size());

        assertEquals("Iphone", expenseResponseDto.getLast().getDescription());

        assertEquals(500.0, expenseResponseDto.getFirst().getAmount());

        assertEquals(LocalDate.now(), expenseResponseDto.getFirst().getDate());
    }

    @Test
    void testExpenseRequestToExpenseMapper() {

        ExpenseRequestDto requestDto = new ExpenseRequestDto("Iphone 16 Pro Max", 150000.0, LocalDate.now(), "Shopping");

        Expense userExpense =
                expenseMapper.expenseRequestToExpenseMapper(0L, requestDto, getCategory("Shopping"), getUser(2L));

        assertEquals("Iphone 16 Pro Max", userExpense.getDescription());
        assertEquals(150000.0, userExpense.getAmount());
        assertEquals("Shopping", userExpense.getCategory().getCategoryName());
    }

    private Category getCategory(String categoryName) {

        return new Category(0L, categoryName);
    }

    private User getUser(long userId) {
        return new User(userId, "Arghya", "test@gmail.com", "encrypted","1234567890");
    }
}
