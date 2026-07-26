package com.homework.web.app.vo;

import lombok.Data;

/** 承载 PublicUserProfile 个人信息卡所需的数据库聚合统计。 */
@Data
public class PublicUserProfileCountsVO {
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
    /** 累计学习秒数，Service 转为小时。 */
    private long studySeconds;
    /** 收到的 Post/Comment 点赞、Post 收藏和 Post 转发总数。 */
    private long receivedTotalActionCount;
}
