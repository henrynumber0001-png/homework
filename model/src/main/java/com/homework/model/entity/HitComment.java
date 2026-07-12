package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Hit 动态下的评论；parentId 非空时表示回复另一条评论。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hit_comment")
public class HitComment extends BaseEntity {

    private Long postId;

    private Long userId;

    /** 被回复的评论 ID；顶级评论为 null。 */
    private Long parentId;

    /** 评论正文，当前限制为 1～500 个 Unicode 字符。 */
    private String content;
}
