package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserInfoStatus implements BaseEnum {

    ACTIVE(1, "active"),
    DISABLED(2, "disabled"),
    BANNED(3, "banned");

    @EnumValue
    @JsonValue
    private final Integer code;

    private final String name;

    UserInfoStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
