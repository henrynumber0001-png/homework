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

    //注意：hit_comment 是一张 自关联关系表，评论和父评论都在同一张 hit_comment 表中。
    //所以 子评论的 parent_comment_id = 父评论的 id。

    private Long postId;

    //发表评论的人Id（无论是子评论还是父评论，都要有一个真正写comment的人）
    private Long commentUserId;

    //外键：父评论id（父评论的主键id）
    private Long parentCommentId;

    /** 评论正文，当前限制为 1～500 个 Unicode 字符。 */
    private String comment;
}
