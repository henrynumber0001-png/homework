package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum SortType implements BaseEnum{

    HOT(1,"热度"),
    LATEST(2,"最新");

    @EnumValue
    @JsonValue
    private Integer value;
    private String label;

    SortType(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
