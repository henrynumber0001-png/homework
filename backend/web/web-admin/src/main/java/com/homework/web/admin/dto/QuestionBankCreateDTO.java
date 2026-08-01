package com.homework.web.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 单条创建题库请求。 */
@Data
public class QuestionBankCreateDTO {

    @NotNull
    private Long subModuleId;

    @NotBlank
    @Size(max = 100)
    private String bankName;

    @Size(max = 10)
    private List<@NotBlank @Size(max = 30) String> tagNames;

    @Min(10)
    @Max(9999)
    /** 变更：原 priority 改为题库人工曝光权重，数值越大越优先。 */
    private Integer sortOrder;
}
