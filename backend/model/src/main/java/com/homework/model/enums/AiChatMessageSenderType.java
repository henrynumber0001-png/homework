package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum AiChatMessageSenderType implements BaseEnum {

    USER(1, "user"),
    AI(2, "ai");

    @EnumValue
    @JsonValue
    private final Integer code;

    private final String name;

    AiChatMessageSenderType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
