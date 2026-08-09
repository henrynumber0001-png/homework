package com.homework.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/** 管理端允许对题目执行的状态动作；该枚举不持久化到数据库。 */
@Getter
public enum QuestionAction implements BaseEnum {

    /** 发布一道草稿或已下架题目。 */
    PUBLISH(1, "publish"),

    /** 下架一道已经发布的题目。 */
    OFFLINE(2, "offline"),

    /** 逻辑删除一道题目。 */
    DELETE(3, "delete");

    /** 前后端接口中传递的固定数字值。 */
    @JsonValue
    private final Integer code;

    /** 供界面、日志和文档使用的动作说明。 */
    private final String name;

    QuestionAction(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
