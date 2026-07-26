package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/** 普通管理员可访问的题库数据范围。 */
@Getter
public enum BankDataScope implements BaseEnum {

    ALL_BANKS(1, "all_banks"),
    ASSIGNED_BANKS(2, "assigned_banks");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    BankDataScope(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
