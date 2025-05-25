package com.project.expense_tracker_backend.constants;

public class ApplicationConstants {

    public static final String USER_DOES_NOT_EXIST = "User with userId %s does not exists";
    public static final String REQUEST_USER_ID_ATTRIBUTE = "userId";
    public static final String EMAIL_NOT_FOUND = "User with email %s does not exists";
    public static final String BAD_CREDENTIALS = "Incorrect password provided";
    public static final String JWT_SUBJECT = "Expense Tracker App";
    public static final String JWT_AUTH_HEADER = "Authorization";
    public static final String STATUS_FAILURE = "Failed";
    public static final String STATUS_SUCCESS = "Success";
    public static final String USER_REGISTRATION_SUCCESSFUL = "User successfully registered";
    public static final String LOGIN_USER_API_PATH = "/public/login";
    public static final String REGISTER_USER_API_PATH = "/public/register";
    public static final String EMAIL_ALREADY_EXISTS = "%s email already exists";
    public static final String YEAR_MONTH_NOT_VALID = "%s is not a valid year-month. Required Format : yyyy-mm";
    public static final String EXPENSE_NOT_FOUND = "Expense with expense id : %s, does not exists";
    public static final String EXPENSE_USER_NOT_MATCH = "Expense with expense id : %s, does not belong to user with user id : %s";
    public static final String USER_DETAILS_CACHE_NAME_BY_EMAIL = "user_details_email";
    public static final String USER_DETAILS_CACHE_NAME_BY_ID = "user_details_id";
    public static final String JWT_CACHE_NAME = "jwt_cache";
    public static final String PASSWORD_CHANGED = "Password changed successfully";
    public static final String JWT_COOKIE_NAME = "jwtToken";
    private ApplicationConstants() {
    }


}
