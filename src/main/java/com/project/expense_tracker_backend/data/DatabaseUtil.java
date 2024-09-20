package com.project.expense_tracker_backend.data;

import com.project.expense_tracker_backend.model.Expense;
import com.project.expense_tracker_backend.repository.ExpenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Component
public class DatabaseUtil {

    private static final Logger log = LoggerFactory.getLogger(DatabaseUtil.class);
    @Autowired
    private ExpenseRepository expenseRepository;

    @Cacheable("userExpenses")
    public List<Expense> fetchExpenses(long userId, LocalDate fromDate, LocalDate toDate) {

        log.info("Inside caching");
        return expenseRepository.findByUserUserIdAndDateBetween(userId, fromDate, toDate);
    }
}
