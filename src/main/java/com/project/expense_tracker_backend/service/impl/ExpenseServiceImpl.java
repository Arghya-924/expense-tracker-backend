package com.project.expense_tracker_backend.service.impl;

import com.project.expense_tracker_backend.constants.ApplicationConstants;
import com.project.expense_tracker_backend.dto.ExpenseRequestDto;
import com.project.expense_tracker_backend.dto.ExpenseResponseDto;
import com.project.expense_tracker_backend.exception.ExpenseNotFoundException;
import com.project.expense_tracker_backend.exception.UserNotFoundException;
import com.project.expense_tracker_backend.mapper.ExpenseMapper;
import com.project.expense_tracker_backend.model.AggregateExpense;
import com.project.expense_tracker_backend.model.Category;
import com.project.expense_tracker_backend.model.Expense;
import com.project.expense_tracker_backend.model.User;
import com.project.expense_tracker_backend.repository.AggregateExpenseRepository;
import com.project.expense_tracker_backend.repository.CategoryRepository;
import com.project.expense_tracker_backend.repository.ExpenseRepository;
import com.project.expense_tracker_backend.repository.UserRepository;
import com.project.expense_tracker_backend.service.IExpenseService;
import com.project.expense_tracker_backend.service.UserDetailsService;
import com.project.expense_tracker_backend.util.BeanUtil;
import com.project.expense_tracker_backend.util.DateUtil;
import com.project.expense_tracker_backend.util.ExpensesUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.*;

@Service
@AllArgsConstructor
@Slf4j
public class ExpenseServiceImpl implements IExpenseService {

    private final UserDetailsService userDetailsService;
    private ExpenseRepository expenseRepository;
    private CategoryRepository categoryRepository;
    private ExpenseMapper expenseMapper;
    private AggregateExpenseRepository aggregateExpenseRepository;

    @Override
    public List<ExpenseResponseDto> getUserExpenses(String yearMonth, long userId) {

        log.info("Fetching expenses for user_id = {} | STARTS", userId);

        // 0th index contains the first date, and 1st index contains the last date of month
        LocalDate[] firstAndLastDateOfMonth = DateUtil.getFirstAndLastDateOfMonth(yearMonth);

        List<Expense> userExpenses = expenseRepository
                .findByUserUserIdAndDateBetween(userId, firstAndLastDateOfMonth[0], firstAndLastDateOfMonth[1]);

        log.info("Fetching expenses for user_id = {} | ENDS", userId);

        return expenseMapper.expenseToExpenseResponseMapper(userExpenses);
    }

    @Override
    public List<ExpenseResponseDto> saveUserExpenses(long userId, List<ExpenseRequestDto> userExpenses) {

        log.info("Saving expenses for user_id = {} | STARTS", userId);

        User user = findUserByUserID(userId);

        List<Expense> savedExpenses = new ArrayList<>();

        Map<YearMonth, Double> aggregatedExpensesPerMonthYear = new HashMap<>();

        for (ExpenseRequestDto userExpense : userExpenses) {

            Category currentCategory = findOrCreateCategory(userExpense.getCategoryName());

            Expense newExpense = expenseMapper.expenseRequestToExpenseMapper(0L, userExpense, currentCategory, user);

            ExpensesUtil.populateExpensePerYearMonthMap(newExpense, aggregatedExpensesPerMonthYear);

            savedExpenses.add(expenseRepository.save(newExpense));
        }

        saveAggregatedExpensePerYearMonth(aggregatedExpensesPerMonthYear, user);

        log.info("Saving expenses for user_id = {} | ENDS", userId);

        return expenseMapper.expenseToExpenseResponseMapper(savedExpenses);
    }

    private void saveAggregatedExpensePerYearMonth(Map<YearMonth, Double> aggregatedExpensesPerMonthYear, User user) {

        log.info("Updating Aggregate Expense for user_id = {} | STARTS", user.getUserId());

        for (Map.Entry<YearMonth, Double> entry : aggregatedExpensesPerMonthYear.entrySet()) {

            YearMonth currentYearMonth = entry.getKey();
            Double aggregateAmount = entry.getValue();

            // if aggregate expense for currentYearMonth exists, then this ID will be updated
            Long aggregateExpenseId = 0L;

            Optional<AggregateExpense> aggregateExpensePerYearMonth =
                    aggregateExpenseRepository.findAggregateExpenseByUserUserIdAndExpenseMonthAndExpenseYear(user.getUserId(), currentYearMonth.getMonth(), currentYearMonth.getYear());

            // if the user already has existing expenses for currentYearMonth, then add the amount to the aggregateAmount
            if (aggregateExpensePerYearMonth.isPresent()) {
                aggregateAmount = aggregateAmount + aggregateExpensePerYearMonth.get().getAmount();
                aggregateExpenseId = aggregateExpensePerYearMonth.get().getId();
            }

            AggregateExpense newAggregateExpense =
                    new AggregateExpense(aggregateExpenseId, user, currentYearMonth.getMonth(), currentYearMonth.getYear(), aggregateAmount);

            // if for the currentYearMonth AggregateExpense exists, then it will update the value, otherwise will create a new entry for the currentYearMonth.
            aggregateExpenseRepository.save(newAggregateExpense);

            log.info("Updating Aggregate Expense for user_id = {} | ENDS", user.getUserId());
        }
    }

    private User findUserByUserID(long userId) {
        return userDetailsService.loadUserById(userId);
    }

