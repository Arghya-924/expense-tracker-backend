package com.project.expense_tracker_backend;

import com.project.expense_tracker_backend.model.AggregateExpense;
import com.project.expense_tracker_backend.model.Category;
import com.project.expense_tracker_backend.model.Expense;
import com.project.expense_tracker_backend.model.User;
import com.project.expense_tracker_backend.repository.AggregateExpenseRepository;
import com.project.expense_tracker_backend.repository.CategoryRepository;
import com.project.expense_tracker_backend.repository.ExpenseRepository;
import com.project.expense_tracker_backend.repository.UserRepository;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;

@SpringBootApplication
@AllArgsConstructor
@EnableCaching
@OpenAPIDefinition(security = { @SecurityRequirement(name = "bearerToken") })
@SecurityScheme(name = "bearerToken", type = SecuritySchemeType.HTTP, scheme = "bearer")
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

        // Create categories
        Category groceries = categoryRepository.save(new Category(null, "Groceries"));
        Category shopping = categoryRepository.save(new Category(null, "Shopping"));
        Category transportation = categoryRepository.save(new Category(null, "Transportation"));
        Category entertainment = categoryRepository.save(new Category(null, "Entertainment"));
        Category utilities = categoryRepository.save(new Category(null, "Utilities"));
        Category foodDining = categoryRepository.save(new Category(null, "Food & Dining"));

        // Create users (password for test1: "12345")
        User user1 = userRepository.save(new User(null, "Arghya", "test1@gmail.com",
                "$2a$10$AcxxmVRLWBX1cBDitvhBDeqGagTuE3.4VYF7SdE.46hNaq5uzdaKG", "1234456667", LocalDateTime.now(),
                LocalDateTime.now()));
        // password for test2: 123456
        User user2 = userRepository.save(new User(null, "Ashmita", "test2@gmail.com",
                "$2a$10$yGbXyAkIObl5Om6j6k9/3esTdVwdAsa.OtZ3clFfOs4wsnKnznO5q", "1234567890", LocalDateTime.now(),
                LocalDateTime.now()));

        // ============ User 1 (Arghya) Expenses ============

        // January 2026
        expenseRepository.save(new Expense(null, "iPhone 15 Pro", 70000.0, LocalDate.of(2026, 1, 15), shopping, user1));
        expenseRepository
                .save(new Expense(null, "Weekly groceries", 2500.0, LocalDate.of(2026, 1, 12), groceries, user1));
        expenseRepository
                .save(new Expense(null, "Netflix subscription", 649.0, LocalDate.of(2026, 1, 5), entertainment, user1));
        expenseRepository
                .save(new Expense(null, "Uber rides", 1200.0, LocalDate.of(2026, 1, 10), transportation, user1));

        // December 2025
        expenseRepository.save(new Expense(null, "iPad Air", 25000.0, LocalDate.of(2025, 12, 20), shopping, user1));
        expenseRepository
                .save(new Expense(null, "Christmas dinner", 3500.0, LocalDate.of(2025, 12, 25), foodDining, user1));
        expenseRepository
                .save(new Expense(null, "Electricity bill", 2800.0, LocalDate.of(2025, 12, 15), utilities, user1));
        expenseRepository.save(
                new Expense(null, "Movie tickets - Avatar", 800.0, LocalDate.of(2025, 12, 10), entertainment, user1));
        expenseRepository
                .save(new Expense(null, "Fuel for car", 3000.0, LocalDate.of(2025, 12, 5), transportation, user1));

        // November 2025
        expenseRepository
                .save(new Expense(null, "Amazon shopping", 15000.0, LocalDate.of(2025, 11, 28), shopping, user1));
        expenseRepository
                .save(new Expense(null, "Monthly groceries", 8000.0, LocalDate.of(2025, 11, 15), groceries, user1));
        expenseRepository
                .save(new Expense(null, "Spotify annual", 1189.0, LocalDate.of(2025, 11, 1), entertainment, user1));

        // October 2025
        expenseRepository
                .save(new Expense(null, "Diwali shopping", 20000.0, LocalDate.of(2025, 10, 20), shopping, user1));
        expenseRepository
                .save(new Expense(null, "Train tickets", 2500.0, LocalDate.of(2025, 10, 10), transportation, user1));
        expenseRepository
                .save(new Expense(null, "Restaurant dinner", 2200.0, LocalDate.of(2025, 10, 5), foodDining, user1));

        // ============ User 2 (Ashmita) Expenses ============

        // January 2026
        expenseRepository
                .save(new Expense(null, "Fish, eggs, vegetables", 500.0, LocalDate.of(2026, 1, 14), groceries, user2));
        expenseRepository.save(new Expense(null, "Zara clothing", 4500.0, LocalDate.of(2026, 1, 8), shopping, user2));
        expenseRepository.save(new Expense(null, "Ola rides", 800.0, LocalDate.of(2026, 1, 6), transportation, user2));

        // December 2025
        expenseRepository.save(new Expense(null, "Winter jacket", 6000.0, LocalDate.of(2025, 12, 22), shopping, user2));
        expenseRepository
                .save(new Expense(null, "Grocery shopping", 3500.0, LocalDate.of(2025, 12, 18), groceries, user2));
        expenseRepository
                .save(new Expense(null, "Internet bill", 1200.0, LocalDate.of(2025, 12, 10), utilities, user2));
        expenseRepository
                .save(new Expense(null, "Cafe coffee day", 450.0, LocalDate.of(2025, 12, 8), foodDining, user2));
        expenseRepository
                .save(new Expense(null, "Concert tickets", 2500.0, LocalDate.of(2025, 12, 1), entertainment, user2));

        // November 2025
        expenseRepository.save(new Expense(null, "Book purchase", 1500.0, LocalDate.of(2025, 11, 25), shopping, user2));
        expenseRepository
                .save(new Expense(null, "Weekly groceries", 2000.0, LocalDate.of(2025, 11, 20), groceries, user2));
        expenseRepository.save(
                new Expense(null, "Metro card recharge", 500.0, LocalDate.of(2025, 11, 10), transportation, user2));
        expenseRepository.save(new Expense(null, "Pizza hut", 900.0, LocalDate.of(2025, 11, 5), foodDining, user2));

        // October 2025
        expenseRepository
                .save(new Expense(null, "Diwali sweets", 1500.0, LocalDate.of(2025, 10, 22), groceries, user2));
        expenseRepository
                .save(new Expense(null, "Saree purchase", 8000.0, LocalDate.of(2025, 10, 18), shopping, user2));
        expenseRepository
                .save(new Expense(null, "Mobile recharge", 599.0, LocalDate.of(2025, 10, 1), utilities, user2));

        // Aggregate expenses - updated totals
        // User1 January: 70000+2500+649+1200 = 74349
        aggregateExpenseRepository.save(new AggregateExpense(null, user1, Month.JANUARY, 2026, 74349.0));
        // User1 December: 25000+3500+2800+800+3000 = 35100
        aggregateExpenseRepository.save(new AggregateExpense(null, user1, Month.DECEMBER, 2025, 35100.0));
        // User1 November: 15000+8000+1189 = 24189
        aggregateExpenseRepository.save(new AggregateExpense(null, user1, Month.NOVEMBER, 2025, 24189.0));
        // User1 October: 20000+2500+2200 = 24700
        aggregateExpenseRepository.save(new AggregateExpense(null, user1, Month.OCTOBER, 2025, 24700.0));

        // User2 January: 500+4500+800 = 5800
        aggregateExpenseRepository.save(new AggregateExpense(null, user2, Month.JANUARY, 2026, 5800.0));
        // User2 December: 6000+3500+1200+450+2500 = 13650
        aggregateExpenseRepository.save(new AggregateExpense(null, user2, Month.DECEMBER, 2025, 13650.0));
        // User2 November: 1500+2000+500+900 = 4900
        aggregateExpenseRepository.save(new AggregateExpense(null, user2, Month.NOVEMBER, 2025, 4900.0));
        // User2 October: 1500+8000+599 = 10099
        aggregateExpenseRepository.save(new AggregateExpense(null, user2, Month.OCTOBER, 2025, 10099.0));

    }

}
