package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question_bank_question")
public class QuestionBankQuestion extends BaseEntity {

    private Long bankId;

    private Long questionId;

    private Integer priority;

    /** Free user visible question. */
    @TableField("is_free_preview")
    private Boolean freePreview;
}
