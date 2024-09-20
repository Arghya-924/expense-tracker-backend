package com.project.expense_tracker_backend;

import com.project.expense_tracker_backend.model.Category;
import com.project.expense_tracker_backend.model.Expense;
import com.project.expense_tracker_backend.model.User;
import com.project.expense_tracker_backend.repository.CategoryRepository;
import com.project.expense_tracker_backend.repository.ExpenseRepository;
import com.project.expense_tracker_backend.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import java.time.LocalDate;

@SpringBootApplication
@AllArgsConstructor
@EnableCaching
public class ExpenseTrackerBackendApplication {

	private UserRepository userRepository;
	private ExpenseRepository expenseRepository;
	private CategoryRepository categoryRepository;

	@PostConstruct
	public void init() {

		Category category1 = new Category(0L, "Groceries");
		Category category2 = new Category(0L, "Shopping");

		category1 = categoryRepository.save(category1);
		category2 = categoryRepository.save(category2);

		User user1 = new User(0L, "Arghya", "arghya924@gmail.com", "12345", "1234456667");
		User user2 = new User(0L, "Ashmita", "dasashmita30@gmail.com", "12345", "1234567890");

		user1 = userRepository.save(user1);
		user2 = userRepository.save(user2);

		Expense expense1 = new Expense(0L, "iPhone", 70000.0, LocalDate.now(), category2, user1);
		Expense expense2 = new Expense(0L, "fish, eggs", 500.0, LocalDate.now(), category1, user2);
		Expense expense3 = new Expense(0L, "iPad", 25000.0, LocalDate.now(), category2, user1);

		expenseRepository.save(expense1);
		expenseRepository.save(expense2);
		expenseRepository.save(expense3);


	}

	public static void main(String[] args) {
		SpringApplication.run(ExpenseTrackerBackendApplication.class, args);
	}

}
