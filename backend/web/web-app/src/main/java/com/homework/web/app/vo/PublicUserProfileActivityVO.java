package com.homework.web.app.vo;

import lombok.Data;
import java.time.LocalDateTime;

/** 展示 PublicUserProfile 下方四个分类列表中的一条活动。 */
@Data
public class PublicUserProfileActivityVO {
    /** POST、REPOST、COMMENT、LIKED_POST、LIKED_COMMENT 或 FAVORITE。 */
    private String activityType;
    /** 活动发生时间，用于列表排序。 */
    private LocalDateTime activityTime;
    /** 关联 Post 摘要。 */
    private HitPostVO post;
    /** 关联 Comment 摘要；非 Comment 活动为空。 */
    private HitCommentVO comment;
}
