package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question_bank_category")
public class QuestionBankCategory extends BaseEntity {

    private Long questionBankId;

    private Long detailId;

    @TableField("is_primary")
    private Boolean primaryFlag;
}
