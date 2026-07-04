package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.QuestionInfoQuestionType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question_info")
public class QuestionInfo extends BaseEntity {

    private String title;

    private String content;

    private String answer;

    private String analysis;

    /** 1.single_choice;2.multiple;3.true_false;4.short_answer;5.essay */
    private QuestionInfoQuestionType questionType;

    @TableField("is_premium")
    private Boolean isPremium;

    private boolean isReleased;

    private Long createUserId;
}
