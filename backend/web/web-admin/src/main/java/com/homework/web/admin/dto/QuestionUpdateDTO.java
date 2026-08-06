package com.homework.web.admin.dto;

import com.homework.model.enums.QuestionInfoQuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 后台编辑题目主体的请求。 */
@Data
public class QuestionUpdateDTO {

    @NotNull
    private QuestionInfoQuestionType questionType;

    @NotBlank
    @Size(max = 5000)
    private String title;

    @Size(max = 20000)
    private String analysis;

    @Size(max = 512)
    private String imageObjectKey;

    private Boolean removeImage;

    @Valid
    @Size(max = 6)
    private List<QuestionOptionDTO> options;

    @Size(max = 6)
    private List<@NotBlank String> correctAnswerKeys;

    @Size(max = 500)
    private String reason;

    @NotNull
    private Integer version;
}
