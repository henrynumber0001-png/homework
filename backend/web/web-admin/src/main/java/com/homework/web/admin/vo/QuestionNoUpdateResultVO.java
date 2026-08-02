package com.homework.web.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 单题序号调整结果。 */
@Data
public class QuestionNoUpdateResultVO {

    private Long bankId;

    private Long questionId;

    private Integer previousQuestionNo;

    private Integer questionNo;

    private Integer bankQuestionOrderVersion;

    private LocalDateTime updatedTime;
}
