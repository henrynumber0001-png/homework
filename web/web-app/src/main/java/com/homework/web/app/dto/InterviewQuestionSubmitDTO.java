package com.homework.web.app.dto;

import lombok.Data;

@Data
public class InterviewQuestionSubmitDTO {
    private Long bankId;
    private Long questionId;
    private String content;
    private Integer timeSpentSeconds;
}
