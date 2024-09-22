package com.project.expense_tracker_backend.service;

import com.project.expense_tracker_backend.constants.ApplicationConstants;
import com.project.expense_tracker_backend.exception.EmailNotFoundException;
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

    @Cacheable(ApplicationConstants.USER_DETAILS_CACHE_NAME)
    public User loadUserByUsername(String email) throws UsernameNotFoundException {

        log.info("Calling loadUserByUsername with email: {}", email);

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EmailNotFoundException(ApplicationConstants.EMAIL_NOT_FOUND, email));
    }

    @CacheEvict(value = ApplicationConstants.USER_DETAILS_CACHE_NAME)
    public void evictUserFromCache(String email) {
        log.info("Evicting user from cache : {}", email);
    }

}
