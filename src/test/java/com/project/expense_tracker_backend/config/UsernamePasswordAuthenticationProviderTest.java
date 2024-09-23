package com.project.expense_tracker_backend.config;

import com.project.expense_tracker_backend.constants.ApplicationConstants;
import com.project.expense_tracker_backend.exception.EmailNotFoundException;
import com.project.expense_tracker_backend.model.User;
import com.project.expense_tracker_backend.service.UserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class UsernamePasswordAuthenticationProviderTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private UsernamePasswordAuthenticationProvider authenticationProvider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSuccessfulAuthentication() {

        Authentication mockAuthentication =
                new UsernamePasswordAuthenticationToken("test@test.com", "encrypted");

        User mockUser = new User(0L, "test", "test@test.com", "encrypted", "mobileNumber");

        ConcurrentMapCache cache = new ConcurrentMapCache(ApplicationConstants.USER_DETAILS_CACHE_NAME);
        cache.putIfAbsent("test@test.com", mockUser);

        when(cacheManager.getCache(ApplicationConstants.USER_DETAILS_CACHE_NAME)).thenReturn(cache);
        when(passwordEncoder.matches("encrypted", "encrypted")).thenReturn(true);

        Authentication authentication = authenticationProvider.authenticate(mockAuthentication);

        assertNotNull(authentication);

        assertEquals("test@test.com", authentication.getPrincipal().toString());
        assertNull(authentication.getCredentials());

    }

    @Test
    void testInvalidUsername() {
        Authentication mockAuthentication =
                new UsernamePasswordAuthenticationToken("test@test.com", "encrypted");

        when(userDetailsService.loadUserByUsername("test@test.com"))
                .thenThrow(new EmailNotFoundException(ApplicationConstants.EMAIL_NOT_FOUND, "test@test.com"));

        assertThrows(EmailNotFoundException.class, () -> {
            authenticationProvider.authenticate(mockAuthentication);
        });

    }

    @Test
    void testInvalidPassword() {
        Authentication mockAuthentication =
                new UsernamePasswordAuthenticationToken("test@test.com", "wrongPassword");

        User mockUser = new User(0L, "test", "test@test.com", "wrongPassword", "mobileNumber");

        ConcurrentMapCache cache = new ConcurrentMapCache(ApplicationConstants.USER_DETAILS_CACHE_NAME);
        cache.putIfAbsent("test@test.com", mockUser);

        when(cacheManager.getCache(ApplicationConstants.USER_DETAILS_CACHE_NAME)).thenReturn(cache);
        when(userDetailsService.loadUserByUsername("test@test.com")).thenReturn(mockUser);
        when(passwordEncoder.matches(mockAuthentication.getCredentials().toString(), mockUser.getPassword()))
                .thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> {
            authenticationProvider.authenticate(mockAuthentication);
        });
    }

}
