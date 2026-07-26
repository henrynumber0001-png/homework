package com.homework.web.app.dto;

import lombok.Data;

import java.util.List;

@Data
public class HitPostCreateDTO {
    /** 发布者从 JWT 登录上下文获取，客户端不能自行指定 userId。 */
    private String content;
    private List<String> tags;
    private List<Long> mentionedUserIds;
}
