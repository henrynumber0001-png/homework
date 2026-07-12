package com.homework.web.app.dto;

import lombok.Data;

@Data
public class HitCommentCreateDTO {
    /** 评论者从 JWT 登录上下文获取，防止冒充其他用户。 */
    private Long parentId;
    private String content;
}
