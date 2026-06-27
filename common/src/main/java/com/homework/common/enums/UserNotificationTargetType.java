package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserNotificationTargetType {

    HIT_POST(1, "hit_post"),
    HIT_COMMENT(2, "hit_comment"),
    QUESTION(3, "question"),
    BANK(4, "bank"),
    USER(5, "user");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    UserNotificationTargetType(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
