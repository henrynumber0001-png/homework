package com.homework.web.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.model.entity.UserInfo;
import com.homework.model.enums.UserInfoStatus;
import com.homework.model.enums.UserInfoUserRole;
import com.homework.web.app.mapper.*;
import com.homework.web.app.service.UserCenterService;
import com.homework.web.app.vo.MembershipInfoVO;
import com.homework.web.app.vo.PublicUserProfileCountsVO;
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
    @Mock private PublicUserProfileMapper profileMapper;
    @Mock private HitPostMapper postMapper;
    @Mock private HitCommentMapper commentMapper;
    @Mock private HitActionMapper actionMapper;
    @Mock private HitCommentLikeMapper commentLikeMapper;
    @Mock private UserCenterService userCenterService;

    private PublicUserProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PublicUserProfileServiceImpl(userInfoMapper, followMapper, chatboxMapper,
                profileMapper, postMapper, commentMapper, actionMapper, commentLikeMapper,
                userCenterService, new ObjectMapper());
    }

    @Test
    void ownProfileReturnsNoFollowOrPrivateMessageActions() {
        UserInfo user = new UserInfo();
        user.setId(7L);
        user.setStatus(UserInfoStatus.ACTIVE);
        user.setUserRole(UserInfoUserRole.USER);
        PublicUserProfileCountsVO counts = new PublicUserProfileCountsVO();
        counts.setPostCount(6);
        MembershipInfoVO membership = new MembershipInfoVO();
        when(userInfoMapper.selectById(7L)).thenReturn(user);
        when(profileMapper.selectCounts(7L)).thenReturn(counts);
        when(userCenterService.getMembershipInfo(7L)).thenReturn(membership);

        PublicUserProfileVO result = service.getProfile(7L, 7L);

        assertTrue(result.isSelf());
        assertNull(result.getFollowedByCurrentUser());
        assertFalse(result.isCanFollow());
        assertFalse(result.isCanSendPrivateMessage());
        assertNull(result.getChatboxId());
        assertEquals(6, result.getPostCount());
        verifyNoInteractions(followMapper, chatboxMapper);
    }
}
