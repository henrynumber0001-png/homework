package com.homework.web.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 管理员把一道题移动到题库内指定序号的请求。 */
@Data
public class QuestionNoUpdateDTO {

    @NotNull
    @Min(1)
    private Integer questionNo;

    @NotNull
    private Integer bankQuestionOrderVersion;

    @NotBlank
    @Size(max = 500)
    private String reason;
}
