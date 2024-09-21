package com.project.expense_tracker_backend.config;

import com.project.expense_tracker_backend.constants.ApplicationConstants;
import com.project.expense_tracker_backend.exception.EmailNotFoundException;
import com.project.expense_tracker_backend.model.User;
import com.project.expense_tracker_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        String email = authentication.getPrincipal().toString();

        Optional<User> registeredUser = userRepository.findByEmail(email);

        if (registeredUser.isEmpty()) {
            throw new EmailNotFoundException(ApplicationConstants.EMAIL_NOT_FOUND, email);
        }

        String encryptedPassword = registeredUser.get().getPassword();

        if (passwordEncoder.matches(authentication.getCredentials().toString(), encryptedPassword)) {

            return new UsernamePasswordAuthenticationToken(email, null);
        } else {
            throw new BadCredentialsException(ApplicationConstants.BAD_CREDENTIALS);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
