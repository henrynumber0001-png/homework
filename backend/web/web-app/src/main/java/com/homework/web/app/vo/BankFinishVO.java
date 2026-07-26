package com.homework.web.app.vo;

import com.homework.web.app.dto.AiEvaluationResult;
import lombok.Data;

import java.util.List;

@Data
public class BankFinishVO {

    private List<InterviewQuestionReviewVO> interviewQuestionReviewVos;

    private List<CertificateQuestionReviewVO> certificateQuestionReviewVos;

    private QuestionCountVO questionCount;




}
