package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum HitPostStatus implements BaseEnum {

    PUBLISHED(1, "published"),
    HIDDEN(2, "hidden"),
    DELETED(3, "deleted");

    @EnumValue
    @JsonValue
    private final Integer code;

    private final String name;

    HitPostStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
