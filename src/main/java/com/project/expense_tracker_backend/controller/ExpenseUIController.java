package com.project.expense_tracker_backend.controller;

import com.project.expense_tracker_backend.constants.ApplicationConstants;
import com.project.expense_tracker_backend.dto.ExpenseResponseDto;
import com.project.expense_tracker_backend.dto.UserExpensesResponse;
import com.project.expense_tracker_backend.exception.YearMonthParseException;
import com.project.expense_tracker_backend.service.IExpenseService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.format.DateTimeParseException;
import java.util.List;

// Add these imports
import com.project.expense_tracker_backend.dto.ExpenseRequestDto;
import com.project.expense_tracker_backend.exception.UserNotFoundException;
// import org.springframework.validation.BindingResult; // For @Valid
// import jakarta.validation.Valid; // For @Valid

@Controller
@RequestMapping("/ui") // Base path for authenticated UI operations
@AllArgsConstructor
@Slf4j
public class ExpenseUIController {

    private final IExpenseService expenseService;

    @GetMapping("/expenses/view")
    public String showExpensesPage(@RequestParam(required = false) String yearMonth,
                                   HttpServletRequest request, Model model,
                                   RedirectAttributes redirectAttributes) {
        try {
            Object userIdAttribute = request.getAttribute(ApplicationConstants.REQUEST_USER_ID_ATTRIBUTE);
            if (userIdAttribute == null) {
                log.warn("User ID not found in request attributes. Redirecting to login.");
                redirectAttributes.addFlashAttribute("errorMessage", "Authentication error. Please login again.");
                return "redirect:/public/ui/login";
            }

            Long userId;
            try {
                userId = Long.parseLong(userIdAttribute.toString());
            } catch (NumberFormatException e) {
                log.error("ExpenseUIController | showExpensesPage | Could not parse userId from attribute: {}", userIdAttribute, e);
                redirectAttributes.addFlashAttribute("errorMessage", "Authentication error (User ID format). Please login again.");
                return "redirect:/public/ui/login";
            }

            List<ExpenseResponseDto> userExpenses = expenseService.getUserExpenses(yearMonth, userId);
            Double totalMonthlyExpense = expenseService.getTotalMonthlyUserExpense(yearMonth, userId);

            model.addAttribute("userExpensesResponse", new UserExpensesResponse<>(userExpenses, totalMonthlyExpense));
            model.addAttribute("currentYearMonth", yearMonth); // For repopulating filter
            model.addAttribute("pageTitle", "My Expenses");

            return "ui/expenses"; // Path to the template: templates/ui/expenses.html
        } catch (DateTimeParseException dateTimeParseException) {
            log.error("ExpenseUIController | showExpensesPage | Exception for yearMonth {}: {}", yearMonth, dateTimeParseException.getMessage());
            model.addAttribute("errorMessage", String.format(ApplicationConstants.YEAR_MONTH_NOT_VALID, yearMonth));
            model.addAttribute("userExpensesResponse", new UserExpensesResponse<>(List.of(), 0.0)); // Empty response
            model.addAttribute("pageTitle", "My Expenses");
            model.addAttribute("currentYearMonth", yearMonth);
            return "ui/expenses";
        } catch (YearMonthParseException ympe) { // Catching from service if it throws this directly
            log.error("ExpenseUIController | showExpensesPage | YearMonthParseException for yearMonth {}: {}", yearMonth, ympe.getMessage());
            model.addAttribute("errorMessage", ympe.getMessage());
            model.addAttribute("userExpensesResponse", new UserExpensesResponse<>(List.of(), 0.0));
            model.addAttribute("pageTitle", "My Expenses");
            model.addAttribute("currentYearMonth", yearMonth);
            return "ui/expenses";
        }
         catch (Exception e) {
            log.error("ExpenseUIController | showExpensesPage | Unexpected error: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "An unexpected error occurred while fetching expenses.");
            model.addAttribute("userExpensesResponse", new UserExpensesResponse<>(List.of(), 0.0));
            model.addAttribute("pageTitle", "My Expenses");
            model.addAttribute("currentYearMonth", yearMonth);
            return "ui/expenses";
        }
    }

    @GetMapping("/expenses/add")
    public String showAddExpensePage(Model model) {
        if (!model.containsAttribute("expenseRequestDto")) {
            model.addAttribute("expenseRequestDto", new ExpenseRequestDto());
        }
        model.addAttribute("pageTitle", "Add New Expense");
        return "ui/add-expense";
    }

    @PostMapping("/expenses/save")
    public String saveExpense(@ModelAttribute("expenseRequestDto") /* @Valid */ ExpenseRequestDto expenseDto,
                              // BindingResult bindingResult, // Uncomment if using @Valid
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        
        Object userIdAttribute = request.getAttribute(ApplicationConstants.REQUEST_USER_ID_ATTRIBUTE);
        if (userIdAttribute == null) {
            log.warn("User ID not found in request attributes for saveExpense. Redirecting to login.");
            redirectAttributes.addFlashAttribute("errorMessage", "Authentication error. Please login again.");
            return "redirect:/public/ui/login";
        }

        Long userId;
        try {
            userId = Long.parseLong(userIdAttribute.toString());
        } catch (NumberFormatException e) {
            log.error("ExpenseUIController | saveExpense | Could not parse userId from attribute: {}", userIdAttribute, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Authentication error (User ID format). Please login again.");
            return "redirect:/public/ui/login";
        }

        /* // Uncomment if using @Valid
        if (bindingResult.hasErrors()) {
            log.warn("Validation errors for user {}: {}", userId, bindingResult.getAllErrors());
            model.addAttribute("pageTitle", "Add New Expense"); // Keep DTO as is (already in model via @ModelAttribute)
            model.addAttribute("errorMessage", "Please correct the errors below."); // Generic error
            return "ui/add-expense"; // Return to form to show errors
        }
        */

        try {
            expenseService.saveUserExpenses(userId, List.of(expenseDto));
            redirectAttributes.addFlashAttribute("successMessage", "Expense added successfully!");
            return "redirect:/ui/expenses/view";
        } catch (UserNotFoundException e) {
            log.error("ExpenseUIController | saveExpense | UserNotFoundException for user {}: {}", userId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Could not save expense: User not found.");
            redirectAttributes.addFlashAttribute("expenseRequestDto", expenseDto); // Send DTO back
            return "redirect:/ui/expenses/add";
        } catch (Exception e) {
            log.error("ExpenseUIController | saveExpense | Exception for user {}: {}", userId, e.getMessage(), e);
            // For generic errors, showing on the form page itself might be better
            model.addAttribute("pageTitle", "Add New Expense");
            model.addAttribute("errorMessage", "An unexpected error occurred while saving the expense.");
            // model still contains 'expenseRequestDto' due to @ModelAttribute
            return "ui/add-expense";
        }
    }

    @GetMapping("/expenses/edit/{expenseId}")
    public String showEditExpensePage(@PathVariable Long expenseId,
                                      HttpServletRequest request, Model model,
                                      RedirectAttributes redirectAttributes) {
        
        Object userIdAttribute = request.getAttribute(ApplicationConstants.REQUEST_USER_ID_ATTRIBUTE);
        if (userIdAttribute == null) {
            log.warn("User ID not found in request attributes for showEditExpensePage. Redirecting to login.");
            redirectAttributes.addFlashAttribute("errorMessage", "Authentication error. Please login again.");
            return "redirect:/public/ui/login";
        }
        Long userId;
        try {
            userId = Long.parseLong(userIdAttribute.toString());
        } catch (NumberFormatException e) {
            log.error("ExpenseUIController | showEditExpensePage | Could not parse userId from attribute: {}", userIdAttribute, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Authentication error (User ID format). Please login again.");
            return "redirect:/public/ui/login";
        }

        try {
            ExpenseResponseDto expenseToEdit = expenseService.getUserExpenses(null, userId)
                .stream()
                .filter(e -> e.getExpenseId().equals(expenseId))
                .findFirst()
                .orElseThrow(() -> new ExpenseNotFoundException("Expense not found or not owned by user with ID: " + expenseId));

            ExpenseRequestDto formDto = new ExpenseRequestDto(
                expenseToEdit.getDescription(),
                expenseToEdit.getAmount(),
                expenseToEdit.getDate(),
                expenseToEdit.getCategoryName()
            );

            model.addAttribute("expenseDto", formDto); 
            model.addAttribute("actualExpenseId", expenseId); 
            model.addAttribute("pageTitle", "Edit Expense");
            return "ui/edit-expense";

        } catch (ExpenseNotFoundException e) {
            log.warn("ExpenseUIController | showEditExpensePage | Expense not found for id {}: {}", expenseId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Expense not found or you don't have permission to edit it.");
            return "redirect:/ui/expenses/view";
        } catch (Exception e) {
            log.error("ExpenseUIController | showEditExpensePage | Error loading expense id {}: {}", expenseId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error loading expense for editing.");
            return "redirect:/ui/expenses/view";
        }
    }

    @PostMapping("/expenses/update/{expenseId}")
    public String updateExpense(@PathVariable Long expenseId,
                                @ModelAttribute("expenseDto") /* @Valid */ ExpenseRequestDto expenseDetails,
                                // BindingResult bindingResult, // If using @Valid
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        
        Object userIdAttribute = request.getAttribute(ApplicationConstants.REQUEST_USER_ID_ATTRIBUTE);
        if (userIdAttribute == null) {
            log.warn("User ID not found in request attributes for updateExpense. Redirecting to login.");
            redirectAttributes.addFlashAttribute("errorMessage", "Authentication error. Please login again.");
            return "redirect:/public/ui/login";
        }
        Long userId;
        try {
            userId = Long.parseLong(userIdAttribute.toString());
        } catch (NumberFormatException e) {
            log.error("ExpenseUIController | updateExpense | Could not parse userId from attribute: {}", userIdAttribute, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Authentication error (User ID format). Please login again.");
            return "redirect:/public/ui/login";
        }
        
        /* // If using @Valid
        if (bindingResult.hasErrors()) {
            log.warn("Validation errors for expense update {}: {}", expenseId, bindingResult.getAllErrors());
            model.addAttribute("pageTitle", "Edit Expense");
            model.addAttribute("actualExpenseId", expenseId); // For th:action
            // expenseDto is already in model via @ModelAttribute
            return "ui/edit-expense";
        }
        */

        try {
            expenseService.updateUserExpense(userId, expenseId, expenseDetails);
            redirectAttributes.addFlashAttribute("successMessage", "Expense updated successfully!");
            return "redirect:/ui/expenses/view";
        } catch (ExpenseNotFoundException e) {
            log.warn("ExpenseUIController | updateExpense | ExpenseNotFoundException for id {}: {}", expenseId, e.getMessage());
            model.addAttribute("pageTitle", "Edit Expense");
            model.addAttribute("errorMessage", "Could not update: " + e.getMessage());
            model.addAttribute("actualExpenseId", expenseId); 
            // expenseDto is already in model
            return "ui/edit-expense";
        } catch (UserNotFoundException e) { 
             log.warn("ExpenseUIController | updateExpense | UserNotFoundException for user {}: {}", userId, e.getMessage());
             model.addAttribute("pageTitle", "Edit Expense");
             model.addAttribute("errorMessage", "Could not update: User not found or invalid.");
             model.addAttribute("actualExpenseId", expenseId);
             return "ui/edit-expense";
        }
        catch (Exception e) {
            log.error("ExpenseUIController | updateExpense | Error updating expense id {}: {}", expenseId, e.getMessage(), e);
            model.addAttribute("pageTitle", "Edit Expense");
            model.addAttribute("errorMessage", "An unexpected error occurred while updating the expense.");
            model.addAttribute("actualExpenseId", expenseId);
            return "ui/edit-expense";
        }
    }

    @GetMapping("/expenses/delete/{expenseId}")
    public String deleteExpense(@PathVariable Long expenseId,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        
        Object userIdAttribute = request.getAttribute(ApplicationConstants.REQUEST_USER_ID_ATTRIBUTE);
        if (userIdAttribute == null) {
            log.warn("User ID not found in request attributes for deleteExpense. Redirecting to login.");
            redirectAttributes.addFlashAttribute("errorMessage", "Authentication error. Please login again.");
            return "redirect:/public/ui/login";
        }
        Long userId;
        try {
            userId = Long.parseLong(userIdAttribute.toString());
        } catch (NumberFormatException e) {
            log.error("ExpenseUIController | deleteExpense | Could not parse userId from attribute: {}", userIdAttribute, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Authentication error (User ID format). Please login again.");
            return "redirect:/public/ui/login";
        }

        try {
            expenseService.deleteUserExpense(expenseId, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Expense deleted successfully!");
        } catch (ExpenseNotFoundException e) {
            log.warn("ExpenseUIController | deleteExpense | ExpenseNotFoundException for id {}: {}", expenseId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Could not delete expense: " + e.getMessage());
        } catch (Exception e) {
            log.error("ExpenseUIController | deleteExpense | Error deleting expense id {}: {}", expenseId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred while deleting the expense.");
        }
        return "redirect:/ui/expenses/view";
    }
    
    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirectAttributes) {
        Cookie cookie = new Cookie(ApplicationConstants.JWT_COOKIE_NAME, null); // Clear value
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        // cookie.setSecure(true); // In production
        cookie.setMaxAge(0); // Expire immediately
        response.addCookie(cookie);

        redirectAttributes.addFlashAttribute("successMessage", "You have been logged out successfully.");
        return "redirect:/public/ui/login";
    }
}
