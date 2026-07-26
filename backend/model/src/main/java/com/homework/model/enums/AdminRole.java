package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/** 后台管理员角色。 */
@Getter
public enum AdminRole implements BaseEnum {

    SUPER_ADMIN(1, "super_admin"),
    STANDARD_ADMIN(2, "standard_admin");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    AdminRole(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
