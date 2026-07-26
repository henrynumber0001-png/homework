package com.homework.web.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.UserCommunityRestriction;
import com.homework.model.enums.CommunityRestrictionScope;
import com.homework.web.app.mapper.UserCommunityRestrictionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** 在 App 发帖和评论入口执行后台社区权限限制。 */
@Service
@RequiredArgsConstructor
public class CommunityAccessService {

    private final UserCommunityRestrictionMapper restrictionMapper;

    public void requirePostAllowed(Long userId) {
        Long count = restrictionMapper.selectCount(new LambdaQueryWrapper<UserCommunityRestriction>()
                .eq(UserCommunityRestriction::getUserId, userId)
                .eq(UserCommunityRestriction::getActive, true)
                .in(UserCommunityRestriction::getScope,
                        CommunityRestrictionScope.POST,
                        CommunityRestrictionScope.BOTH)
                .and(wrapper -> wrapper.isNull(UserCommunityRestriction::getEndTime)
                        .or().gt(UserCommunityRestriction::getEndTime, LocalDateTime.now())));
        if (count > 0) {
            throw new HomeworkException(ResultCodeEnum.COMMUNITY_POST_RESTRICTED);
        }
    }

    public void requireCommentAllowed(Long userId) {
        Long count = restrictionMapper.selectCount(new LambdaQueryWrapper<UserCommunityRestriction>()
                .eq(UserCommunityRestriction::getUserId, userId)
                .eq(UserCommunityRestriction::getActive, true)
                .in(UserCommunityRestriction::getScope,
                        CommunityRestrictionScope.COMMENT,
                        CommunityRestrictionScope.BOTH)
                .and(wrapper -> wrapper.isNull(UserCommunityRestriction::getEndTime)
                        .or().gt(UserCommunityRestriction::getEndTime, LocalDateTime.now())));
        if (count > 0) {
            throw new HomeworkException(ResultCodeEnum.COMMUNITY_COMMENT_RESTRICTED);
        }
    }
}
