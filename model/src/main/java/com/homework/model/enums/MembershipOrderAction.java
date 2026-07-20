package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum MembershipOrderAction implements BaseEnum {

    PURCHASE(1, "purchase"),
    UPGRADE(2, "upgrade"),
    RENEWAL(3, "renewal");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    MembershipOrderAction(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
