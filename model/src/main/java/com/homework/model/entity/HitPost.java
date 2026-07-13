package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.HitPostStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Hit 学习打卡动态。
 *
 * <p>互动数量保存在动态表中，避免公共时间线每次都对互动表做聚合统计。
 * 业务层通过原子 SQL 修改计数，保证高并发点赞时不会丢失更新。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hit_post")
public class HitPost extends BaseEntity {

    /** 发布者用户 ID。 */
    private Long postUserId;

    /** 打卡正文，业务规则限制为 1～140 个 Unicode 字符。 */
    private String content;

    /** 标签的 JSON 数组，例如 ["React", "Hooks"]。 */
    private String tagsJson;

    /** 1.published; 2.hidden; 3.deleted。 */
    private HitPostStatus postStatus;

    private Integer commentCount;

    private Integer likeCount;

    private Integer favoriteCount;

    private Integer repostCount;
}
