package com.homework.model.enums;

import lombok.Getter;

@Getter
public enum ItemType implements BaseEnum {

    MODULE(1, "module");


    private Integer value;
    private String label;
    ItemType(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
