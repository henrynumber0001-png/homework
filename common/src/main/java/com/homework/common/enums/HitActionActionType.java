package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum HitActionActionType {

    LIKE(1, "like"),
    FAVORITE(2, "favorite"),
    REPOST(3, "repost");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    HitActionActionType(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
