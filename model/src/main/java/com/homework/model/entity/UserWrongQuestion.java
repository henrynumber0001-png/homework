package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_wrong_question")
public class UserWrongQuestion extends BaseEntity {

    private Long userId;

    private Long questionId;

    private Integer wrongCount;
}
