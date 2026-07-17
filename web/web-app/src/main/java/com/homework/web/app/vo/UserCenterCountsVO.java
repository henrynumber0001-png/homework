package com.homework.web.app.vo;

import lombok.Data;

@Data
public class UserCenterCountsVO {

    private long followerCount;
    private long followingCount;
    private long postCount;


    private long answeredQuestionCount;
    private long learnedBankCount; //有作答记录的题库
    private long studyHours;
    private long wrongQuestionCount;
    private long favoriteQuestionCount;
    private long noteCount;
}
