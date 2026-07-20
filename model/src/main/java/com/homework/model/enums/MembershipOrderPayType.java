package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum MembershipOrderPayType implements BaseEnum {

    WECHAT(1, "wechat"),
    ALIPAY(2, "alipay");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    MembershipOrderPayType(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
