package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum MembershipStatus implements BaseEnum {

    FREE(0, "free"),
    PREMIUM(1, "premium"),
    PREMIUM_PLUS(2, "premium_plus");

    @EnumValue
    @JsonValue
    private final Integer code;

    private final String name;

    MembershipStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
