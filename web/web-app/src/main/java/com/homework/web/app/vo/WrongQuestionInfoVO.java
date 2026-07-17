package com.homework.web.app.vo;

import com.homework.model.enums.QuestionInfoQuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class WrongQuestionInfoVO {
    private Long questionId;

    @Schema(description = "题目标题")
    private String title;

    private List<String> options;

    private QuestionInfoQuestionType questionType;

    private String imageUrl;

    //认证题库的用户选项
    private List<String> chosonOptions;

    //认证题库的正确选项
    @Schema(description = "正确选项")
    private List<String> correctAnswer;

    //面试题的用户输入内容
    @Schema(description = "用户输入的回答")
    private String content;


    @Schema(description = "答案解析")
    private String analysis;
}
