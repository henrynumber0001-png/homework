package com.homework.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/** 超级管理员允许对普通管理员账号执行的状态动作；该枚举不持久化到数据库。 */
@Getter
public enum AdminAccountAction implements BaseEnum {

    /** 停用一个当前有效的普通管理员账号。 */
    DISABLE(1, "disable"),

    /** 重新启用一个已停用的普通管理员账号。 */
    ACTIVATE(2, "activate"),

    /** 归档一个普通管理员账号并结束其使用周期。 */
    ARCHIVE(3, "archive");

    /** 前后端接口中传递的固定数字值。 */
    @JsonValue
    private final Integer value;

    /** 供界面、日志和文档使用的动作说明。 */
    private final String label;

    AdminAccountAction(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
