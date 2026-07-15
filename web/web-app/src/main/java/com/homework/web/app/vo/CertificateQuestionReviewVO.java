package com.homework.web.app.vo;

import com.homework.model.enums.QuestionInfoQuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class CertificateQuestionReviewVO {

    private Long questionId;

    @Schema(description = "题目标题")
    private String title;

    private List<String> options;

    private QuestionInfoQuestionType questionType;

    private String imageUrl;

    //浏览器如果刷新恢复，后端 review 接口就需要返回 content 给前端，告诉前端，用户的选择是什么
    private List<String> chosonOptions;

    @Schema(description = "正确选项")
    private List<String> correctAnswer;

    @Schema(description = "答案解析")
    private String analysis;

    private Boolean isCorrect;

    private Boolean isFavorite;
}
