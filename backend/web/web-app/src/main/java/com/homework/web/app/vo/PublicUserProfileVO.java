package com.homework.web.app.vo;

import com.homework.model.enums.MembershipStatus;
import com.homework.model.enums.MembershipType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/** 返回 PublicUserProfile 页面上方的个人信息卡和访问者关系状态。 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicUserProfileVO {
    /** 主页用户 ID。 */
    private Long userId;

    private PublicUserInfoVO userInfo;
    private MembershipStatus membershipStatus;
    private MembershipType membershipType;

    /** 粉丝数。 */
    private long followerCount;
    /** 关注数。 */
    private long followingCount;

    /** 已作答题目数。 */
    private Long answeredQuestionCount;
    /** 有作答记录的题库数。 */
    private Long learnedBankCount;
    /** 累计学习小时数。 */
    private Long studyHours;
    /** 当前访问者是否就是主页用户。 */
    private boolean self;
    /** 当前访问者是否关注主页用户；自己的主页为空。 */
    private Boolean followedByCurrentUser;
    /** 当前访问者是否与主页用户互关。 */
    private boolean mutualFollow;
    /** 当前访问者和主页用户之间是否存在拉黑关系。 */
    private boolean blocked;
    /** 当前访问者是否可以给主页用户发私信。 */
    private boolean canSendPrivateMessage;
    /** 双方已有 Chatbox ID；不存在时为空。 */
    private Long chatboxId;


}
