package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum AiChatSessionStatus implements BaseEnum {

    ACTIVE(1, "active"),
    CLOSED(2, "closed");

    @EnumValue
    @JsonValue
    private final Integer code;

    private final String name;

    AiChatSessionStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
