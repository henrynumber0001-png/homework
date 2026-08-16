package com.homework.web.app.vo;

import com.homework.model.enums.MembershipStatus;
import com.homework.model.enums.MembershipType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/** 返回 PublicUserProfile 页面上方的个人信息卡和访问者关系状态。 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicUserProfileVO {

    private Long userId;

    private PublicUserInfoVO userInfo;
    private MembershipStatus membershipStatus;
    private MembershipType membershipType;

    private long followerCount;

    private long followingCount;

    private Long answeredQuestionCount;

    private Long learnedBankCount;

    private Long studyHours;

    private boolean self;

    private Boolean followedByCurrentUser;

    private boolean mutualFollow;
    //当前访问者和主页用户之间是否存在拉黑关系
    //拉黑一定是双向的
    private boolean blocked;

    private boolean blockedByCurrentUser;

    private boolean canSendPrivateMessage;

    //双方已有 Chatbox ID；不存在时为空
    private Long chatboxId;


}
