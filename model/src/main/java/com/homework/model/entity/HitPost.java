package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.common.enums.HitPostPostStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hit_post")
public class HitPost extends BaseEntity {

    private Long userId;

    private String content;

    private String tagsJson;

    /** 1.published;2.hidden;3.deleted */
    private HitPostPostStatus postStatus;

    private Integer commentCount;

    private Integer likeCount;

    private Integer favoriteCount;

    private Integer repostCount;
}
