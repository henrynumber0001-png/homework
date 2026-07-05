package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.QuestionInfoQuestionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_question_answer")
public class UserQuestionAnswer extends BaseEntity {

    private Long userId;

    private Long questionId;

    private QuestionInfoQuestionType questionType;

    @Schema(description = "用户输入的回答")
    private String content;

    @TableField("is_correct")
    private Boolean isCorrect;

    private BigDecimal aiScoreRate;

    private Integer timeSpentSeconds;

    private LocalDateTime answeredTime;
}
