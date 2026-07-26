package com.homework.web.app.vo;

import com.homework.model.enums.QuestionInfoQuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class InterviewQuestionPageVO {
    private Long questionId;

    @Schema(description = "题目标题")
    private String title;

    /** 题目图片的一小时只读签名地址。 */
    private String imageUrl;

    private QuestionInfoQuestionType questionType;

    private Boolean isFavorite;
}
