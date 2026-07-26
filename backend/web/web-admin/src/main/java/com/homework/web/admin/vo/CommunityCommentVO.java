package com.homework.web.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 后台社区评论治理列表行。 */
@Data
public class CommunityCommentVO {

    /** 评论 ID。 */
    private Long id;

    /** 所属动态 ID。 */
    private Long postId;

    /** 评论作者用户 ID。 */
    private Long userId;

    /** 评论作者昵称。 */
    private String displayName;

    /** 父评论 ID。 */
    private Long parentCommentId;

    /** 完整评论正文。 */
    private String content;

    /** 治理状态名称。 */
    private String status;

    /** 点赞数。 */
    private Integer likeCount;

    /** 发布时间。 */
    private LocalDateTime createdTime;

    /** 乐观锁版本。 */
    private Integer version;
}
