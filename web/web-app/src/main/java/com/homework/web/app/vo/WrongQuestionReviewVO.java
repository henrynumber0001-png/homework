package com.homework.web.app.vo;

import com.homework.model.enums.QuestionInfoQuestionType;
import com.homework.web.app.dto.AiEvaluationResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WrongQuestionReviewVO {
    private Long questionId; //虽然请求参数里已经有，但一个完整的详情响应最好能够标识自己对应哪道题

    private String title;

    private List<String> options;

    private QuestionInfoQuestionType questionType;

    private String imageUrl;

    private List<String> chosenOptions;

    @Schema(description = "正确选项")
    private List<String> correctAnswer;

    @Schema(description = "答案解析")
    private String analysis;

    private AiEvaluationResult aiResult;

    private String content;

    private LocalDateTime answeredTime;
}
