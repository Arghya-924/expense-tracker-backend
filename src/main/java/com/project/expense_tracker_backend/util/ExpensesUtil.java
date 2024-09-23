package com.project.expense_tracker_backend.util;

import com.project.expense_tracker_backend.model.Expense;

import java.time.YearMonth;
import java.util.Map;

public class ExpensesUtil {

    public static void populateExpensePerYearMonthMap(Expense newExpense, Map<YearMonth, Double> aggregateMap) {

        YearMonth expenseYearMonth = DateUtil.getYearMonth(newExpense.getDate());

        aggregateMap.put(expenseYearMonth, aggregateMap.getOrDefault(expenseYearMonth, 0.0) + newExpense.getAmount());
    }
}
