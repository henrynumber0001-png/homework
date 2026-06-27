package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum OtpCodeStatus {

    ACTIVE(1, "active"),
    CONSUMED(2, "consumed"),
    EXPIRED(3, "expired"),
    BLOCKED(4, "blocked");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    OtpCodeStatus(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
