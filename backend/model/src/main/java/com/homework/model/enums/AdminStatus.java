package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/** 后台管理员账号状态。 */
@Getter
public enum AdminStatus implements BaseEnum {

    INVITED(1, "invited"),
    ACTIVE(2, "active"),
    DISABLED(3, "disabled"),
    ARCHIVED(4, "archived");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    AdminStatus(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
