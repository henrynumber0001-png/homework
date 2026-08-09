package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum MedalInfoStatus implements BaseEnum {

    ACTIVE(1, "active"),
    DISABLED(2, "disabled");

    @EnumValue
    @JsonValue
    private final Integer code;

    private final String name;

    MedalInfoStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
