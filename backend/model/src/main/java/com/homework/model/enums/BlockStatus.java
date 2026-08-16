package com.homework.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum BlockStatus implements BaseEnum {

    ACTIVATE(1,"activate block"),
    DEACTIVATE(2,"deactivate block");

    @JsonValue
    private final Integer code;
    private final String name;

    BlockStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
