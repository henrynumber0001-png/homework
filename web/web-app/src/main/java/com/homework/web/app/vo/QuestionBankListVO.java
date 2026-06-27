package com.homework.web.app.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class QuestionBankListVO {
    private Long id;
    private String name;
    private Integer bankType;
    private Integer questionCount;
    private Integer practiceUserCount;
    private BigDecimal avgCorrectRate;
    private Integer favoriteCount;
    private Integer viewCount;
    private Boolean premium;
    private List<String> tags;
}
