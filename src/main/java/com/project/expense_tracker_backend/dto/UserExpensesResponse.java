package com.project.expense_tracker_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserExpensesResponse<T> {

    private T userExpenses;
    private Double totalMonthlyExpense;
}
