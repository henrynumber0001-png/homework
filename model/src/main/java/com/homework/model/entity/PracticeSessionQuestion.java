package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.common.enums.PracticeSessionQuestionStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("practice_session_question")
public class PracticeSessionQuestion extends BaseEntity {

    private Long sessionId;

    private Long questionId;

    private Integer questionNo;

    /** 1.not_answered;2.answered;3.correct;4.wrong;5.marked */
    private PracticeSessionQuestionStatus status;
}
