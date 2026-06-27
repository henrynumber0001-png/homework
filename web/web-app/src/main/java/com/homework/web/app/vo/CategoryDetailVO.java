package com.homework.web.app.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CategoryDetailVO {
    private Long id;
    private Long subModuleId;
    private String detailName;
    private Integer participantCount;
    private BigDecimal avgCorrectRate;
    private String tagsJson;
    private Integer sortOrder;
}
