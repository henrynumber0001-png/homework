package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.UserNotificationType;
import com.homework.model.enums.UserNotificationReadStatus;
import com.homework.model.enums.UserNotificationSendTo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_notification")
public class UserNotification extends BaseEntity {

    private Long receiverUserId; //接收者Id

    private Long senderUserId; //发出通知者Id

    private UserNotificationType notificationType;

    /** 通知关联的对象类型，例如动态、评论或用户。 */
    private UserNotificationSendTo sendTo;

    /** 关联对象在对应数据表中的 ID。 */
    private Long itemId;

    /** 通知关联的 Post；评论删除后仍用于进入原 Post。 */
    private Long postId;

    private String title;

    private String content;

    /** 1.unread;2.read */
    private UserNotificationReadStatus readStatus;
}
