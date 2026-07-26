package com.homework.web.app.vo;

import lombok.Data;
import java.time.LocalDateTime;

/** 承载 PublicUserProfile 活动 SQL 的扁平查询结果，由 Service 组装为页面 VO。 */
@Data
public class PublicUserProfileActivityRowVO {
    /** 活动类型。 */
    private String activityType;
    /** 活动时间。 */
    private LocalDateTime activityTime;
    /** 原创、转发或互动记录的 ID，用于稳定排序。 */
    private Long activityId;
    /** 关联 Post ID。 */
    private Long postId;
    /** 关联 Comment ID；非 Comment 活动为空。 */
    private Long commentId;
}
