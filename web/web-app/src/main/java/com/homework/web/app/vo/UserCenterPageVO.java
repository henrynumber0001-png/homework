package com.homework.web.app.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserCenterPageVO {

     private GraphInfoVO graphInfoVO;

     private UserInfoVO userInfoVO;

     private PremiumUserInfoVO premiumUserInfoVO;

     private long followerCount;
     private long followingCount;
     private long postCount;

     private long questionCount;
     private long bankCount;

     private long learningHours;

     private List<WrongQuestionVO> wrongQuestionVOList;

     private List<FavoriteQuestionVO> favoriteQuestionVOList;

     private List<NoteVO> noteVOList;




}
