package com.homework.web.app.vo;

import lombok.Data;

import java.util.List;

//“我的笔记”功能，也是按照 group-module-subModule-bank-noteContentList进行展示的
@Data
public class NoteVO {

    private long groupId;
    private long moduleId;
    private long subModuleId;
    private long bankId;
    //每个题库中的全部笔记列表
    private List<QuestionNoteVO> questionNoteVOList;
}
