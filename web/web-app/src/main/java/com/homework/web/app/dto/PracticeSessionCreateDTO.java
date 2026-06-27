package com.homework.web.app.dto;

import lombok.Data;

@Data
public class PracticeSessionCreateDTO {
    private Long userId;
    private Long bankId;
    /** 1.interview_practice;2.cert_practice;3.cert_exam */
    private Integer sessionMode;
}
