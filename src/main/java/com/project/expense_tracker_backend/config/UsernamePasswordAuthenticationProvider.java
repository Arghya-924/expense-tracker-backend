package com.project.expense_tracker_backend.config;

import com.project.expense_tracker_backend.constants.ApplicationConstants;
import com.project.expense_tracker_backend.model.User;
import com.project.expense_tracker_backend.service.UserDetailsService;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UsernamePasswordAuthenticationProvider implements AuthenticationProvider {

    private final PasswordEncoder passwordEncoder;

    private final UserDetailsService userDetailsService;

    private final CacheManager cacheManager;

    public UsernamePasswordAuthenticationProvider(PasswordEncoder passwordEncoder, UserDetailsService userDetailsService, CacheManager cacheManager) {
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.cacheManager = cacheManager;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getPrincipal().toString();

        User registeredUser = fetchUserFromUserDetailsCache(email);

        boolean cacheCalled = true;

        if (registeredUser == null) {
            cacheCalled = false;
            registeredUser = userDetailsService.loadUserByUsername(email);
        }

        try {
            checkValidAuthentication(authentication, registeredUser);
        } catch (BadCredentialsException exception) {

            if (!cacheCalled) {
                throw new BadCredentialsException(exception.getMessage());
            }

            // evict the user from the cache and retrieve the User from the DB to get the latest password
            userDetailsService.evictUserFromCache(email);
            registeredUser = userDetailsService.loadUserByUsername(email);

            // try to authenticate again, if it still fails, then throw exception
            checkValidAuthentication(authentication, registeredUser);
        }

        return new UsernamePasswordAuthenticationToken(email, null);

    }

    private User fetchUserFromUserDetailsCache(String email) {

        Cache cache = this.cacheManager.getCache(ApplicationConstants.USER_DETAILS_CACHE_NAME);

        if (null == cache) {
            return null;
        }

        return cache.get(email, User.class);
    }

    private void checkValidAuthentication(Authentication authentication, User registeredUser) {
        String encryptedPassword = registeredUser.getPassword();

        if (!passwordEncoder.matches(authentication.getCredentials().toString(), encryptedPassword)) {
            throw new BadCredentialsException(ApplicationConstants.BAD_CREDENTIALS);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
