package com.homework.web.app.vo;

import com.homework.model.enums.QuestionInfoQuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class InterviewQuestionPageVO {
    private Long questionId;

    private Long bankId;

    @Schema(description = "题目标题")
    private String title;

    private Integer SortOder;

    private QuestionInfoQuestionType questionType;

    private boolean isReleased;
}
