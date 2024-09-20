package com.project.expense_tracker_backend.repository;

import com.project.expense_tracker_backend.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByUserUserId(long userId);

    List<Expense> findByUserUserIdAndDateBetween(long userId, LocalDate startDate, LocalDate endDate);

    Optional<Expense> findByExpenseIdAndUserUserId(long expenseId, long userId);
}
