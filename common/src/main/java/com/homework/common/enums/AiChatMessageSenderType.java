package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum AiChatMessageSenderType {

    USER(1, "user"),
    AI(2, "ai");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    AiChatMessageSenderType(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
