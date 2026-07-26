package com.homework.web.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class QuestionBankVO {
    private Long id;

    private String bankName;

    private Long subModuleId;

    @Schema(description = "完成题库的人数")
    private Integer completeCount;

    private BigDecimal avgCorrectRate;

    private List<String> tagNames;

}
