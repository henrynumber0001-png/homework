package com.homework.web.app.vo;

import com.homework.model.enums.UserNotificationNotificationType;
import com.homework.model.enums.UserNotificationReadStatus;
import com.homework.model.enums.UserNotificationTargetType;
import lombok.Data;

import java.time.LocalDateTime;

/** “我的消息”中回复、互动和系统消息的统一展示模型。 */
@Data
public class NotificationVO {
    private Long id;
    private Long senderUserId;
    private String senderDisplayName;
    private String senderAvatar;
    private UserNotificationNotificationType notificationType;
    private UserNotificationTargetType targetType;
    private Long targetId;
    private String title;
    private String content;
    private UserNotificationReadStatus readStatus;
    private LocalDateTime createdTime;
}
