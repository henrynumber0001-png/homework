package com.homework.web.admin.vo;

import lombok.Data;

import java.util.List;

/** 后台用户详情。 */
@Data
public class UserDetailVO extends UserRowVO {

    /** 脱敏登录身份列表。 */
    private List<UserIdentityVO> identities;

    /** 当前有效社区限制。 */
    private UserCommunityRestrictionVO communityRestriction;

    /** 用户发布的未逻辑删除动态数量。 */
    private Long postCount;

    /** 用户发布的未逻辑删除评论数量。 */
    private Long commentCount;
}
