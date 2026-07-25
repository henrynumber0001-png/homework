package com.homework.web.app.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BankCorrectRateVO {

    private Long bankId;

    private BigDecimal avgCorrectRate;
}
