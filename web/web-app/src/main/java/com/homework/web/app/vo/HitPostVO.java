package com.homework.web.app.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 展示公共时间线或 PublicUserProfile 中的一条 Hit Post。 */
@Data
public class HitPostVO {
    /** Post ID。 */
    private Long postId;
    /** Post 作者 ID。 */
    private Long userId;
    /** Post 作者展示名。 */
    private String displayName;
    /** Post 作者头像。 */
    private String avatar;
    /** Post 正文。 */
    private String content;
    /** Post 标签。 */
    private List<String> tags;
    /** Comment 数。 */
    private Integer commentCount;
    /** 点赞数。 */
    private Integer likeCount;
    /** 收藏数。 */
    private Integer favoriteCount;
    /** 转发数。 */
    private Integer repostCount;
    /** 当前访问者是否点赞。 */
    private boolean liked;
    /** 当前访问者是否收藏。 */
    private boolean favorited;
    /** 当前访问者是否转发。 */
    private boolean reposted;
    /** Post 创建时间。 */
    private LocalDateTime createdTime;
}
