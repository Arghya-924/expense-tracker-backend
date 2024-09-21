package com.project.expense_tracker_backend.repository;

import com.project.expense_tracker_backend.model.AggregateExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Month;
import java.util.Optional;

@Repository
public interface AggregateExpenseRepository extends JpaRepository<AggregateExpense, Long> {

//    Optional<AggregateExpense> findAggregateExpenseByUserUserIdAndExpenseMonth(long userId, Month expenseMonth);

    Optional<AggregateExpense> findAggregateExpenseByUserUserIdAndExpenseMonthAndExpenseYear(long userId, Month expenseMonth, int expenseYear);
}
