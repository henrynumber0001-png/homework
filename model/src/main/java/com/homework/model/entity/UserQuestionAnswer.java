package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.common.enums.UserQuestionAnswerAnswerType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_question_answer")
public class UserQuestionAnswer extends BaseEntity {

    private Long userId;

    private Long sessionId;

    private Long questionId;

    /** 1.text;2.single_choice;3.multiple_choice */
    private UserQuestionAnswerAnswerType answerType;

    private String answerText;

    private String selectedOptionsJson;

    @TableField("is_correct")
    private Boolean correct;

    private BigDecimal aiScoreRate;

    private Integer timeSpentSeconds;

    private LocalDateTime answeredTime;
}
