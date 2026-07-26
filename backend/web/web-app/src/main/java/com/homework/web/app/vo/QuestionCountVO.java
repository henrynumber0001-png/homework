package com.homework.web.app.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuestionCountVO {

    /**
     * 对于GroupType = INTERVIEW的题库，不返回 correctCount
     */

    private Long totalCount;
    private Long answeredCount;
    //只针对 GroupType = CERTIFICATION
    private Long correctCount;

    private BigDecimal correctRate;
}
