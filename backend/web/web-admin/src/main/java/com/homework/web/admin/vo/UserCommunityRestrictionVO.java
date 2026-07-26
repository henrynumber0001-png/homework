package com.homework.web.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 用户当前有效的社区权限限制。 */
@Data
public class UserCommunityRestrictionVO {

    /** 限制范围名称。 */
    private String scope;

    /** 限制开始时间。 */
    private LocalDateTime startTime;

    /** 限制结束时间，永久限制时为空。 */
    private LocalDateTime endTime;

    /** 限制原因。 */
    private String reason;
}
