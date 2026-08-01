package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ItemType implements BaseEnum {


    // 构建修正：题库模块图片查询一直使用 MODULE，但枚举缺失；现在补回数据库值 1。
    MODULE(1, "module"),
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
