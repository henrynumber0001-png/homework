package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum OtpCodeStatus implements BaseEnum {

    ACTIVE(1, "active"),
    CONSUMED(2, "consumed"),
    EXPIRED(3, "expired"),
    BLOCKED(4, "blocked");

    @EnumValue
    @JsonValue
    private final Integer code;

    private final String name;

    OtpCodeStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
