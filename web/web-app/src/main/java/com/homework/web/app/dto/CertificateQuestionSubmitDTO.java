package com.homework.web.app.dto;

import com.homework.model.enums.QuestionInfoQuestionType;
import lombok.Data;

import java.util.List;

@Data
public class CertificateQuestionSubmitDTO {
    private Long bankId;
    private Long questionId;
    private QuestionInfoQuestionType questionType;
    private List<String> chosonOptions;
    private Integer timeSpentSeconds;
}
