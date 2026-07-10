package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.ExamSessionStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

//考试场次
@Data
@TableName(value = "certificate_exam_session", autoResultMap = true)
public class CertificateExamSession extends BaseEntity {
    private Long userId;
    private Long bankId;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> questionOrder; // 本次考试随机后的固定题序，其实就是 questionIds
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime submittedAt;
    private ExamSessionStatus status;
    private Long correctCount;
    private BigDecimal correctRate;
}
