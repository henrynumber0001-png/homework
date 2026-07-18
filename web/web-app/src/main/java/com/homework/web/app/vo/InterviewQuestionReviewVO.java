package com.homework.web.app.vo;

import com.homework.model.enums.QuestionInfoQuestionType;
import com.homework.web.app.dto.AiEvaluationResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
//答题卡review
public class InterviewQuestionReviewVO {

    private Long questionId;

    @Schema(description = "题目标题")
    private String title;

    private QuestionInfoQuestionType questionType;

    @Schema(description = "参考答案")
    private String analysis;

    private AiEvaluationResult aiResult;

    private Boolean isFavorite;

    private Boolean isCorrect;

    //浏览器如果刷新恢复，后端 review 接口就需要返回 content 给前端，告诉前端，用户的输入文本是什么
    private String content;

}
