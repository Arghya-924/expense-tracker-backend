package com.project.expense_tracker_backend.service;

import com.project.expense_tracker_backend.dto.ExpenseRequestDto;
import com.project.expense_tracker_backend.dto.ExpenseResponseDto;

import java.util.List;

public interface IExpenseService {

    List<ExpenseResponseDto> getUserExpenses(String yearMonth, long userId);

    List<ExpenseResponseDto> saveUserExpenses(long userId, List<ExpenseRequestDto> userExpenses);

    ExpenseResponseDto updateUserExpense(long userId, long expenseId, ExpenseRequestDto expenseRequestDto);

    void deleteUserExpense(long expenseId, long userId);

    Double getTotalMonthlyUserExpense(String yearMonth, long userId);
}
