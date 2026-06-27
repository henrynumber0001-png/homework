package com.homework.web.app.dto;

import lombok.Data;

@Data
public class HitCommentCreateDTO {
    private Long userId;
    private Long parentId;
    private String content;
}
