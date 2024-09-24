package com.project.expense_tracker_backend.service;

import com.project.expense_tracker_backend.constants.ApplicationConstants;
import com.project.expense_tracker_backend.exception.EmailNotFoundException;
import com.project.expense_tracker_backend.exception.UserNotFoundException;
import com.project.expense_tracker_backend.model.User;
import com.project.expense_tracker_backend.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class UserDetailsService {

    private UserRepository userRepository;

    @Cacheable(ApplicationConstants.USER_DETAILS_CACHE_NAME_BY_EMAIL)
    public User loadUserByUsername(String email) throws UsernameNotFoundException {

        log.info("Calling loadUserByUsername with email: {}", email);

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EmailNotFoundException(ApplicationConstants.EMAIL_NOT_FOUND, email));
    }

    @Cacheable(ApplicationConstants.USER_DETAILS_CACHE_NAME_BY_ID)
    public User loadUserById(Long userId) {
        log.info("Calling loadUserById with id: {}", userId);

        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(ApplicationConstants.USER_DOES_NOT_EXIST, userId));
    }

    @CacheEvict(value = ApplicationConstants.USER_DETAILS_CACHE_NAME_BY_EMAIL)
    public void evictUserFromCache(String email) {
        log.info("Evicting user from cache : {}", email);
    }

}
