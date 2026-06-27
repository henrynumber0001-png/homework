package com.homework.common.result;


import lombok.Getter;

@Getter
public enum ResultCodeEnum {

    SUCCESS(200, "success"),
    FAIL(500, "failed"),

    PARAM_ERROR(400, "Parameter error"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Resource not found"),

    USER_NOT_FOUND(1001, "User not found"),
    USER_DISABLED(1002, "User disabled"),
    LOGIN_FAILED(1003, "Login failed"),
    TOKEN_EXPIRED(1004, "Token expired"),

    QUESTION_NOT_FOUND(2001, "Question not found"),
    QUESTION_BANK_NOT_FOUND(2002, "Question bank not found"),
    PREMIUM_REQUIRED(3001, "Premium membership required");

    private final Integer code;
    private final String message;

    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
