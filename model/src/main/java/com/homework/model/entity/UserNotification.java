package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.common.enums.UserNotificationNotificationType;
import com.homework.common.enums.UserNotificationReadStatus;
import com.homework.common.enums.UserNotificationTargetType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_notification")
public class UserNotification extends BaseEntity {

    private Long receiverUserId;

    private Long senderUserId;

    /** 1.reply;2.like;3.system;4.private_message;5.favorite;6.repost;7.follow */
    private UserNotificationNotificationType notificationType;

    /** 1.hit_post;2.hit_comment;3.question;4.bank;5.user */
    private UserNotificationTargetType targetType;

    private Long targetId;

    private String title;

    private String content;

    /** 1.unread;2.read */
    private UserNotificationReadStatus readStatus;
}
