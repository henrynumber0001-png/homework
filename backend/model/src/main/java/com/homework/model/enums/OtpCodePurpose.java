package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum OtpCodePurpose implements BaseEnum {

    LOGIN(1, "login"),
    REGISTER(2, "register"),
    BIND_PHONE(3, "bind_phone"),
    RESET_PASSWORD(4, "reset_password");

    @EnumValue
    @JsonValue
    private final Integer code;

    private final String name;

    OtpCodePurpose(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
