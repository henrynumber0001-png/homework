package com.homework.web.app.vo;

import com.homework.model.enums.ActionStatus;
import com.homework.model.enums.HitActionType;
import lombok.Data;

/** 返回 Post 点赞、收藏或转发操作完成后的最终状态与计数。 */
@Data
public class HitActionResultVO {
    /** 本次设置的互动类型。 */
    private HitActionType actionType;
    /** 本次设置的目标状态。 */
    private ActionStatus actionStatus;
    /** Post 当前点赞数。 */
    private int likeCount;
    /** Post 当前收藏数。 */
    private int favoriteCount;
    /** Post 当前转发数。 */
    private int repostCount;
}
