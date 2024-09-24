package com.project.expense_tracker_backend.service.impl;

import com.project.expense_tracker_backend.config.JwtGenerator;
import com.project.expense_tracker_backend.dto.LoginRequestDto;
import com.project.expense_tracker_backend.dto.LoginResponseDto;
import com.project.expense_tracker_backend.exception.EmailNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class LoginServiceImplTest {

    @Mock
    private JwtGenerator jwtGenerator;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private LoginServiceImpl loginService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void successfulUserLogin() {

        LoginRequestDto mockRequestDto = new LoginRequestDto("arghya", "12345");

        Authentication mockAuth =
                new UsernamePasswordAuthenticationToken(mockRequestDto.getEmail(), mockRequestDto.getPassword());
        Authentication returnAuth =
                new UsernamePasswordAuthenticationToken(mockRequestDto.getEmail(), null);

        when(authenticationManager.authenticate(mockAuth))
                .thenReturn(returnAuth);

        when(jwtGenerator.generateToken(returnAuth)).thenReturn(new JwtGenerator.TokenPair("JWT_TOKEN_GENERATED", new Date()));

        LoginResponseDto response = loginService.loginUserAndGenerateToken(mockRequestDto);

        assertNotNull(response);
        assertEquals("JWT_TOKEN_GENERATED", response.getAuthToken());
    }

    @Test
    void testInvalidUserName() {
        LoginRequestDto mockRequestDto = new LoginRequestDto("arghya", "12345");

        Authentication mockAuth =
                new UsernamePasswordAuthenticationToken(mockRequestDto.getEmail(), mockRequestDto.getPassword());

        when(authenticationManager.authenticate(mockAuth))
                .thenThrow(EmailNotFoundException.class);

        assertThrows(EmailNotFoundException.class, () ->
                loginService.loginUserAndGenerateToken(mockRequestDto));
    }

    @Test
    void testBadCredentials() {
        LoginRequestDto mockRequestDto = new LoginRequestDto("arghya", "12345");

        Authentication mockAuth =
                new UsernamePasswordAuthenticationToken(mockRequestDto.getEmail(), mockRequestDto.getPassword());

        when(authenticationManager.authenticate(mockAuth))
                .thenThrow(BadCredentialsException.class);

        assertThrows(BadCredentialsException.class, () ->
                loginService.loginUserAndGenerateToken(mockRequestDto));
    }
}
