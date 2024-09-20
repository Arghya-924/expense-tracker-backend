package com.project.expense_tracker_backend.mapper;

import com.project.expense_tracker_backend.dto.ExpenseRequestDto;
import com.project.expense_tracker_backend.dto.ExpenseResponseDto;
import com.project.expense_tracker_backend.model.Category;
import com.project.expense_tracker_backend.model.Expense;
import com.project.expense_tracker_backend.model.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ExpenseMapper {

    public List<ExpenseResponseDto> expenseToExpenseResponseMapper(List<Expense> expenses) {

        return expenses.stream().map(expense -> new ExpenseResponseDto(
                expense.getExpenseId(), expense.getDescription(), expense.getAmount(), expense.getDate(), expense.getCategory().getCategoryName()
        )).collect(Collectors.toList());
    }

    public Expense expenseRequestToExpenseMapper(long expenseId, ExpenseRequestDto userExpense, Category currentCategory, User user) {

        return new Expense(0L, userExpense.getDescription(),
                userExpense.getAmount(), userExpense.getDate(), currentCategory, user);
    }
}
