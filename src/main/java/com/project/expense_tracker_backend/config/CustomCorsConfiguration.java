package com.project.expense_tracker_backend.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

@Component
public class CustomCorsConfiguration implements CorsConfigurationSource {
    @Override
    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:4200", "http://127.0.0.1:4200",
                "http://localhost:3000", "http://127.0.0.1:3000",
                "http://localhost:5000", "http://127.0.0.1:5000",
                "http://localhost:5500", "http://127.0.0.1:5500",
                "http://localhost:8080", "http://127.0.0.1:8080"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("*"));
        return config;
    }
}
