package com.homework.web.app.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** Hit 评论及评论作者的展示信息。 */
@Data
public class HitCommentVO {
    private Long id;
    private Long postId;
    private Long userId;
    private String displayName;
    private String avatar;
    private Long parentId;
    private String content;
    private LocalDateTime createdTime;
}
