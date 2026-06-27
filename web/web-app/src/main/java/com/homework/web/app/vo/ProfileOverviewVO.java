package com.homework.web.app.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ProfileOverviewVO {
    private Long userId;
    private String displayName;
    private String avatar;
    private Long followingCount;
    private Long followerCount;
    private Long postCount;
    private Long answeredCount;
    private Long learnedBankCount;
    private Long studySeconds;
    private Long medalCount;
    private List<Map<String, Object>> weeklyStudyStats;
}
