package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum MembershipPurchaseType implements BaseEnum {

    FULL(1, "full"),
    DIFF(2, "diff");

    @EnumValue
    @JsonValue
    private final Integer code;

    private final String name;

    MembershipPurchaseType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
