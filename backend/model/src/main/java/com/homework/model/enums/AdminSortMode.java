package com.homework.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/** 管理端题库和题目列表支持的排序模式；该枚举不持久化到数据库。 */
@Getter
public enum AdminSortMode implements BaseEnum {

    /** 按最后更新时间降序，较新记录优先。 */
    UPDATED_TIME_DESC(1, "updated_time_desc"),

    /** 按题库人工曝光权重降序，较高权重优先。 */
    SORT_ORDER_DESC(2, "sort_order_desc"),

    /** 按题库内题目序号升序。 */
    QUESTION_NO_ASC(3, "question_no_asc");

    /** 前后端接口中传递的固定数字值。 */
    @JsonValue
    private final Integer code;

    /** 供日志、文档和调试使用的英文说明。 */
    private final String name;

    AdminSortMode(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
