package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.common.enums.PracticeSessionSessionMode;
import com.homework.common.enums.PracticeSessionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("practice_session")
public class PracticeSession extends BaseEntity {

    private Long userId;

    private Long bankId;

    /** 1.interview_practice;2.cert_practice;3.cert_exam */
    private PracticeSessionSessionMode sessionMode;

    /** 1.in_progress;2.submitted;3.completed;4.abandoned */
    private PracticeSessionStatus status;

    private Integer totalQuestionCount;

    private Integer answeredCount;

    private Integer correctCount;

    private Integer wrongCount;

    private BigDecimal scoreRate;

    private Integer durationSeconds;

    private LocalDateTime startedTime;

    private LocalDateTime submittedTime;

    private LocalDateTime completedTime;
}
