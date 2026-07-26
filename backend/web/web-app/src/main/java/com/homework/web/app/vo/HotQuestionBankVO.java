package com.homework.web.app.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class HotQuestionBankVO {
    private long bankId;

    private String bankName;

    //题库下方的标签显示
    private String moduleName;

    //完成人数
    private Integer completeCount;

    //平均正确率
    private BigDecimal avgCorrectRate;
}
