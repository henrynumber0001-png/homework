package com.homework.web.app.vo;

import com.homework.model.enums.QuestionInfoQuestionType;
import lombok.Data;

@Data
public class FavoriteQuestionVO {
    private Long questionId;
    private String title;
    private QuestionInfoQuestionType questionType;
    private Boolean isAvailable;
}
