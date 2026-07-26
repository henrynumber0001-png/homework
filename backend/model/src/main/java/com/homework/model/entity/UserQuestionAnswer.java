package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.QuestionInfoQuestionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "user_question_answer", autoResultMap = true)
public class UserQuestionAnswer extends BaseEntity {

    //知道用户做的题属于哪一个题库
    private Long bankId;

    private Long userId;

    private Long questionId;

    private QuestionInfoQuestionType questionType;

    @Schema(description = "用户输入的回答")
    private String content;

    @Schema(description = "用户选择的选项")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> chosenOptions;

    @Schema(description = "用户选项是否正确")
    private Boolean isCorrect;

    @Schema(description = "面试简答题的AI评分")
    private BigDecimal aiScoreRate;

    private LocalDateTime answeredTime;
}
