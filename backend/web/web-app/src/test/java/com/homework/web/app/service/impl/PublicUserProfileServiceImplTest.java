package com.homework.web.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.common.storage.UserImageUrlResolver;
import com.homework.model.entity.UserInfo;
import com.homework.model.enums.MembershipStatus;
import com.homework.model.enums.UserInfoStatus;
import com.homework.web.app.mapper.*;
import com.homework.web.app.service.MembershipAccessService;
import com.homework.web.app.service.MembershipAccessSnapshot;
import com.homework.web.app.vo.PublicUserProfileVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicUserProfileServiceImplTest {
    @Mock private UserInfoMapper userInfoMapper;
    @Mock private UserFollowMapper followMapper;
    @Mock private PrivateChatboxMapper chatboxMapper;
    @Mock private HitPostMapper postMapper;
    @Mock private HitActionMapper actionMapper;
    @Mock private UserImageUrlResolver userImageUrlResolver;
    @Mock private MembershipAccessService membershipAccessService;
    @Mock private UserQuestionAnswerMapper userQuestionAnswerMapper;
    @Mock private UserLearningStatDailyMapper userLearningStatDailyMapper;
    @Mock private UserBlockMapper userBlockMapper;

    private PublicUserProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PublicUserProfileServiceImpl(userInfoMapper, followMapper, chatboxMapper,
                postMapper, actionMapper, new ObjectMapper(), userImageUrlResolver,
                membershipAccessService, userQuestionAnswerMapper, userLearningStatDailyMapper,
                userBlockMapper);
    }

    @Test
    void ownProfileReturnsNoFollowOrPrivateMessageActions() {
        UserInfo user = new UserInfo();
        user.setId(7L);
        user.setStatus(UserInfoStatus.ACTIVE);
        user.setDisplayName("测试用户");
        user.setAccountNo("HW000007");
        when(userInfoMapper.selectById(7L)).thenReturn(user);
        when(followMapper.selectCount(any())).thenReturn(0L);
        when(userQuestionAnswerMapper.selectCount(any())).thenReturn(0L);
        when(userQuestionAnswerMapper.selectList(any())).thenReturn(java.util.List.of());
        when(userLearningStatDailyMapper.selectList(any())).thenReturn(java.util.List.of());
        when(membershipAccessService.getAccess(7L)).thenReturn(
                new MembershipAccessSnapshot(MembershipStatus.FREE, null, null, null));

        PublicUserProfileVO result = service.getProfile(7L, 7L);

        assertTrue(result.isSelf());
        assertNull(result.getFollowedByCurrentUser());
        assertFalse(result.isBlocked());
        assertFalse(result.isCanSendPrivateMessage());
        assertNull(result.getChatboxId());
        assertEquals(0, result.getFollowerCount());
        assertEquals(0, result.getFollowingCount());
        verifyNoInteractions(chatboxMapper);
        verifyNoInteractions(userBlockMapper);
    }

    @Test
    void unblockedProfileAllowsFollowAndPrivateMessageActions() {
        prepareProfileUser(8L);
        when(userBlockMapper.selectCount(any())).thenReturn(0L);

        PublicUserProfileVO result = service.getProfile(7L, 8L);

        assertFalse(result.isSelf());
        assertFalse(result.isBlocked());
        assertTrue(result.isCanSendPrivateMessage());
    }

    @Test
    void blockedProfileKeepsFollowStateButOmitsRestrictedProfileData() {
        prepareProfileUser(8L);
        when(userBlockMapper.selectCount(any())).thenReturn(1L);

        PublicUserProfileVO result = service.getProfile(7L, 8L);

        assertFalse(result.isSelf());
        assertTrue(result.isBlocked());
        assertFalse(result.isCanSendPrivateMessage());
        assertFalse(result.getFollowedByCurrentUser());
        assertEquals("测试用户", result.getUserInfo().getDisplayName());
        assertNull(result.getUserInfo().getAccountNo());
        assertNull(result.getUserInfo().getCompanyOrSchool());
        assertNull(result.getAnsweredQuestionCount());
        assertNull(result.getLearnedBankCount());
        assertNull(result.getStudyHours());
        verifyNoInteractions(chatboxMapper);
        verifyNoInteractions(userQuestionAnswerMapper);
        verifyNoInteractions(userLearningStatDailyMapper);
    }

    @Test
    void blockedProfilePostsReturnEmptyWithoutQueryingPosts() {
        UserInfo user = new UserInfo();
        user.setId(8L);
        user.setStatus(UserInfoStatus.ACTIVE);
        when(userInfoMapper.selectById(8L)).thenReturn(user);
        when(userBlockMapper.selectCount(any())).thenReturn(1L);

        assertTrue(service.listPosts(7L, 8L, 1, 20).isEmpty());
        verifyNoInteractions(postMapper);
        verifyNoInteractions(actionMapper);
    }

    private void prepareProfileUser(long profileUserId) {
        UserInfo user = new UserInfo();
        user.setId(profileUserId);
        user.setStatus(UserInfoStatus.ACTIVE);
        user.setDisplayName("测试用户");
        user.setAccountNo("HW000008");
        when(userInfoMapper.selectById(profileUserId)).thenReturn(user);
        when(followMapper.selectCount(any())).thenReturn(0L);
        lenient().when(userQuestionAnswerMapper.selectCount(any())).thenReturn(0L);
        lenient().when(userQuestionAnswerMapper.selectList(any())).thenReturn(java.util.List.of());
        lenient().when(userLearningStatDailyMapper.selectList(any())).thenReturn(java.util.List.of());
        when(membershipAccessService.getAccess(profileUserId)).thenReturn(
                new MembershipAccessSnapshot(MembershipStatus.FREE, null, null, null));
    }
}
