package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_question_note")
public class UserQuestionNote extends BaseEntity {

    private Long userId;

    private Long bankId;

    private Long questionId;

    private String noteContent;

}
