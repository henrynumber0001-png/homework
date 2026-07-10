package com.homework.web.app.vo;

import com.homework.model.enums.QuestionInfoQuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class CertificateQuestionPageVO {
    private Long questionId;

    @Schema(description = "题目标题")
    private String title;

    private List<String> options;

    private QuestionInfoQuestionType questionType;

    private String imageUrl;
}
