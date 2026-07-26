package com.homework.web.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 后台统一状态动作结果。 */
@Data
public class ActionResultVO {

    /** 被操作资源 ID。 */
    private Long targetId;

    /** 已执行动作。 */
    private String action;

    /** 动作后的业务状态。 */
    private String status;

    /** 动作后的乐观锁版本。 */
    private Integer version;

    /** 动作完成时间。 */
    private LocalDateTime updatedTime;
}
