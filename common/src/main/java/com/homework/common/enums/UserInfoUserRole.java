package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserInfoUserRole {

    USER(1, "user"),
    ADMIN(2, "admin");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    UserInfoUserRole(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
