package com.homework.web.app.vo;

import lombok.Data;

import java.util.List;

@Data
public class WrongQuestionVO {

    private long groupId;
    private long moduleId;
    private long subModuleId;
    private long bankId;
    private List<Long> wrongQuestionIds;
}
