package com.homework.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/** 管理端允许对题库执行的状态动作；该枚举不持久化到数据库。 */
@Getter
public enum QuestionBankAction implements BaseEnum {

    /** 将草稿或已下架题库发布上线。 */
    PUBLISH(1, "publish"),

    /** 将已发布题库下架。 */
    OFFLINE(2, "offline"),

    /** 逻辑删除草稿或已下架题库。 */
    DELETE(3, "delete");

    /** 前后端接口中传递的固定数字值。 */
    @JsonValue
    private final Integer code;

    /** 供界面、日志和文档使用的动作说明。 */
    private final String name;

    QuestionBankAction(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
