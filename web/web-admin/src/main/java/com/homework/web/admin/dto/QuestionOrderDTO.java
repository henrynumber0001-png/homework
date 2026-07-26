package com.homework.web.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 保存题库内完整题目顺序的请求。 */
@Data
public class QuestionOrderDTO {

    @NotEmpty
    private List<@NotNull Long> questionIds;

    @NotNull
    private Integer bankQuestionOrderVersion;

    @Size(max = 500)
    private String reason;
}
