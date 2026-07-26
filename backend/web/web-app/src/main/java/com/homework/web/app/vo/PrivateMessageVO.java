package com.homework.web.app.vo;

import com.homework.model.enums.PrivateMessageStatus;
import lombok.Data;

import java.time.LocalDateTime;

/** 展示私信 Chatbox 中的一条纯文本消息。 */
@Data
public class PrivateMessageVO {
    /** 消息 ID。 */
    private Long id;
    /** 所属 Chatbox ID。 */
    private Long chatboxId;
    /** 发送者 ID。 */
    private Long senderUserId;
    /** 发送者展示名。 */
    private String senderDisplayName;
    /** 发送者头像。 */
    private String senderAvatar;
    /** 接收者 ID。 */
    private Long receiverUserId;
    /** 纯文本消息内容。 */
    private String content;
    /** 消息发送或已读状态。 */
    private PrivateMessageStatus messageStatus;
    /** 消息发送时间。 */
    private LocalDateTime createdTime;
}
