package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.common.enums.AiChatMessageSenderType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_chat_message")
public class AiChatMessage extends BaseEntity {

    private Long sessionId;

    /** 1.user;2.ai */
    private AiChatMessageSenderType senderType;

    private String messageContent;

    private String modelName;
}
