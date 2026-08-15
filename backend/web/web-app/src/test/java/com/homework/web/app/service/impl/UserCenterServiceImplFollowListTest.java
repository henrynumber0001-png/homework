package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.homework.common.storage.UserImageUrlResolver;
import com.homework.model.entity.UserFollow;
import com.homework.model.entity.UserInfo;
import com.homework.model.enums.UserInfoStatus;
import com.homework.web.app.mapper.UserBlockMapper;
import com.homework.web.app.mapper.UserFollowMapper;
import com.homework.web.app.mapper.UserInfoMapper;
import com.homework.web.app.vo.FolloweeVO;
import com.homework.web.app.vo.FollowerVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserCenterServiceImplFollowListTest {
    private UserInfoMapper userInfoMapper;
    private UserFollowMapper followMapper;
    private UserBlockMapper blockMapper;
    private UserImageUrlResolver imageUrlResolver;
    private UserCenterServiceImpl service;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "test");
        TableInfoHelper.initTableInfo(assistant, UserFollow.class);
        TableInfoHelper.initTableInfo(assistant, UserInfo.class);
        TableInfoHelper.initTableInfo(assistant, com.homework.model.entity.UserBlock.class);
        userInfoMapper = mock(UserInfoMapper.class);
        followMapper = mock(UserFollowMapper.class);
        blockMapper = mock(UserBlockMapper.class);
        imageUrlResolver = mock(UserImageUrlResolver.class);
        service = new UserCenterServiceImpl(
                userInfoMapper, null, followMapper, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                imageUrlResolver, null, null, null, blockMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void followingListUsesFolloweeIdAndChecksTheReverseRelationship() {
        UserFollow currentFollowsTarget = follow(7L, 8L);
        when(followMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<UserFollow> page = invocation.getArgument(0);
            page.setRecords(List.of(currentFollowsTarget));
            return page;
        });
        when(userInfoMapper.selectByIds(any())).thenReturn(List.of(activeUser(8L, "目标用户")));
        when(followMapper.selectCount(any())).thenReturn(1L);
        when(blockMapper.selectCount(any())).thenReturn(0L);

        List<FolloweeVO> result = service.getFollowing(7L, 1, 20);

        assertEquals(1, result.size());
        assertEquals(8L, result.get(0).getFolloweeUserId());
        assertEquals("目标用户", result.get(0).getFolloweeDisplayName());
        assertTrue(result.get(0).isMutualFollow());
    }

    @Test
    @SuppressWarnings("unchecked")
    void followerListShowsMutualWhenCurrentUserFollowsTheFollowerBack() {
        UserFollow targetFollowsCurrent = follow(8L, 7L);
        when(followMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<UserFollow> page = invocation.getArgument(0);
            page.setRecords(List.of(targetFollowsCurrent));
            return page;
        });
        when(userInfoMapper.selectByIds(any())).thenReturn(List.of(activeUser(8L, "粉丝用户")));
        when(followMapper.selectCount(any())).thenReturn(1L);
        when(blockMapper.selectCount(any())).thenReturn(0L);

        List<FollowerVO> result = service.getFollowers(7L, 1, 20);

        assertEquals(1, result.size());
        assertEquals(8L, result.get(0).getFollowerUserId());
        assertEquals("粉丝用户", result.get(0).getFollowerDisplayName());
        assertTrue(result.get(0).isMutualFollow());
    }

    private static UserFollow follow(long followerId, long followeeId) {
        UserFollow follow = new UserFollow();
        follow.setFollowerUserId(followerId);
        follow.setFolloweeUserId(followeeId);
        return follow;
    }

    private static UserInfo activeUser(long id, String displayName) {
        UserInfo user = new UserInfo();
        user.setId(id);
        user.setDisplayName(displayName);
        user.setStatus(UserInfoStatus.ACTIVE);
        return user;
    }
}
