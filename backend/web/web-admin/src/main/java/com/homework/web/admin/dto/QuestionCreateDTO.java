package com.homework.web.admin.dto;

import com.homework.model.enums.QuestionInfoQuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 后台单条创建题目的请求。 */
@Data
public class QuestionCreateDTO {

    @NotNull
    private QuestionInfoQuestionType questionType;

    @NotBlank
    @Size(max = 5000)
    private String title;

    @Size(max = 20000)
    private String analysis;

    @Size(max = 512)
    private String imageObjectKey;

    @Valid
    @Size(max = 26)
    private List<QuestionOptionDTO> options;

    @Size(max = 26)
    private List<String> correctAnswers;
}
