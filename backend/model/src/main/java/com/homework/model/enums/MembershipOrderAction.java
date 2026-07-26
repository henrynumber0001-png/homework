package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum MembershipOrderAction implements BaseEnum {

    FULL_PURCHASE(1, "full_purchase"),
    DIFF_UPGRADE(2, "diff_upgrade");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    MembershipOrderAction(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
