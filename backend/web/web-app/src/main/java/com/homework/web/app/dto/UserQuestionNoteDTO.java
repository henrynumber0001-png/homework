package com.homework.web.app.dto;

import lombok.Data;

@Data
public class UserQuestionNoteDTO {

    private Long bankId;

    private Long questionId;

    private String noteContent;
}
