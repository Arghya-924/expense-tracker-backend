package com.project.expense_tracker_backend.service;

import com.project.expense_tracker_backend.dto.LoginRequestDto;
import com.project.expense_tracker_backend.dto.LoginResponseDto;
import com.project.expense_tracker_backend.dto.UserRegistrationDto;

public interface ILoginService {

    LoginResponseDto loginUserAndGenerateToken(LoginRequestDto loginRequestDto);

    void registerNewUser(UserRegistrationDto userDetails);
}
