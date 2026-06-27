package com.homework.web.app.dto;

import lombok.Data;

import java.util.List;

@Data
public class HitPostCreateDTO {
    private Long userId;
    private String content;
    private List<String> tags;
}
