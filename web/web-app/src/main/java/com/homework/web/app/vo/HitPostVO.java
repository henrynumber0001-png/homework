package com.homework.web.app.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 公共 Hit 时间线返回给前端的动态数据。 */
@Data
public class HitPostVO {
    private Long id;
    private Long userId;
    private String displayName;
    private String avatar;
    private String content;
    private List<String> tags;
    private Integer commentCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer repostCount;
    private boolean liked;
    private boolean favorited;
    private boolean reposted;
    private LocalDateTime createdTime;
}
