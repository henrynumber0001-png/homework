package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum OtpCodePurpose {

    LOGIN(1, "login"),
    REGISTER(2, "register"),
    BIND_PHONE(3, "bind_phone"),
    RESET_PASSWORD(4, "reset_password");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    OtpCodePurpose(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
