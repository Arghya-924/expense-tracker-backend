package com.project.expense_tracker_backend.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.project.expense_tracker_backend.constants.ApplicationConstants;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.index.qual.NonNegative;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public Cache<Object, Object> userDetailsCacheByEmail() {
        return Caffeine.newBuilder().maximumSize(10L)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .build();
    }

    @Bean
    public Cache<Object, Object> userDetailsCacheById() {
        return Caffeine.newBuilder().maximumSize(50L).expireAfterAccess(30, TimeUnit.MINUTES).build();
    }

    @Bean
    public Cache<Object, Object> jwtCache() {
        return Caffeine.newBuilder().expireAfter(new JwtAuthTokenExpiry()).build();
    }

    @Bean
    public CacheManager cacheManager() {

        CaffeineCacheManager cacheManager =
                new CaffeineCacheManager(
                        ApplicationConstants.USER_DETAILS_CACHE_NAME_BY_EMAIL,
                        ApplicationConstants.USER_DETAILS_CACHE_NAME_BY_ID,
                        ApplicationConstants.JWT_CACHE_NAME);

        cacheManager.registerCustomCache(ApplicationConstants.USER_DETAILS_CACHE_NAME_BY_EMAIL, userDetailsCacheByEmail());
        cacheManager.registerCustomCache(ApplicationConstants.USER_DETAILS_CACHE_NAME_BY_ID, userDetailsCacheById());
        cacheManager.registerCustomCache(ApplicationConstants.JWT_CACHE_NAME, jwtCache());

        return cacheManager;
    }

    static class JwtAuthTokenExpiry implements Expiry<Object, Object> {

        @Override
        public long expireAfterCreate(Object key, Object value, long currentTime) {

            JwtGenerator.TokenPair tokenPair = (JwtGenerator.TokenPair) value;

            // Get the expiration time in milliseconds
            long expirationTimeMillis = tokenPair.expiration().getTime();

            // Convert the current time from nanoseconds to milliseconds
            long currentTimeMillis = System.currentTimeMillis();

            // Calculate the duration until expiration in nanoseconds
            long durationMillis = expirationTimeMillis - currentTimeMillis;

            log.info("Expire after create : {} ms", durationMillis);

            // Convert the duration from milliseconds to nanoseconds
            return TimeUnit.MILLISECONDS.toNanos(durationMillis);
        }

        @Override
        public long expireAfterUpdate(Object key, Object value, long currentTime, @NonNegative long currentDuration) {
            return currentDuration;
        }

        @Override
        public long expireAfterRead(Object key, Object value, long currentTime, @NonNegative long currentDuration) {
            log.info("Expire after read : {} ns", currentDuration);
            return currentDuration;
        }
    }
}
