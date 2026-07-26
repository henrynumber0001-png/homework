package com.homework.web.app.service;

import com.homework.model.enums.UserNotificationSendTo;
import com.homework.model.enums.UserNotificationType;

/** 供 Hit、关注等业务写入或撤销用户通知。 */
public interface NotificationService {
    void create(Long receiverUserId, Long actionUserId, UserNotificationType type,
                UserNotificationSendTo sendTo, Long itemId, Long postId,
                String title, String content);

    void remove(Long receiverUserId, Long actionUserId, UserNotificationType type,
                UserNotificationSendTo sendTo, Long itemId);
}
