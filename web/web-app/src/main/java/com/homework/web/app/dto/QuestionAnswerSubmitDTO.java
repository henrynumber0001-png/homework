package com.homework.web.app.dto;

import lombok.Data;

@Data
public class QuestionAnswerSubmitDTO {
    private Long userId;
    private Long sessionId;
    private Long questionId;
    /** 1.text;2.single_choice;3.multiple_choice */
    private Integer answerType;
    private String answerText;
    private String selectedOptionsJson;
    private Integer timeSpentSeconds;
}
