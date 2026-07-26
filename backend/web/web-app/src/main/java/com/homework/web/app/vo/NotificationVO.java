package com.homework.web.app.vo;

import com.homework.model.enums.UserNotificationType;
import com.homework.model.enums.UserNotificationReadStatus;
import lombok.Data;

import java.time.LocalDateTime;

/** 展示“评论和@、赞和转发、系统消息”列表中的一条通知。 */
@Data
public class NotificationVO {
    /** UserNotification ID。 */
    private Long id;
    /** 发出评论、互动或关注动作的用户 ID；纯系统消息为空。 */
    private Long actionUserId;
    /** 动作发出者的展示名。 */
    private String actionDisplayName;
    /** 动作发出者的头像。 */
    private String actionAvatar;
    /** 通知类型。 */
    private UserNotificationType notificationType;
    /** 通知关联的 Post ID。 */
    private Long postId;
    /** 通知关联的 Comment ID；Post 或系统通知为空。 */
    private Long commentId;
    /** Comment 或 parentComment 是否已删除。 */
    private boolean commentDeleted;
    /** 关联 Post 当前是否可以访问。 */
    private boolean postAvailable;
    /** 通知标题。 */
    private String title;
    /** 通知摘要；评论删除时为“原评论已删除”。 */
    private String content;
    /** 当前用户是否已读。 */
    private UserNotificationReadStatus readStatus;
    /** 通知产生时间。 */
    private LocalDateTime createdTime;
}
