package com.homework.web.app.vo;

import com.homework.model.enums.PrivateChatAccess;
import lombok.Data;
import java.time.LocalDateTime;

/** 展示私信模块中的一个 Chatbox 摘要。 */
@Data
public class PrivateChatboxVO {
    /** Chatbox ID。 */
    private Long chatboxId;
    /** 对方用户 ID。 */
    private Long otherUserId;
    /** 对方展示名。 */
    private String otherDisplayName;
    /** 对方头像。 */
    private String otherAvatar;
    /** 当前 Chatbox 的发送权限。 */
    private PrivateChatAccess chatAccess;
    /** 当前用户是否可以发送下一条消息。 */
    private boolean canCurrentUserSend;
    /** 最后一条消息摘要。 */
    private String lastMessage;
    /** 最后一条消息时间。 */
    private LocalDateTime lastMessageTime;
    /** 当前用户在该 Chatbox 的未读消息数。 */
    private long unreadCount;
}
