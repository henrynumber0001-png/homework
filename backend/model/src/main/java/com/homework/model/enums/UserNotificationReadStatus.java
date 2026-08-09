package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserNotificationReadStatus implements BaseEnum {

    UNREAD(1, "unread"),
    READ(2, "read");

    @EnumValue
    @JsonValue
    private final Integer code;

    private final String name;

    UserNotificationReadStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
