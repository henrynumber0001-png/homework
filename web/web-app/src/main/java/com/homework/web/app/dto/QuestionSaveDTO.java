package com.homework.web.app.dto;

import lombok.Data;

@Data
public class QuestionSaveDTO {
    private Long id;
    private String title;
    private String content;
    private String answer;
    private String analysis;
    private Integer questionType;
    private Integer difficulty;
    private Boolean premium;
    private Integer questionStatus;
    private Long createUserId;
}
