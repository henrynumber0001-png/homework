package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hit_comment")
public class HitComment extends BaseEntity {

    private Long postId;

    private Long userId;

    private Long parentId;

    private String content;
}
