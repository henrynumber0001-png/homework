package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum BillingType implements BaseEnum {

    MONTHLY(1, "monthly"),
    QUARTERLY(2, "quarterly"),
    YEARLY(3, "yearly");

    @EnumValue
    @JsonValue
    private final Integer code;

    private final String name;

    BillingType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
