package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/** 会员台账变更来源。 */
@Getter
public enum MembershipChangeType implements BaseEnum {

    ADMIN_GRANT(1, "admin_grant"),
    ADMIN_SUSPEND(2, "admin_suspend"),
    ADMIN_RESUME(3, "admin_resume"),
    ADMIN_REVOKE(4, "admin_revoke");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    MembershipChangeType(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
