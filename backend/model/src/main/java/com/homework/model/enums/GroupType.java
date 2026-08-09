package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum GroupType implements BaseEnum {

    INTERVIEW(1, "interview"),
    CERTIFICATION(2, "certification");

    @EnumValue
    @JsonValue
    private final Integer code;

    private final String name;

    GroupType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
