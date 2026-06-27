package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserAuthIdentityStatus {

    PENDING(1, "pending"),
    VERIFIED(2, "verified"),
    DISABLED(3, "disabled"),
    UNLINKED(4, "unlinked");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    UserAuthIdentityStatus(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
