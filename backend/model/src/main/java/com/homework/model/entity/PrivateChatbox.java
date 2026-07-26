package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.PrivateChatAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 两名用户之间唯一的私信聊天盒。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("private_chatbox")
//注意！！！
//private_chatbox 表是一张“会话表”。
//它不是用来保存每一条聊天内容，而是保存：
//哪两个人正在聊天、谁发起聊天、当前能否继续发送、最后一条消息是什么。
public class PrivateChatbox extends BaseEntity {
    private Long userAId;
    private Long userBId;
    private Long initiatorUserId;
    private PrivateChatAccess chatAccess;
    private Long lastMessageId;
    private java.time.LocalDateTime lastMessageTime;
}
