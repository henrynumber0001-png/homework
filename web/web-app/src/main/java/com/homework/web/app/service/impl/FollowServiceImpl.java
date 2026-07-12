package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.homework.model.entity.UserFollow;
import com.homework.model.entity.UserNotification;
import com.homework.model.enums.UserNotificationNotificationType;
import com.homework.model.enums.UserNotificationReadStatus;
import com.homework.model.enums.UserNotificationTargetType;
import com.homework.web.app.mapper.UserFollowMapper;
import com.homework.web.app.mapper.UserInfoMapper;
import com.homework.web.app.mapper.UserNotificationMapper;
import com.homework.web.app.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final UserFollowMapper userFollowMapper;
    private final UserInfoMapper userInfoMapper;
    private final UserNotificationMapper userNotificationMapper;

    /**
     * 关注状态真正从“未关注”变为“已关注”时创建系统通知；取消关注会撤销
     * 尚存在的对应通知，重复请求不会生成重复关系或重复通知。
     */
    @Override
    @Transactional
    public boolean follow(Long currentUserId, Long targetUserId, Boolean active) {
        if (currentUserId == null || targetUserId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        if (Objects.equals(currentUserId, targetUserId)) {
            throw new IllegalArgumentException("不能关注自己");
        }
        if (userInfoMapper.selectById(targetUserId) == null) {
            throw new IllegalArgumentException("被关注用户不存在");
        }

        UserFollow existing = userFollowMapper.selectIncludingDeletedForUpdate(currentUserId, targetUserId);
        boolean currentlyActive = existing != null && !Boolean.TRUE.equals(existing.getDeleted());
        boolean nextActive = active == null ? !currentlyActive : active;
        boolean changed = false;
        if (nextActive && !currentlyActive) {
            if (existing == null) {
                UserFollow follow = new UserFollow();
                follow.setFollowerUserId(currentUserId);
                follow.setFollowingUserId(targetUserId);
                userFollowMapper.insert(follow);
                changed = true;
            } else {
                changed = userFollowMapper.restoreById(existing.getId()) == 1;
            }
        } else if (!nextActive && currentlyActive) {
            changed = userFollowMapper.deactivateById(existing.getId()) == 1;
        }

        if (changed && nextActive) {
            UserNotification notification = new UserNotification();
            notification.setReceiverUserId(targetUserId);
            notification.setSenderUserId(currentUserId);
            notification.setNotificationType(UserNotificationNotificationType.FOLLOW);
            notification.setTargetType(UserNotificationTargetType.USER);
            notification.setTargetId(currentUserId);
            notification.setTitle("新增关注");
            notification.setContent("有新用户关注了你");
            notification.setReadStatus(UserNotificationReadStatus.UNREAD);
            userNotificationMapper.insert(notification);
        } else if (changed) {
            userNotificationMapper.delete(new QueryWrapper<UserNotification>()
                    .eq("receiver_user_id", targetUserId)
                    .eq("sender_user_id", currentUserId)
                    .eq("notification_type", UserNotificationNotificationType.FOLLOW.getValue())
                    .eq("target_type", UserNotificationTargetType.USER.getValue())
                    .eq("target_id", currentUserId));
        }
        return nextActive;
    }
}
