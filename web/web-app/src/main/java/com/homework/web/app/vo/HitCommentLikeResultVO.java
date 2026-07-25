package com.homework.web.app.vo;

import lombok.Data;

/** 返回 Comment 点赞操作完成后的最终状态与点赞数。 */
@Data
public class HitCommentLikeResultVO {
    /** 当前用户最终是否已点赞该 Comment。 */
    private boolean liked;
    /** Comment 当前点赞数。 */
    private int likeCount;
}
