package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserNotificationSendTo implements BaseEnum {

    HIT_POST(1, "hit_post"),
    HIT_COMMENT(2, "hit_comment"),
    QUESTION(3, "question"),
    BANK(4, "bank"),
    USER(5, "user"),
    PRIVATE_MESSAGE(6, "private_message");

    @EnumValue
    @JsonValue
    private final Integer code;

    private final String name;

    UserNotificationSendTo(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
