package com.project.expense_tracker_backend.controller;

import com.project.expense_tracker_backend.constants.ApplicationConstants;
import com.project.expense_tracker_backend.service.ILoginService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/user")
@AllArgsConstructor
public class UserController {

    private ILoginService loginService;

    @PostMapping("/changePass")
    public ResponseEntity<String> changePassword(@RequestBody String newPassword, HttpServletRequest request) {

        long userId = Long.parseLong(request.getAttribute(ApplicationConstants.REQUEST_USER_ID_ATTRIBUTE).toString());

        log.info("Password change request received for user id : {}", userId);

        loginService.changeUserPassword(newPassword, userId);

        log.info("Password successfully changed for user id : {}", userId);

        return ResponseEntity.ok(ApplicationConstants.PASSWORD_CHANGED);
    }
}
