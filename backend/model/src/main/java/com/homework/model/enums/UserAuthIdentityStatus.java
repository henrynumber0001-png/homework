package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserAuthIdentityStatus implements BaseEnum {

    PENDING(1, "pending"),
    VERIFIED(2, "verified"),
    DISABLED(3, "disabled"),
    UNLINKED(4, "unlinked");

    @EnumValue
    @JsonValue
    private final Integer code;

    private final String name;

    UserAuthIdentityStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
