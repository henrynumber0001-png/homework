package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.common.enums.QuestionInfoDifficulty;
import com.homework.common.enums.QuestionInfoQuestionStatus;
import com.homework.common.enums.QuestionInfoQuestionType;
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

    private String searchText;

    /** 1.single_choice;2.multiple;3.true_false;4.short_answer;5.essay */
    private QuestionInfoQuestionType questionType;

    /** 1.easy;2.medium;3.hard */
    private QuestionInfoDifficulty difficulty;

    private Integer maxAnswerChars;

    @TableField("is_premium")
    private Boolean premium;

    /** 1.draft;2.published;3.archived */
    private QuestionInfoQuestionStatus questionStatus;

    private Long createUserId;
}
