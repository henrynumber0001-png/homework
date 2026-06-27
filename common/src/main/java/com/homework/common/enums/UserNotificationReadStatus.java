package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserNotificationReadStatus {

    UNREAD(1, "unread"),
    READ(2, "read");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    UserNotificationReadStatus(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
