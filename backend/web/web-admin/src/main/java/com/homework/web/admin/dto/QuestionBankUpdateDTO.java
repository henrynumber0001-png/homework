package com.homework.web.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 编辑题库基础信息请求。 */
@Data
public class QuestionBankUpdateDTO {

    @NotNull
    private Long subModuleId;

    @NotBlank
    @Size(max = 100)
    private String bankName;

    @Size(max = 10)
    private List<@Size(min = 1, max = 30) String> tags;

    @Min(0)
    @Max(9999)
    private Integer priority;

    @NotBlank
    @Size(max = 500)
    private String reason;

    @NotNull
    private Integer version;
}
