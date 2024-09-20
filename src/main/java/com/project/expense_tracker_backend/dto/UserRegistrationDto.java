package com.project.expense_tracker_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserRegistrationDto {

    private String name;

    private String email;

    private String password;

    private String mobileNumber;
}
