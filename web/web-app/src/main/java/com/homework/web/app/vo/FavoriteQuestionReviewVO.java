package com.homework.web.app.vo;

import com.homework.model.enums.QuestionInfoQuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class FavoriteQuestionReviewVO {
    private Long questionId; //虽然请求参数里已经有，但一个完整的详情响应最好能够标识自己对应哪道题

    private String title;

    private QuestionInfoQuestionType questionType;

    private List<String> options;

    /** 题目图片的一小时只读签名地址。 */
    private String imageUrl;

    @Schema(description = "正确选项")
    private List<String> correctAnswer;

    @Schema(description = "答案解析")
    private String analysis;
}
