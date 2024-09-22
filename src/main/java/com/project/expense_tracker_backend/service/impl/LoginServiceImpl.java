package com.project.expense_tracker_backend.service.impl;

import com.project.expense_tracker_backend.config.JwtGenerator;
import com.project.expense_tracker_backend.constants.ApplicationConstants;
import com.project.expense_tracker_backend.dto.LoginRequestDto;
import com.project.expense_tracker_backend.dto.LoginResponseDto;
import com.project.expense_tracker_backend.dto.UserRegistrationDto;
import com.project.expense_tracker_backend.model.User;
import com.project.expense_tracker_backend.repository.UserRepository;
import com.project.expense_tracker_backend.service.ILoginService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class LoginServiceImpl implements ILoginService {

    private static final Logger log = LoggerFactory.getLogger(LoginServiceImpl.class);

    private JwtGenerator jwtGenerator;

    private AuthenticationManager authenticationManager;

    private UserRepository userRepository;

    @Override
    public LoginResponseDto loginUserAndGenerateToken(LoginRequestDto loginRequestDto) {

        String email = loginRequestDto.getEmail();
        String password = loginRequestDto.getPassword();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        String token = jwtGenerator.generateToken(authentication);

        return new LoginResponseDto(ApplicationConstants.STATUS_SUCCESS, token);
    }

    @Override
    public void registerNewUser(UserRegistrationDto userDetails) {

        User newUser = new User(userDetails);

        userRepository.save(newUser);

    }
}
