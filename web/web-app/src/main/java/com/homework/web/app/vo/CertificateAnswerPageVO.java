package com.homework.web.app.vo;

import com.homework.model.enums.QuestionInfoQuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class CertificateAnswerPageVO {
    @Schema(description = "正确选项")
    private List<String> correctAnswer;

    @Schema(description = "答案解析")
    private String analysis;

    private Long questionId;
    private Boolean correct;

}
