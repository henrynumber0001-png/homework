package com.homework.web.app.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 个人中心热力图下方的一条 Hit 活动。 */
@Data
public class UserCenterActivityVO {
    /** POST、REPOST、COMMENT、LIKED_POST、LIKED_COMMENT 或 FAVORITE。 */
    private String activityType;
    private LocalDateTime activityTime;
    private HitPostVO post;
    private HitCommentVO comment;
}
