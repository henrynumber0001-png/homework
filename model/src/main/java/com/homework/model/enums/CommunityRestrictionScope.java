package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/** 用户社区功能限制范围。 */
@Getter
public enum CommunityRestrictionScope implements BaseEnum {

    POST(1, "post"),
    COMMENT(2, "comment"),
    BOTH(3, "both");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    CommunityRestrictionScope(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
