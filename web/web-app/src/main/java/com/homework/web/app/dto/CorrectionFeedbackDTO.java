package com.homework.web.app.dto;

import lombok.Data;

@Data
public class CorrectionFeedbackDTO {
    private Long userId;
    private Long questionId;
    private Long answerId;
    private String feedbackContent;
}
