package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.UserFollow;
import com.homework.model.entity.UserInfo;
import com.homework.model.enums.ActionStatus;
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
    public FollowStateVO follow(Long currentUserId, Long targetUserId, ActionStatus status) {

        if (status == null || Objects.equals(currentUserId, targetUserId)) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        UserInfo target = userInfoMapper.selectById(targetUserId);
        if (target == null || target.getStatus() != UserInfoStatus.ACTIVE) {
            throw new HomeworkException(ResultCodeEnum.APP_ACCOUNT_STATUS_ERROR);
        }

        //这一条是在查询 currentUser 是否正在关注/曾经关注 targetUser
        //follower_user_id 和 followee_user_id 做了唯一索引，因此 follow 和 unfollow 是逻辑删除和恢复关系
        UserFollow follow = followMapper.selectIncludingDeletedForUpdate(currentUserId, targetUserId);


        boolean change = false;
        if (follow != null && Boolean.TRUE.equals(follow.getDeleted())) {  //曾经关注，现在取关了
            if (status == ActionStatus.ACTIVATE) {
                int result = followMapper.restoreById(follow.getId());
                if (result != 1) {
                    throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
                }
                change = true;
            }

        } else if (follow != null && Boolean.FALSE.equals(follow.getDeleted())) { //如果现在正在关注
            if (status == ActionStatus.DEACTIVATE) {
                int result = followMapper.deactivateById(follow.getId());
                if (result != 1) {
                    throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
                }
                change = true;
            }
        } else {//说明 currentUser 从未关注过 targetUser，那么进一步查看指令是 关注还是取关
            //如果是取关，不做任何回应
            //如果是关注，创建一条新的 userFollow
            if (status == ActionStatus.ACTIVATE) {
                UserFollow userFollow = new UserFollow();
                userFollow.setFollowerUserId(currentUserId);
                userFollow.setFolloweeUserId(targetUserId);
                int result = followMapper.insert(userFollow);
                if (result != 1) {
                    throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
                }
                change = true;
            }

        }


        // 关注关系发生真实变化后，同步维护对方收到的 FOLLOW 类型通知。
        if (change && status == ActionStatus.ACTIVATE) {
            // 当前用户关注目标用户：接收者是 targetUserId，发送者是 currentUserId。
            notificationService.create(targetUserId, currentUserId, UserNotificationType.FOLLOW,
                    UserNotificationSendTo.USER, currentUserId, null, "新增关注", "有新用户关注了你");
        }
        if (change && status == ActionStatus.DEACTIVATE) {
            // 当前用户取消关注目标用户：删除此前由这次关注关系产生的通知。
            notificationService.remove(targetUserId, currentUserId, UserNotificationType.FOLLOW,
                    UserNotificationSendTo.USER, currentUserId);
        }


        FollowStateVO vo = new FollowStateVO();
        vo.setStatus(status);
        vo.setFollowerCount(followMapper.countFollowers(targetUserId));

        //为什么要有这个字段？因为前端要据此判断，当你点击了 follow 按钮之后，应该显示 following 还是 mutual
        LambdaQueryWrapper<UserFollow> query = new LambdaQueryWrapper<>();
        query.eq(UserFollow::getFollowerUserId, currentUserId)
                .eq(UserFollow::getFolloweeUserId, targetUserId);

        LambdaQueryWrapper<UserFollow> reverseQuery = new LambdaQueryWrapper<>();
        reverseQuery.eq(UserFollow::getFollowerUserId, targetUserId)
                .eq(UserFollow::getFolloweeUserId, currentUserId);

        //这里不能用 and(eq eq and(eq eq))，因为 followerId 不可能同时是 currentUserId 和 targetUserId
        //因此要拆分成 2个 query
        boolean mutual = followMapper.selectCount(query) > 0 && followMapper.selectCount(reverseQuery) > 0;

        vo.setMutualFollow(mutual);
        return vo;
    }
}
