package com.homework.web.app.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 展示 Hit 下的一条 Comment 及其作者和点赞状态。 */
@Data
public class HitCommentVO {
    /** Comment ID。 */
    private Long commentId;
    /** 所属 Post ID。 */
    private Long postId;
    /** Comment 作者 ID。 */
    private Long commentUserId;
    /** Comment 作者展示名。 */
    private String displayName;
    /** Comment 作者头像。 */
    private String avatar;
    /** 被回复的 parentComment ID；直接评论 Post 时为空。 */
    private Long parentCommentId;
    /** Comment 正文。 */
    private String comment;
    /** Comment 收到的点赞数。 */
    private Integer likeCount;
    /** 当前访问者是否已点赞该 Comment。 */
    private boolean liked;
    /** Comment 创建时间。 */
    private LocalDateTime createdTime;
}
