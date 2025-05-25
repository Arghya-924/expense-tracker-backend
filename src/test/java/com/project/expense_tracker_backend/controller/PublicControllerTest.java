package com.project.expense_tracker_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.expense_tracker_backend.constants.ApplicationConstants;
import com.project.expense_tracker_backend.dto.LoginRequestDto;
import com.project.expense_tracker_backend.dto.LoginResponseDto;
import com.project.expense_tracker_backend.dto.UserRegistrationDto;
import com.project.expense_tracker_backend.exception.EmailNotFoundException;
import com.project.expense_tracker_backend.service.ILoginService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicController.class)
public class PublicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ILoginService loginService;

    @Autowired
    private ObjectMapper objectMapper;

    // --- Tests for /public/register ---

    @Test
    void registerUser_success() throws Exception {
        UserRegistrationDto userDto = new UserRegistrationDto("Test User", "test@example.com", "password123");
        doNothing().when(loginService).registerNewUser(any(UserRegistrationDto.class));

        mockMvc.perform(post("/public/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isCreated())
                .andExpect(content().string(ApplicationConstants.USER_REGISTRATION_SUCCESSFUL));

        verify(loginService, times(1)).registerNewUser(any(UserRegistrationDto.class));
    }

    @Test
    void registerUser_emailAlreadyExists() throws Exception {
        UserRegistrationDto userDto = new UserRegistrationDto("Test User", "test@example.com", "password123");
        String expectedErrorMessage = String.format(ApplicationConstants.EMAIL_ALREADY_EXISTS, userDto.getEmail());

        doThrow(new DataIntegrityViolationException(expectedErrorMessage))
                .when(loginService).registerNewUser(any(UserRegistrationDto.class));

        MvcResult result = mockMvc.perform(post("/public/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isConflict()) // Assuming GlobalExceptionHandler handles this as 409
                .andReturn();

        // Check if the exception thrown by the controller is DataIntegrityViolationException
        // and has the correct message from the controller's catch block.
        // The PublicController itself re-throws new DataIntegrityViolationException(message)
        assertTrue(result.getResolvedException() instanceof DataIntegrityViolationException);
        assertEquals(expectedErrorMessage, result.getResolvedException().getMessage());

        verify(loginService, times(1)).registerNewUser(any(UserRegistrationDto.class));
    }


    @Test
    void registerUser_invalidInput_blankEmail() throws Exception {
        UserRegistrationDto userDto = new UserRegistrationDto("Test User", "", "password123"); // Blank email

        mockMvc.perform(post("/public/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email", containsString("Email cannot be blank"))); // Adjust field name and message as per DTO validation
    }

    @Test
    void registerUser_invalidInput_shortPassword() throws Exception {
        UserRegistrationDto userDto = new UserRegistrationDto("Test User", "test@example.com", "123"); // Short password

        mockMvc.perform(post("/public/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password", containsString("Password must be at least 8 characters long"))); // Adjust field name and message
    }

    // --- Tests for /public/login ---

    @Test
    void loginUser_success() throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto("test@example.com", "password123");
        LoginResponseDto loginResponse = new LoginResponseDto("jwt-token-123", "Test User", "test@example.com");

        when(loginService.loginUserAndGenerateToken(any(LoginRequestDto.class))).thenReturn(loginResponse);

        mockMvc.perform(post("/public/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwtToken").value("jwt-token-123"))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.username").value("test@example.com"));

        verify(loginService, times(1)).loginUserAndGenerateToken(any(LoginRequestDto.class));
    }

    @Test
    void loginUser_emailNotFound() throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto("unknown@example.com", "password123");
        String errorMessage = "Email not found: " + loginRequest.getUsername();
        when(loginService.loginUserAndGenerateToken(any(LoginRequestDto.class)))
                .thenThrow(new EmailNotFoundException(errorMessage));

        MvcResult result = mockMvc.perform(post("/public/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isNotFound()) // Assuming GlobalExceptionHandler maps EmailNotFoundException to 404
                .andReturn();

        assertTrue(result.getResolvedException() instanceof EmailNotFoundException);
        assertEquals(errorMessage, result.getResolvedException().getMessage());

        verify(loginService, times(1)).loginUserAndGenerateToken(any(LoginRequestDto.class));
    }

    @Test
    void loginUser_badCredentials() throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto("test@example.com", "wrongpassword");
        String errorMessage = "Bad credentials for user: " + loginRequest.getUsername();
        when(loginService.loginUserAndGenerateToken(any(LoginRequestDto.class)))
                .thenThrow(new BadCredentialsException(errorMessage));

        MvcResult result = mockMvc.perform(post("/public/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized()) // Assuming GlobalExceptionHandler maps BadCredentialsException to 401
                .andReturn();
        
        assertTrue(result.getResolvedException() instanceof BadCredentialsException);
        assertEquals(errorMessage, result.getResolvedException().getMessage());

        verify(loginService, times(1)).loginUserAndGenerateToken(any(LoginRequestDto.class));
    }

    @Test
    void loginUser_invalidInput_blankUsername() throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto("", "password123"); // Blank username

        mockMvc.perform(post("/public/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username", containsString("Username cannot be blank"))); // Adjust as per DTO validation
    }

    @Test
    void loginUser_invalidInput_blankPassword() throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto("test@example.com", ""); // Blank password

        mockMvc.perform(post("/public/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password", containsString("Password cannot be blank"))); // Adjust as per DTO validation
    }
}
