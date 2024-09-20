package com.project.expense_tracker_backend.service.impl;

import com.project.expense_tracker_backend.constants.ApplicationConstants;
import com.project.expense_tracker_backend.data.DatabaseUtil;
import com.project.expense_tracker_backend.dto.ExpenseRequestDto;
import com.project.expense_tracker_backend.dto.ExpenseResponseDto;
import com.project.expense_tracker_backend.exception.ExpenseNotFoundException;
import com.project.expense_tracker_backend.exception.UserNotFoundException;
import com.project.expense_tracker_backend.mapper.ExpenseMapper;
import com.project.expense_tracker_backend.model.Category;
import com.project.expense_tracker_backend.model.Expense;
import com.project.expense_tracker_backend.model.User;
import com.project.expense_tracker_backend.repository.CategoryRepository;
import com.project.expense_tracker_backend.repository.ExpenseRepository;
import com.project.expense_tracker_backend.repository.UserRepository;
import com.project.expense_tracker_backend.service.IExpenseService;
import com.project.expense_tracker_backend.util.BeanUtil;
import com.project.expense_tracker_backend.util.DateUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class ExpenseServiceImpl implements IExpenseService {

    private ExpenseRepository expenseRepository;
    private CategoryRepository categoryRepository;
    private UserRepository userRepository;
    private ExpenseMapper expenseMapper;
    private DatabaseUtil databaseUtil;

    @Override
    public List<ExpenseResponseDto> getUserExpenses(String yearMonth, long userId) {

        log.info("Fetching expenses for user_id = {} | start", userId);

        // 0th index contains the first date, and 1st index contains the last date of month
        LocalDate[] firstAndLastDateOfMonth = DateUtil.getFirstAndLastDateOfMonth(yearMonth);

//        List<Expense> userExpenses = databaseUtil.fetchExpenses(userId, newdate1, newdate2);

        List<Expense> userExpenses = expenseRepository
                .findByUserUserIdAndDateBetween(userId, firstAndLastDateOfMonth[0], firstAndLastDateOfMonth[1]);

        log.info("Fetching expenses for user_id = {} | end", userId);

        return expenseMapper.expenseToExpenseResponseMapper(userExpenses);
    }

    @Override
    public List<ExpenseResponseDto> saveUserExpenses(long userId, List<ExpenseRequestDto> userExpenses) {

        User user = findUserByUserID(userId);

        List<Expense> savedExpenses = new ArrayList<>();

        for(ExpenseRequestDto userExpense : userExpenses) {

            Category currentCategory = findOrCreateCategory(userExpense.getCategoryName());

            Expense newExpense = expenseMapper.expenseRequestToExpenseMapper(0L, userExpense, currentCategory, user);

            savedExpenses.add(expenseRepository.save(newExpense));
        }

        return expenseMapper.expenseToExpenseResponseMapper(savedExpenses);
    }

    private User findUserByUserID(long userId) {
        Optional<User> optionalUser = userRepository.findById(userId);

        if(optionalUser.isEmpty()) {
            throw new UserNotFoundException(ApplicationConstants.USER_DOES_NOT_EXIST, userId);
        }
        return optionalUser.get();
    }

    private Category findOrCreateCategory(String categoryName) {

        Optional<Category> categoryOptional = categoryRepository.findByCategoryName(categoryName);

        if(categoryOptional.isPresent()) {
            return categoryOptional.get();
        }

        Category newCategory = new Category(0L, categoryName);

        return categoryRepository.save(newCategory);
    }

    @Override
    public ExpenseResponseDto updateUserExpense(long userId, long expenseId, ExpenseRequestDto expenseRequestDto) {

        Expense existingExpense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ExpenseNotFoundException(expenseId));

        // expense does not belong to logged-in user
        if(existingExpense.getUser().getUserId() != userId) {
            throw new ExpenseNotFoundException(expenseId, userId);
        }

        BeanUtil.copyNonNullProperties(expenseRequestDto, existingExpense);

        if(expenseRequestDto.getCategoryName() != null &&
                ! expenseRequestDto.getCategoryName().equals(existingExpense.getCategory().getCategoryName())) {

            Category category = findOrCreateCategory(expenseRequestDto.getCategoryName());

            existingExpense.setCategory(category);
        }

        Expense updatedExpense =  expenseRepository.save(existingExpense);

        return expenseMapper.expenseToExpenseResponseMapper(List.of(updatedExpense)).getFirst();
    }

    @Override
    public void deleteUserExpense(long expenseId, long userId) {

        Expense existingExpense = expenseRepository.findByExpenseIdAndUserUserId(expenseId, userId)
                .orElseThrow(() -> new ExpenseNotFoundException(expenseId, userId));

        expenseRepository.delete(existingExpense);
    }
}
