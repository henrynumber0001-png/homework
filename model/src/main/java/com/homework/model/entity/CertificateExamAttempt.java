package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.ExamAttemptStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "certificate_exam_attempt", autoResultMap = true)
public class CertificateExamAttempt extends BaseEntity {
    private Long userId;
    private Long bankId;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> questionOrder; // 本次考试随机后的固定题序
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime submittedAt;
    private ExamAttemptStatus status;
    private Long correctCount;
    private BigDecimal correctRate;
}
