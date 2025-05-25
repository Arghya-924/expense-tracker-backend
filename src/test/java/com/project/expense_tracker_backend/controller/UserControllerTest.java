package com.project.expense_tracker_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.expense_tracker_backend.constants.ApplicationConstants;
import com.project.expense_tracker_backend.filter.JwtValidatorFilter;
import com.project.expense_tracker_backend.security.UsernamePasswordAuthenticationProvider;
import com.project.expense_tracker_backend.service.ILoginService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.HttpServletRequest;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ILoginService loginService;

    @MockBean
    private JwtValidatorFilter jwtValidatorFilter; // To prevent it from interfering

    @MockBean
    private UsernamePasswordAuthenticationProvider usernamePasswordAuthenticationProvider; // To prevent it from interfering

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void changePassword_success() throws Exception {
        // Mock HttpServletRequest
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getAttribute(ApplicationConstants.REQUEST_USER_ID_ATTRIBUTE)).thenReturn("1");

        // Mock loginService behavior
        doNothing().when(loginService).changeUserPassword(anyString(), anyLong());

        // Perform POST request
        mockMvc.perform(post("/user/changePass")
                        .requestAttr(ApplicationConstants.REQUEST_USER_ID_ATTRIBUTE, "1") // Simulate attribute being set
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"newPassword123\"")) // Send new password as a JSON string
                .andExpect(status().isOk())
                .andExpect(content().string(ApplicationConstants.PASSWORD_CHANGED));

        // Verify service call
        verify(loginService, times(1)).changeUserPassword("newPassword123", 1L);
    }

    @Test
    void changePassword_serviceThrowsException() throws Exception {
        // Mock HttpServletRequest
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getAttribute(ApplicationConstants.REQUEST_USER_ID_ATTRIBUTE)).thenReturn("1");

        // Mock loginService to throw an exception
        doThrow(new RuntimeException("User not found")).when(loginService).changeUserPassword(anyString(), anyLong());

        // Perform POST request
        mockMvc.perform(post("/user/changePass")
                        .requestAttr(ApplicationConstants.REQUEST_USER_ID_ATTRIBUTE, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"newPassword123\""))
                .andExpect(status().isInternalServerError()); // Assuming GlobalExceptionHandler handles this as 500

        // Verify service call
        verify(loginService, times(1)).changeUserPassword("newPassword123", 1L);
    }

    @Test
    void changePassword_missingUserIdAttribute() throws Exception {
        // Perform POST request without the user ID attribute
        // The controller will try to call Long.parseLong(null) or similar if request.getAttribute returns null
        mockMvc.perform(post("/user/changePass")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"newPassword123\""))
                .andExpect(status().isInternalServerError()); // Expecting a 500 due to NullPointerException/NumberFormatException

        // Verify that loginService was NOT called
        verify(loginService, never()).changeUserPassword(anyString(), anyLong());
    }
}
