package com.homework.web.app.vo;

import lombok.Data;

/** 返回头像菜单及“我的消息”四个模块的未读数量。 */
@Data
public class MessageUnreadSummaryVO {
    /** 未读评论和 @ 数。 */
    private long commentsAndMentions;
    /** 未读点赞、收藏和转发数。 */
    private long interactions;
    /** 未读系统消息和新增关注数。 */
    private long system;
    /** 未读私信消息条数。 */
    private long privateMessages;

    /** 返回四类未读数之和。 */
    public long getTotal() {
        return commentsAndMentions + interactions + system + privateMessages;
    }
}
