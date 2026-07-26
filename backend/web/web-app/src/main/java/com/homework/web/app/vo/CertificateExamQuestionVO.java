package com.homework.web.app.vo;

import com.homework.model.enums.QuestionInfoQuestionType;
import lombok.Data;

import java.util.List;
@Data
public class CertificateExamQuestionVO {
    private Long questionId;
    private String title;
    private List<String> options;
    private QuestionInfoQuestionType questionType;
    private String imageUrl;
    private List<String> chosenOptions; // 只恢复用户选择，不返回正确答案
    private Boolean answered;
    private Boolean isFavorite;
}
