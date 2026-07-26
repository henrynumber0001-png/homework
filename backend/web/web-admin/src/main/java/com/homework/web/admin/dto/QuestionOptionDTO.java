package com.homework.web.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 认证题选项输入。 */
@Data
public class QuestionOptionDTO {

    @NotBlank
    @Size(max = 5000)
    private String key;

    @NotBlank
    @Size(max = 5000)
    private String content;
}
