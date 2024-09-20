package com.project.expense_tracker_backend.controller;

import com.project.expense_tracker_backend.constants.ApplicationConstants;
import com.project.expense_tracker_backend.dto.LoginRequestDto;
import com.project.expense_tracker_backend.dto.LoginResponseDto;
import com.project.expense_tracker_backend.dto.UserRegistrationDto;
import com.project.expense_tracker_backend.exception.EmailNotFoundException;
import com.project.expense_tracker_backend.service.ILoginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
public class PublicController {

    private static final Logger log = LoggerFactory.getLogger(PublicController.class);
    @Autowired
    private AuthenticationProvider authenticationProvider;

    @Autowired
    private ILoginService loginService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> userLogin(@RequestBody LoginRequestDto loginRequestDto) {

        try{
            LoginResponseDto responseDto = loginService.loginUserAndGenerateToken(loginRequestDto);

            return ResponseEntity.ok(responseDto);
        }
        catch (EmailNotFoundException emailNotFoundException) {

            log.error("userLogin | EmailNotFoundException: {}", emailNotFoundException.getMessage());

            throw emailNotFoundException;
        }
        catch (BadCredentialsException badCredentialsException) {
            log.error("userLogin | BadCredentialsException: {}", badCredentialsException.getLocalizedMessage());

            throw badCredentialsException;
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Object> registerUser(@RequestBody UserRegistrationDto userDetails) {

        try{
            loginService.registerNewUser(userDetails);

            return ResponseEntity.status(HttpStatus.CREATED).body(ApplicationConstants.USER_REGISTRATION_SUCCESSFUL);
        }
        catch (DataIntegrityViolationException ex) {
            log.error("PublicController | registerUser | Exception: {}", ex.getLocalizedMessage());

            throw new DataIntegrityViolationException(
                    String.format(ApplicationConstants.EMAIL_ALREADY_EXISTS, userDetails.getEmail()));
        }
    }
}
