package com.homework.web.app.vo;

import com.homework.model.enums.QuestionInfoQuestionType;
import com.homework.web.app.dto.AiEvaluationResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InterViewAnswerPageVO {


    @Schema(description = "题目ID")
    private Long questionId;

    @Schema(description = "参考答案")
    private String analysis;

    private AiEvaluationResult aiResult;



}
