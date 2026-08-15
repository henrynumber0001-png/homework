package com.homework.web.app.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** listActivities SQL 的基础查询结果，Service 会继续补充 Post 和 Comment 详情。 */
@Data
public class UserCenterActivityRowVO {
    private String activityType;
    private LocalDateTime activityTime;
    private Long activityId;
    private Long postId;
    private Long commentId;
}
