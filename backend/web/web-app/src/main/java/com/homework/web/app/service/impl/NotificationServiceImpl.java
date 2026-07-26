package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.model.entity.UserNotification;
import com.homework.model.enums.*;
import com.homework.web.app.mapper.UserNotificationMapper;
import com.homework.web.app.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final UserNotificationMapper notificationMapper;

    @Override
    public void create(Long receiverUserId, Long actionUserId, UserNotificationType type,
                       UserNotificationSendTo sendTo, Long itemId, Long postId,
                       String title, String content) {
        if (receiverUserId == null || Objects.equals(receiverUserId, actionUserId)) {
            return;
        }
        UserNotification notification = new UserNotification();
        notification.setReceiverUserId(receiverUserId);
        notification.setSenderUserId(actionUserId);
        notification.setNotificationType(type);
        notification.setSendTo(sendTo);
        notification.setItemId(itemId);
        notification.setPostId(postId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setReadStatus(UserNotificationReadStatus.UNREAD);
        notificationMapper.insert(notification);
    }

    @Override
    public void remove(Long receiverUserId, Long actionUserId, UserNotificationType type,
                       UserNotificationSendTo sendTo, Long itemId) {
        notificationMapper.delete(new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getReceiverUserId, receiverUserId)
                .eq(UserNotification::getSenderUserId, actionUserId)
                .eq(UserNotification::getNotificationType, type)
                .eq(UserNotification::getSendTo, sendTo)
                .eq(UserNotification::getItemId, itemId));
    }
}
