package com.homework.web.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 确认执行题目导入的请求。 */
@Data
public class QuestionImportCommitDTO {

    @NotNull
    @Min(1)
    @Max(1000)
    private Integer confirmTotalRows;
}
