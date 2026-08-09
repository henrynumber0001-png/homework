package com.homework.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/** 管理端允许对用户会员权益执行的动作；该枚举不持久化到数据库。 */
@Getter
public enum MembershipAction implements BaseEnum {

    /** 给用户发放指定类型和时长的会员。 */
    GRANT(1, "grant"),

    /** 暂停用户当前有效的会员访问权限。 */
    SUSPEND(2, "suspend"),

    /** 恢复此前被暂停的会员访问权限。 */
    RESUME(3, "resume"),

    /** 提前收回用户当前有效的会员权益。 */
    REVOKE(4, "revoke");

    /** 前后端接口中传递的固定数字值。 */
    @JsonValue
    private final Integer code;

    /** 供界面、日志和文档使用的动作说明。 */
    private final String name;

    MembershipAction(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
