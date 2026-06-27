package com.homework.web.app.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CategoryDetailSaveDTO {
    private Long id;
    private Long subModuleId;
    private String detailName;
    private Integer participantCount;
    private BigDecimal avgCorrectRate;
    private String tagsJson;
    private Integer sortOrder;
}
