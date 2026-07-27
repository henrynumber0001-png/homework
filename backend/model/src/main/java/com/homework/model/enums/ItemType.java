package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ItemType implements BaseEnum {


    USER_CENTER_BANNER(2, "user_center_banner");


    @EnumValue
    @JsonValue
    private Integer value;
    private String label;
    ItemType(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
