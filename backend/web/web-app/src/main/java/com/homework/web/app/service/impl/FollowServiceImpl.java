package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.model.entity.UserFollow;
import com.homework.model.entity.UserInfo;
import com.homework.model.enums.UserInfoStatus;
import com.homework.model.enums.UserNotificationSendTo;
import com.homework.model.enums.UserNotificationType;
import com.homework.web.app.mapper.UserFollowMapper;
import com.homework.web.app.mapper.UserInfoMapper;
import com.homework.web.app.service.FollowService;
import com.homework.web.app.service.NotificationService;
import com.homework.web.app.vo.FollowStateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {
    private final UserFollowMapper followMapper;
    private final UserInfoMapper userInfoMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public FollowStateVO follow(Long currentUserId, Long targetUserId, Boolean active) {
        if (active == null) throw new IllegalArgumentException("active 不能为空");
        if (Objects.equals(currentUserId, targetUserId)) throw new IllegalArgumentException("不能关注自己");
        UserInfo target = userInfoMapper.selectById(targetUserId);
        if (target == null || target.getStatus() != UserInfoStatus.ACTIVE) {
            throw new IllegalArgumentException("用户不存在");
        }
        UserFollow row = followMapper.selectIncludingDeletedForUpdate(currentUserId, targetUserId);
        boolean current = row != null && !Boolean.TRUE.equals(row.getDeleted());
        boolean requested = active;
        if (requested != current) {
            if (requested && row == null) {
                row = new UserFollow();
                row.setFollowerUserId(currentUserId);
                row.setFolloweeUserId(targetUserId);
                followMapper.insert(row);
            } else if (requested) {
                followMapper.restoreById(row.getId());
            } else {
                followMapper.deactivateById(row.getId());
            }
            if (requested) {
                notificationService.create(targetUserId, currentUserId, UserNotificationType.FOLLOW,
                        UserNotificationSendTo.USER, currentUserId, null, "新增关注", "有新用户关注了你");
            } else {
                notificationService.remove(targetUserId, currentUserId, UserNotificationType.FOLLOW,
                        UserNotificationSendTo.USER, currentUserId);
            }
        }
        FollowStateVO vo = new FollowStateVO();
        vo.setActive(requested);
        vo.setFollowerCount(followMapper.countFollowers(targetUserId));
        vo.setMutualFollow(requested && followMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerUserId, targetUserId)
                .eq(UserFollow::getFolloweeUserId, currentUserId)) > 0);
        return vo;
    }
}
