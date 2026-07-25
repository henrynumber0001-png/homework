package com.homework.web.app.vo;

import lombok.Data;

import java.util.List;

@Data
public class HomePageVO {
    private List<HotQuestionBankVO> interviewQuestionBankVOList;

    private List<HotQuestionBankVO> certificateQuestionBankVOList;

    private List<HitPostVO> hotPostList;

}
