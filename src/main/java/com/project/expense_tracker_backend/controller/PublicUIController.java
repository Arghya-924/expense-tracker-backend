package com.project.expense_tracker_backend.controller;

import com.project.expense_tracker_backend.constants.ApplicationConstants;
import com.project.expense_tracker_backend.dto.LoginRequestDto;
import com.project.expense_tracker_backend.dto.LoginResponseDto;
import com.project.expense_tracker_backend.dto.UserRegistrationDto;
import com.project.expense_tracker_backend.exception.EmailNotFoundException; // Ensure this is imported
import com.project.expense_tracker_backend.service.ILoginService;

import jakarta.servlet.http.Cookie; // Ensure this is imported
import jakarta.servlet.http.HttpServletResponse; // Ensure this is imported

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException; // Ensure this is imported
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/public/ui")
@AllArgsConstructor
@Slf4j
public class PublicUIController {

    private final ILoginService loginService;
    // Assuming JWT_COOKIE_NAME is defined in ApplicationConstants
    // ApplicationConstants.JWT_COOKIE_NAME = "jwtToken"

    @GetMapping("/register")
    public String showRegistrationPage(Model model) {
        if (!model.containsAttribute("userRegistrationDto")) { // Keep previous data on redirect with error
            model.addAttribute("userRegistrationDto", new UserRegistrationDto());
        }
        model.addAttribute("pageTitle", "User Registration");
        return "registration";
    }

    @PostMapping("/register")
    public String handleRegistration(@ModelAttribute("userRegistrationDto") UserRegistrationDto userDetails, RedirectAttributes redirectAttributes) {
        try {
            loginService.registerNewUser(userDetails);
            log.info("PublicUIController | handleRegistration | User registration successful for email: {}", userDetails.getEmail());
            redirectAttributes.addFlashAttribute("successMessage", ApplicationConstants.USER_REGISTRATION_SUCCESSFUL);
            return "redirect:/public/ui/login";
        } catch (DataIntegrityViolationException ex) {
            log.error("PublicUIController | handleRegistration | DataIntegrityViolationException for email {}: {}", userDetails.getEmail(), ex.getMessage());
            String errorMessage = ex.getMessage() != null && ex.getMessage().contains("already exists") ?
                                  ex.getMessage() :
                                  String.format(ApplicationConstants.EMAIL_ALREADY_EXISTS, userDetails.getEmail());
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
            redirectAttributes.addFlashAttribute("userRegistrationDto", userDetails); // Send back for repopulation
            return "redirect:/public/ui/register";
        } catch (Exception ex) {
            log.error("PublicUIController | handleRegistration | Exception during registration for email {}: {}", userDetails.getEmail(), ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred during registration. Please try again.");
            redirectAttributes.addFlashAttribute("userRegistrationDto", userDetails); // Send back for repopulation
            return "redirect:/public/ui/register";
        }
    }

    @GetMapping("/login")
    public String showLoginPage(Model model) {
        if (!model.containsAttribute("loginRequestDto")) { // Keep previous data on redirect with error
             model.addAttribute("loginRequestDto", new LoginRequestDto("", ""));
        }
        model.addAttribute("pageTitle", "User Login");
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(@ModelAttribute("loginRequestDto") LoginRequestDto loginRequestDto,
                              RedirectAttributes redirectAttributes,
                              HttpServletResponse response /* Inject HttpServletResponse */) {
        try {
            LoginResponseDto loginResponse = loginService.loginUserAndGenerateToken(loginRequestDto);
            log.info("PublicUIController | handleLogin | Login successful for email: {}", loginRequestDto.getEmail());

            // Create and set the JWT cookie
            Cookie jwtCookie = new Cookie(ApplicationConstants.JWT_COOKIE_NAME, loginResponse.getJwtToken());
            jwtCookie.setHttpOnly(true);
            jwtCookie.setPath("/"); // Cookie accessible for all paths
            // jwtCookie.setSecure(true); // Uncomment in production if served over HTTPS
            jwtCookie.setMaxAge(24 * 60 * 60); // Expires in 1 day (adjust as needed)
            // jwtCookie.setMaxAge((int) (loginResponse.getExpirationTime() / 1000)); // Alternative: use token expiration

            response.addCookie(jwtCookie);

            redirectAttributes.addFlashAttribute("successMessage", "Login successful!");
            return "redirect:/ui/expenses/view"; // Redirect to a protected page (will be created later)
                                             // For now, can redirect to "/" if expenses page not ready
                                             // return "redirect:/";


        } catch (EmailNotFoundException ex) {
            log.error("PublicUIController | handleLogin | EmailNotFoundException for {}: {}", loginRequestDto.getEmail(), ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Email not found. Please check your email or register.");
            redirectAttributes.addFlashAttribute("loginRequestDto", loginRequestDto); // Send back for repopulation
            return "redirect:/public/ui/login";
        } catch (BadCredentialsException ex) {
            log.error("PublicUIController | handleLogin | BadCredentialsException for {}: {}", loginRequestDto.getEmail(), ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid password. Please try again.");
            redirectAttributes.addFlashAttribute("loginRequestDto", loginRequestDto); // Send back for repopulation
            return "redirect:/public/ui/login";
        } catch (Exception ex) {
            log.error("PublicUIController | handleLogin | Exception during login for {}: {}", loginRequestDto.getEmail(), ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred during login. Please try again.");
            redirectAttributes.addFlashAttribute("loginRequestDto", loginRequestDto); // Send back for repopulation
            return "redirect:/public/ui/login";
        }
    }
}
