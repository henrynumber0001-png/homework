package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 用户对 Hit 评论的点赞记录。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hit_comment_like")
public class HitCommentLike extends BaseEntity {
    private Long commentId;
    private Long actionUserId;
}
