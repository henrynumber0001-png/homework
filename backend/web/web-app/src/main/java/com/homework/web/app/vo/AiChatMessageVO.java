package com.homework.web.app.vo;

import com.homework.model.enums.AiChatMessageSenderType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiChatMessageVO {
    //AiChatMessageVO 是聊天窗口里的一条消息。

    /** 消息 id。 */
    private Long messageId;

    /** 消息发送方：用户或 AI。 */
    private AiChatMessageSenderType senderType;

    /** 消息正文。 */
    private String messageContent;

    /** 消息创建时间，用于前端展示或排序。 */
    private LocalDateTime createdTime;
}
