package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum PrivateMessageMessageStatus {

    SENT(1, "sent"),
    READ(2, "read"),
    BLOCKED(3, "blocked");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    PrivateMessageMessageStatus(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
