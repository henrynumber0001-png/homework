package com.homework.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Hit 互动指令的目标状态。
 *
 * <p>ACTIVATE 表示执行点赞、收藏或转发；
 * DEACTIVATE 表示取消对应互动。该枚举用于接口参数，不持久化到数据库。</p>
 */
@Getter
public enum ActionStatus implements BaseEnum {

    ACTIVATE(1, "activate"),
    DEACTIVATE(2, "deactivate");

    /** 前后端接口中传递的固定数字值。 */
    @JsonValue
    private final Integer value;

    /** 供界面、日志和文档使用的动作说明。 */
    private final String label;

    ActionStatus(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
