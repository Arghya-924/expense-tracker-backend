package com.project.expense_tracker_backend.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.project.expense_tracker_backend.constants.ApplicationConstants;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

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
    public CacheManager cacheManager() {

        CaffeineCacheManager cacheManager =
                new CaffeineCacheManager(
                        ApplicationConstants.USER_DETAILS_CACHE_NAME_BY_EMAIL,
                        ApplicationConstants.USER_DETAILS_CACHE_NAME_BY_ID);

        cacheManager.registerCustomCache(ApplicationConstants.USER_DETAILS_CACHE_NAME_BY_EMAIL, userDetailsCacheByEmail());
        cacheManager.registerCustomCache(ApplicationConstants.USER_DETAILS_CACHE_NAME_BY_ID, userDetailsCacheById());

        return cacheManager;
    }
}
