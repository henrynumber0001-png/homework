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
    private final Integer value;

    private final String label;

    HitPostStatus(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
