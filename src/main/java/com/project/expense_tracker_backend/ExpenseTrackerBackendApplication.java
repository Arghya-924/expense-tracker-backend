package com.project.expense_tracker_backend;

import com.project.expense_tracker_backend.model.AggregateExpense;
import com.project.expense_tracker_backend.model.Category;
import com.project.expense_tracker_backend.model.Expense;
import com.project.expense_tracker_backend.model.User;
import com.project.expense_tracker_backend.repository.AggregateExpenseRepository;
import com.project.expense_tracker_backend.repository.CategoryRepository;
import com.project.expense_tracker_backend.repository.ExpenseRepository;
import com.project.expense_tracker_backend.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.Date;

@SpringBootApplication
@AllArgsConstructor
@EnableCaching
public class ExpenseTrackerBackendApplication {

    private UserRepository userRepository;
    private ExpenseRepository expenseRepository;
    private CategoryRepository categoryRepository;
    private AggregateExpenseRepository aggregateExpenseRepository;

    public static void main(String[] args) {
        SpringApplication.run(ExpenseTrackerBackendApplication.class, args);
    }

    @PostConstruct
    public void init() {

        Category category1 = new Category(0L, "Groceries");
        Category category2 = new Category(0L, "Shopping");

        category1 = categoryRepository.save(category1);
        category2 = categoryRepository.save(category2);

        User user1 = new User(0L, "Arghya", "test1@gmail.com", "12345", "1234456667");
        User user2 = new User(0L, "Ashmita", "test2@gmail.com", "12345", "1234567890");

        user1 = userRepository.save(user1);
        user2 = userRepository.save(user2);

        Expense expense1 = new Expense(0L, "iPhone", 70000.0, LocalDate.now(), category2, user1);
        Expense expense2 = new Expense(0L, "fish, eggs", 500.0, LocalDate.now(), category1, user2);
        Expense expense3 = new Expense(0L, "iPad", 25000.0, LocalDate.now(), category2, user1);

        expenseRepository.save(expense1);
        expenseRepository.save(expense2);
        expenseRepository.save(expense3);

        AggregateExpense aggregateExpense1 = new AggregateExpense(0L, user1, Month.of(new Date().getMonth() + 1), Year.now().getValue(), (double) 95000);
        AggregateExpense aggregateExpense2 = new AggregateExpense(0L, user2, Month.of(new Date().getMonth() + 1), Year.now().getValue(), (double) 500);

        aggregateExpenseRepository.save(aggregateExpense1);
        aggregateExpenseRepository.save(aggregateExpense2);

    }

}
