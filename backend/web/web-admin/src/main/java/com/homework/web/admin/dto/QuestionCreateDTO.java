package com.homework.web.admin.dto;

import com.homework.model.enums.QuestionInfoQuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;


@Data
public class QuestionCreateDTO { //这个是 每一条新创建的题目，需要输入的内容

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
    @Size(max = 6)
    private List<QuestionOptionDTO> options;

    @Size(max = 6)
    private List<@NotBlank String> correctAnswerKeys;
}
