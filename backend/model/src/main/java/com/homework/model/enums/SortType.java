package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum SortType implements BaseEnum {

    HOT(1, "hot"),
    LATEST(2, "latest");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String name;

    SortType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
