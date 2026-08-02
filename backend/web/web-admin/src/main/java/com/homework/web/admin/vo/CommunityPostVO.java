package com.homework.web.admin.vo;

import com.homework.model.enums.HitPostStatus;
import lombok.Data;

import java.time.LocalDateTime;

/** 后台社区动态治理列表行。 */
@Data
public class CommunityPostVO {

    /** 动态 ID。 */
    private Long id;

    /** 作者用户 ID。 */
    private Long userId;

    /** 作者昵称。 */
    private String displayName;

    /** 完整动态正文。 */
    private String content;

    /** 标签 JSON。 */
    private String tagsJson;

    /** 治理状态名称。 */
    private HitPostStatus status;

    /** 有效评论数。 */
    private Integer commentCount;

    /** 点赞数。 */
    private Integer likeCount;

    /** 收藏数。 */
    private Integer favoriteCount;

    /** 转发数。 */
    private Integer repostCount;

    /** 发布时间。 */
    private LocalDateTime createdTime;

    /** 乐观锁版本。 */
    private Integer version;
}
