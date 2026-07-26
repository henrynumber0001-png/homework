package com.homework.web.app.vo;

import lombok.Data;

/** 返回 PublicUserProfile 页面上方的个人信息卡和访问者关系状态。 */
@Data
public class PublicUserProfileVO {
    /** 主页用户 ID。 */
    private Long userId;
    /** 现有会员与基础个人信息。 */
    private MembershipInfoVO membershipInfoVO;
    /** 粉丝数。 */
    private long followerCount;
    /** 关注数。 */
    private long followingCount;
    /** 原创 Post 与有效转发数量之和。 */
    private long postCount;
    /** 已作答题目数。 */
    private long answeredQuestionCount;
    /** 有作答记录的题库数。 */
    private long learnedBankCount;
    /** 累计学习小时数。 */
    private long studyHours;
    /** 收到的 Post/Comment 点赞、Post 收藏和 Post 转发总数。 */
    private long receivedTotalActionCount;
    /** 当前访问者是否就是主页用户。 */
    private boolean self;
    /** 当前访问者是否关注主页用户；自己的主页为空。 */
    private Boolean followedByCurrentUser;
    /** 当前访问者是否与主页用户互关。 */
    private boolean mutualFollow;
    /** 当前访问者是否可以关注主页用户。 */
    private boolean canFollow;
    /** 当前访问者是否可以给主页用户发私信。 */
    private boolean canSendPrivateMessage;
    /** 双方已有 Chatbox ID；不存在时为空。 */
    private Long chatboxId;
}
