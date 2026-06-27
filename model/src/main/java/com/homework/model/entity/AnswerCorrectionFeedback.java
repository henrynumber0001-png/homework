package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.common.enums.AnswerCorrectionFeedbackStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("answer_correction_feedback")
public class AnswerCorrectionFeedback extends BaseEntity {

    private Long userId;

    private Long questionId;

    private Long answerId;

    private String feedbackContent;

    /** 1.pending;2.accepted;3.rejected;4.resolved */
    private AnswerCorrectionFeedbackStatus status;

    private Long adminUserId;

    private String adminReply;
}
