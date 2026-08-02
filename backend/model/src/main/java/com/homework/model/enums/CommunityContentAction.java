package com.homework.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/** 管理端允许对社区动态或评论执行的治理动作；该枚举不持久化到数据库。 */
@Getter
public enum CommunityContentAction implements BaseEnum {

    /** 隐藏当前公开展示的社区内容。 */
    HIDE(1, "hide"),

    /** 将隐藏或已删除的社区内容恢复为公开状态。 */
    RESTORE(2, "restore"),

    /** 将社区内容标记为已删除。 */
    DELETE(3, "delete");

    /** 前后端接口中传递的固定数字值。 */
    @JsonValue
    private final Integer value;

    /** 供界面、日志和文档使用的动作说明。 */
    private final String label;

    CommunityContentAction(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
