package com.homework.web.app.vo;

import com.homework.model.enums.ActionStatus;
import lombok.Data;

/** 返回当前用户与公开主页用户之间的关注状态。 */
@Data
public class FollowStateVO {
    /** 当前用户是否正在关注目标用户。 */
    private ActionStatus status;
    /** 目标用户当前粉丝数。 */
    private long followerCount;
    /** 双方是否互相关注。 */
    private boolean mutualFollow;
}
