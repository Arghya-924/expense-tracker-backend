package com.project.expense_tracker_backend.config;

import com.project.expense_tracker_backend.exception.EmailNotFoundException;
import com.project.expense_tracker_backend.model.User;
import com.project.expense_tracker_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class CustomAuthenticationProviderTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomAuthenticationProvider authenticationProvider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSuccessfulAuthentication() {

        Authentication mockAuthentication =
                new UsernamePasswordAuthenticationToken("test@test.com", "encrypted");

        Optional<User> mockUser = getUser();

        when(userRepository.findByEmail(mockAuthentication.getPrincipal().toString()))
                .thenReturn(mockUser);

        when(passwordEncoder.matches(mockAuthentication.getCredentials().toString(), mockUser.get().getPassword()))
                .thenReturn(true);

        Authentication authentication = authenticationProvider.authenticate(mockAuthentication);

        assertNotNull(authentication);

        assertEquals("test@test.com", authentication.getPrincipal().toString());
        assertNull(authentication.getCredentials());

    }

    @Test
    void testInvalidUsername() {
        Authentication mockAuthentication =
                new UsernamePasswordAuthenticationToken("test@test.com", "encrypted");

        Optional<User> mockUser = Optional.empty();

        when(userRepository.findByEmail(mockAuthentication.getPrincipal().toString()))
                .thenReturn(mockUser);

        assertThrows(EmailNotFoundException.class, () -> {
            authenticationProvider.authenticate(mockAuthentication);
        });

    }

    @Test
    void testInvalidPassword() {
        Authentication mockAuthentication =
                new UsernamePasswordAuthenticationToken("test@test.com", "wrongPassword");

        Optional<User> mockUser = getUser();

        when(userRepository.findByEmail(mockAuthentication.getPrincipal().toString()))
                .thenReturn(mockUser);

        when(passwordEncoder.matches(mockAuthentication.getCredentials().toString(), mockUser.get().getPassword()))
                .thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> {
            authenticationProvider.authenticate(mockAuthentication);
        });
    }

    private Optional<User> getUser() {
        return Optional.of(new User(1L, "test", "test@test.com", "encrypted", "1234567890"));
    }
}