    private Category findOrCreateCategory(String categoryName) {

        Optional<Category> categoryOptional = categoryRepository.findByCategoryName(categoryName);

        if (categoryOptional.isPresent()) {
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
        if (existingExpense.getUser().getUserId() != userId) {
            throw new ExpenseNotFoundException(expenseId, userId);
        }

        Expense updatedExpense = new Expense(existingExpense);

        BeanUtil.copyNonNullProperties(expenseRequestDto, updatedExpense);

        if (expenseRequestDto.getCategoryName() != null &&
                !expenseRequestDto.getCategoryName().equals(updatedExpense.getCategory().getCategoryName())) {

            Category category = findOrCreateCategory(expenseRequestDto.getCategoryName());

            updatedExpense.setCategory(category);
        }

        updateAggregateExpense(userId, existingExpense, updatedExpense);

        updatedExpense = expenseRepository.save(updatedExpense);

        return expenseMapper.expenseToExpenseResponseMapper(List.of(updatedExpense)).getFirst();
    }

    private void updateAggregateExpense(long userId, Expense existingExpense, Expense updatedExpense) {

        log.info("updateAggregateExpense | Updating Aggregate Expense for user_id = {} | STARTS", userId);


        YearMonth oldYearMonth = DateUtil.getYearMonth(existingExpense.getDate());
        YearMonth newYearMonth = DateUtil.getYearMonth(updatedExpense.getDate());

        AggregateExpense oldMonthAggregateExpense = aggregateExpenseRepository
                .findAggregateExpenseByUserUserIdAndExpenseMonthAndExpenseYear(userId, oldYearMonth.getMonth(), oldYearMonth.getYear())
                .orElseThrow(() -> new RuntimeException("Aggregate expense not found"));

        if (oldYearMonth.equals(newYearMonth)) {

            Double newAggregateExpenseAmount = oldMonthAggregateExpense.getAmount() - existingExpense.getAmount() + updatedExpense.getAmount();

            oldMonthAggregateExpense.setAmount(newAggregateExpenseAmount);
            aggregateExpenseRepository.save(oldMonthAggregateExpense);
        } else {

            Double oldAggregateAmount = oldMonthAggregateExpense.getAmount() - existingExpense.getAmount();
            oldMonthAggregateExpense.setAmount(oldAggregateAmount);
            aggregateExpenseRepository.save(oldMonthAggregateExpense);

            Long newAggregateExpenseId = 0L;
            Double newAggregateAmount = 0.0;

            Optional<AggregateExpense> optionalNewMonthAggregateExpense = aggregateExpenseRepository
                    .findAggregateExpenseByUserUserIdAndExpenseMonthAndExpenseYear(userId, newYearMonth.getMonth(), newYearMonth.getYear());

            if (optionalNewMonthAggregateExpense.isPresent()) {
                newAggregateExpenseId = optionalNewMonthAggregateExpense.get().getId();
                newAggregateAmount = optionalNewMonthAggregateExpense.get().getAmount();
            }
            newAggregateAmount += updatedExpense.getAmount();

            User user = findUserByUserID(userId);

            AggregateExpense newMonthAggregateExpense =
                    new AggregateExpense(newAggregateExpenseId, user, newYearMonth.getMonth(), newYearMonth.getYear(), newAggregateAmount);

            aggregateExpenseRepository.save(newMonthAggregateExpense);

        }
        log.info("updateAggregateExpense | Updating Aggregate Expense for user_id = {} | ENDS", userId);
    }

    @Override
    public void deleteUserExpense(long expenseId, long userId) {

        Expense existingExpense = expenseRepository.findByExpenseIdAndUserUserId(expenseId, userId)
                .orElseThrow(() -> new ExpenseNotFoundException(expenseId, userId));

        // update the aggregate expense before deleting the expense
        updateAggregateExpenseForDeleteExpense(userId, existingExpense);

        expenseRepository.delete(existingExpense);
    }

    private void updateAggregateExpenseForDeleteExpense(long userId, Expense existingExpense) {

        Month expenseMonth = existingExpense.getDate().getMonth();
        int expenseYear = existingExpense.getDate().getYear();

        AggregateExpense aggregateExpense = aggregateExpenseRepository
                .findAggregateExpenseByUserUserIdAndExpenseMonthAndExpenseYear(userId, expenseMonth, expenseYear)
                .orElseThrow(() -> new RuntimeException("Aggregate expense not found"));

        aggregateExpense.setAmount(aggregateExpense.getAmount() - existingExpense.getAmount());
        aggregateExpenseRepository.save(aggregateExpense);
    }

    @Override
    public Double getTotalMonthlyUserExpense(String yearMonth, long userId) {

        YearMonth expenseYearMonth = DateUtil.getYearMonth(yearMonth);

        log.info("Fetching total expense for Month and Year : {}", expenseYearMonth);

        Optional<AggregateExpense> aggregateMonthlyExpense = aggregateExpenseRepository
                .findAggregateExpenseByUserUserIdAndExpenseMonthAndExpenseYear(
                        userId, expenseYearMonth.getMonth(), expenseYearMonth.getYear());

        if (aggregateMonthlyExpense.isEmpty()) {
            log.warn("No total expense found for Month and Year: {}", expenseYearMonth);
            return null;
        }
        log.info("Fetched total expense for Month and Year : {}", expenseYearMonth);
        return aggregateMonthlyExpense.get().getAmount();
    }
}
