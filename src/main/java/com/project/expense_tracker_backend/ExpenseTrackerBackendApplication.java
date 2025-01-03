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
import java.time.Year;
import java.util.Date;

@SpringBootApplication
@AllArgsConstructor
@EnableCaching
@OpenAPIDefinition(security = {@SecurityRequirement(name = "bearerToken")})
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

        Category category1 = new Category(null, "Groceries");
        Category category2 = new Category(null, "Shopping");

        category1 = categoryRepository.save(category1);
        category2 = categoryRepository.save(category2);

        User user1 = new User(null, "Arghya", "test1@gmail.com", "$2a$10$AcxxmVRLWBX1cBDitvhBDeqGagTuE3.4VYF7SdE.46hNaq5uzdaKG", "1234456667", LocalDateTime.now(), LocalDateTime.now());
        User user2 = new User(null, "Ashmita", "test2@gmail.com", "$2a$10$yGbXyAkIObl5Om6j6k9/3esTdVwdAsa.OtZ3clFfOs4wsnKnznO5q", "1234567890", LocalDateTime.now(), LocalDateTime.now());

        user1 = userRepository.save(user1);
        user2 = userRepository.save(user2);

        Expense expense1 = new Expense(null, "iPhone", 70000.0, LocalDate.now(), category2, user1);
        Expense expense2 = new Expense(null, "fish, eggs", 500.0, LocalDate.now(), category1, user2);
        Expense expense3 = new Expense(null, "iPad", 25000.0, LocalDate.now(), category2, user1);

        expenseRepository.save(expense1);
        expenseRepository.save(expense2);
        expenseRepository.save(expense3);

        AggregateExpense aggregateExpense1 = new AggregateExpense(null, user1, Month.of(new Date().getMonth() + 1), Year.now().getValue(), (double) 95000);
        AggregateExpense aggregateExpense2 = new AggregateExpense(null, user2, Month.of(new Date().getMonth() + 1), Year.now().getValue(), (double) 500);

        aggregateExpenseRepository.save(aggregateExpense1);
        aggregateExpenseRepository.save(aggregateExpense2);

    }

}
