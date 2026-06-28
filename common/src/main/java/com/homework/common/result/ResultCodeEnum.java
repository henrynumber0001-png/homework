package com.homework.common.result;


import lombok.Getter;

@Getter
public enum ResultCodeEnum {

    SUCCESS(200, "success"),
    FAIL(500, "failed"),

    PARAM_ERROR(202, "参数不正确"),
    SERVICE_ERROR(203, "服务异常"),
    DATA_ERROR(204, "数据异常"),
    SYSTEM_ERROR(205, "系统异常"),
    REPEAT_SUBMIT(206, "重复提交"),


    APP_LOGIN_NOT_AUTH(501, "未登陆"),
    APP_LOGIN_EMAIL_EMPTY(502, "邮箱为空"),
    APP_LOGIN_EMAIL_EXIST(511,"邮箱已存在"),
    APP_LOGIN_PASSWORD_EMPTY(503, "密码为空"),
    APP_LOGIN_PASSWORD_CONFIRM_ERROR(504, "密码不一致"),
    APP_LOGIN_TURNSTILE_VERIFY_ERROR(505, "人机验证失败，请重试"),
    APP_LOGIN_DISPLAY_NAME_EMPTY(506,"昵称为空"),



    APP_LOGIN_PHONE_EMPTY(504, "手机号码为空"),
    APP_LOGIN_CODE_EMPTY(505, "验证码为空"),
    APP_SEND_SMS_TOO_OFTEN(506, "验证法发送过于频繁"),
    APP_LOGIN_CODE_EXPIRED(507, "验证码已过期"),
    APP_LOGIN_CODE_ERROR(508, "验证码错误"),
    APP_ACCOUNT_DISABLED_ERROR(509, "该用户已被禁用"),
    APP_LOGIN_USER_NOT_EXIST(510, "用户不存在"),



    TOKEN_EXPIRED(601, "token过期"),
    TOKEN_INVALID(602, "token非法");

    private final Integer code;
    private final String message;

    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
