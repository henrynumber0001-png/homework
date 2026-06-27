package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum PremiumOrderPayType {

    WECHAT(1, "wechat"),
    ALIPAY(2, "alipay");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    PremiumOrderPayType(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
