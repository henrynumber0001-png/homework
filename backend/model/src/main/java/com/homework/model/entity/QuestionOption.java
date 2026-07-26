package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question_option")
public class QuestionOption extends BaseEntity {

    private Long questionId;

    /** A/B/C/D... */
    private String optionKey;

    private String optionContent;

    @TableField("is_correct")
    private Boolean correct;

    private Integer sortOrder;
}
