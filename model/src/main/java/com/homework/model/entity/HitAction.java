package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.common.enums.HitActionActionType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hit_action")
public class HitAction extends BaseEntity {

    private Long postId;

    private Long userId;

    /** 1.like;2.favorite;3.repost */
    private HitActionActionType actionType;
}
