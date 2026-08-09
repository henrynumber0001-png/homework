package com.homework.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/** 管理端允许对 App 用户账号执行的状态动作；该枚举不持久化到数据库。 */
@Getter
public enum UserAccountAction implements BaseEnum {

    /** 临时停用一个正常账号。 */
    DISABLE(1, "disable"),

    /** 将停用账号恢复为正常状态。 */
    ACTIVATE(2, "activate"),

    /** 封禁一个正常或已停用账号。 */
    BAN(3, "ban"),

    /** 解除封禁并恢复账号。 */
    UNBAN(4, "unban");

    /** 前后端接口中传递的固定数字值。 */
    @JsonValue
    private final Integer code;

    /** 供界面、日志和文档使用的动作说明。 */
    private final String name;

    UserAccountAction(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
