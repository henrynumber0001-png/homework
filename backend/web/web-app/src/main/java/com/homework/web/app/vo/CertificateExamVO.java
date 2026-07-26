package com.homework.web.app.vo;

import com.homework.model.enums.ExamSessionStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CertificateExamVO {
    private Long sessionId; //考试场次Id
    private Long bankId;
    private LocalDateTime expiresAt; // 前端根据服务器截止时间显示倒计时
    private ExamSessionStatus status;
    private List<CertificateExamQuestionVO> questions;
}
