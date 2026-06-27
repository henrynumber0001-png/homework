package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum PrivateMessageAllowReason {

    MUTUAL_FOLLOW(1, "mutual_follow"),
    FIRST_NON_MUTUAL_MESSAGE(2, "first_non_mutual_message");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    PrivateMessageAllowReason(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
