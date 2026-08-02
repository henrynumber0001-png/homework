package com.homework.web.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 后台统一状态动作结果。 */
@Data
public class ActionResultVO {

   //动作是对谁操作的
    private Long targetId;

    /** 已执行动作。 */
    private Integer action;

    /** 动作后的业务状态。 */
    private Integer status;

    /** 动作后的乐观锁版本。 */
    private Integer version;

    /** 动作完成时间。 */
    private LocalDateTime updatedTime;
}
