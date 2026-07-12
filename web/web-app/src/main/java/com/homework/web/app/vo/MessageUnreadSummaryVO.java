package com.homework.web.app.vo;

import lombok.Data;

/** 头像菜单角标及“我的消息”四个模块的未读数量。 */
@Data
public class MessageUnreadSummaryVO {
    private long replies;
    private long likes;
    private long system;
    private long privateMessages;

    public long getTotal() {
        return replies + likes + system + privateMessages;
    }
}
