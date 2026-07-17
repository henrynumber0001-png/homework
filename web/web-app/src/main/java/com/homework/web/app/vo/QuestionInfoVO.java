package com.homework.web.app.vo;

import com.homework.model.enums.QuestionInfoQuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class QuestionInfoVO {
    private Long questionId;
    private QuestionInfoQuestionType questionType;

    @Schema(description = "题目标题")
    private String title;

    private List<String> options;

    private String imageUrl;

    @Schema(description = "正确选项")
    private List<String> correctAnswer;

    @Schema(description = "答案解析")
    private String analysis;
}
