package com.homework.web.app.vo;

import com.homework.model.enums.GroupType;
import com.homework.model.enums.QuestionInfoQuestionType;
import lombok.Data;

@Data
public class WrongQuestionVO {
    private Long questionId;
    private String title;
    private QuestionInfoQuestionType questionType;
    //这道错题是否已下架或删除
    private Boolean isAvailable;

}
