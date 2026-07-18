package com.homework.web.app.vo;

import lombok.Data;

import java.util.List;

@Data
public class WrongQuestionBankVO {

    private Long bankId;
    private String bankName;
    //来自QuestionBank
    private List<String> tagNames;

    private Long wrongQuestionCount;
}
