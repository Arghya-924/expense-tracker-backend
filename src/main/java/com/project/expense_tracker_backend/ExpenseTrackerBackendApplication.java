package com.project.expense_tracker_backend;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.AllArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@AllArgsConstructor
@EnableCaching
@OpenAPIDefinition(security = {@SecurityRequirement(name = "bearerToken")})
@SecurityScheme(name = "bearerToken", type = SecuritySchemeType.HTTP, scheme = "bearer")
public class ExpenseTrackerBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpenseTrackerBackendApplication.class, args);
    }

}
