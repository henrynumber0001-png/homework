package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import com.homework.web.app.service.NotificationService;
import com.homework.web.app.vo.FollowStateVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowServiceImplTest {

    private static final long CURRENT_USER_ID = 7L;
    private static final long TARGET_USER_ID = 8L;
    private static final long FOLLOW_ID = 21L;

    @Mock
    private UserFollowMapper followMapper;
    @Mock
    private UserInfoMapper userInfoMapper;
    @Mock
    private NotificationService notificationService;

    private FollowServiceImpl service;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "follow-service-test");
        TableInfoHelper.initTableInfo(assistant, UserFollow.class);
        service = new FollowServiceImpl(followMapper, userInfoMapper, notificationService);
    }

    @Test
    void activateNeverFollowedUserCreatesRelationAndNotification() {
        prepareActiveTarget();
        when(followMapper.selectIncludingDeletedForUpdate(CURRENT_USER_ID, TARGET_USER_ID))
                .thenReturn(null);
        when(followMapper.insert(any(UserFollow.class))).thenReturn(1);
        when(followMapper.countFollowers(TARGET_USER_ID)).thenReturn(11L);
        when(followMapper.selectCount(any())).thenReturn(1L, 0L);

        FollowStateVO result = service.follow(
                CURRENT_USER_ID, TARGET_USER_ID, ActionStatus.ACTIVATE);

        ArgumentCaptor<UserFollow> relationCaptor = ArgumentCaptor.forClass(UserFollow.class);
        verify(followMapper).insert(relationCaptor.capture());
        assertEquals(CURRENT_USER_ID, relationCaptor.getValue().getFollowerUserId());
        assertEquals(TARGET_USER_ID, relationCaptor.getValue().getFolloweeUserId());
        verifyFollowNotificationCreated();
        verify(followMapper, never()).restoreById(any());
        verify(followMapper, never()).deactivateById(any());
        assertEquals(ActionStatus.ACTIVATE, result.getStatus());
        assertEquals(11L, result.getFollowerCount());
        assertFalse(result.isMutualFollow());
    }

    @Test
    void deactivateNeverFollowedUserIsIdempotentAndHasNoSideEffects() {
        prepareActiveTarget();
        when(followMapper.selectIncludingDeletedForUpdate(CURRENT_USER_ID, TARGET_USER_ID))
                .thenReturn(null);
        when(followMapper.countFollowers(TARGET_USER_ID)).thenReturn(10L);

        FollowStateVO result = service.follow(
                CURRENT_USER_ID, TARGET_USER_ID, ActionStatus.DEACTIVATE);

        verify(followMapper, never()).insert(any(UserFollow.class));
        verify(followMapper, never()).restoreById(any());
        verify(followMapper, never()).deactivateById(any());
        verifyNoInteractions(notificationService);
        assertEquals(ActionStatus.DEACTIVATE, result.getStatus());
        assertEquals(10L, result.getFollowerCount());
        assertFalse(result.isMutualFollow());
    }

    @Test
    void activateDeletedRelationRestoresItAndCreatesNotification() {
        prepareActiveTarget();
        UserFollow deletedRelation = relation(true);
        when(followMapper.selectIncludingDeletedForUpdate(CURRENT_USER_ID, TARGET_USER_ID))
                .thenReturn(deletedRelation);
        when(followMapper.restoreById(FOLLOW_ID)).thenReturn(1);
        when(followMapper.countFollowers(TARGET_USER_ID)).thenReturn(11L);
        when(followMapper.selectCount(any())).thenReturn(1L, 0L);

        FollowStateVO result = service.follow(
                CURRENT_USER_ID, TARGET_USER_ID, ActionStatus.ACTIVATE);

        verify(followMapper).restoreById(FOLLOW_ID);
        verify(followMapper, never()).insert(any(UserFollow.class));
        verify(followMapper, never()).deactivateById(any());
        verifyFollowNotificationCreated();
        assertEquals(ActionStatus.ACTIVATE, result.getStatus());
        assertFalse(result.isMutualFollow());
    }

    @Test
    void deactivateDeletedRelationIsIdempotentAndHasNoSideEffects() {
        prepareActiveTarget();
        when(followMapper.selectIncludingDeletedForUpdate(CURRENT_USER_ID, TARGET_USER_ID))
                .thenReturn(relation(true));
        when(followMapper.countFollowers(TARGET_USER_ID)).thenReturn(10L);

        FollowStateVO result = service.follow(
                CURRENT_USER_ID, TARGET_USER_ID, ActionStatus.DEACTIVATE);

        verify(followMapper, never()).insert(any(UserFollow.class));
        verify(followMapper, never()).restoreById(any());
        verify(followMapper, never()).deactivateById(any());
        verifyNoInteractions(notificationService);
        assertEquals(ActionStatus.DEACTIVATE, result.getStatus());
        assertFalse(result.isMutualFollow());
    }

    @Test
    void activateExistingRelationIsIdempotentAndReportsMutualFollow() {
        prepareActiveTarget();
        when(followMapper.selectIncludingDeletedForUpdate(CURRENT_USER_ID, TARGET_USER_ID))
                .thenReturn(relation(false));
        when(followMapper.countFollowers(TARGET_USER_ID)).thenReturn(10L);
        when(followMapper.selectCount(any())).thenReturn(1L);

        FollowStateVO result = service.follow(
                CURRENT_USER_ID, TARGET_USER_ID, ActionStatus.ACTIVATE);

        verify(followMapper, never()).insert(any(UserFollow.class));
        verify(followMapper, never()).restoreById(any());
        verify(followMapper, never()).deactivateById(any());
        verifyNoInteractions(notificationService);
        assertEquals(ActionStatus.ACTIVATE, result.getStatus());
        assertTrue(result.isMutualFollow());
    }

    @Test
    void deactivateExistingRelationDeletesItAndRemovesNotification() {
        prepareActiveTarget();
        when(followMapper.selectIncludingDeletedForUpdate(CURRENT_USER_ID, TARGET_USER_ID))
                .thenReturn(relation(false));
        when(followMapper.deactivateById(FOLLOW_ID)).thenReturn(1);
        when(followMapper.countFollowers(TARGET_USER_ID)).thenReturn(9L);
        // 取消关注后，正向关系查询为 0，&& 短路，不再查询反向关系。
        when(followMapper.selectCount(any())).thenReturn(0L);

        FollowStateVO result = service.follow(
                CURRENT_USER_ID, TARGET_USER_ID, ActionStatus.DEACTIVATE);

        verify(followMapper).deactivateById(FOLLOW_ID);
        verify(notificationService).remove(
                TARGET_USER_ID,
                CURRENT_USER_ID,
                UserNotificationType.FOLLOW,
                UserNotificationSendTo.USER,
                CURRENT_USER_ID
        );
        verify(followMapper, never()).insert(any(UserFollow.class));
        verify(followMapper, never()).restoreById(any());
        assertEquals(ActionStatus.DEACTIVATE, result.getStatus());
        assertEquals(9L, result.getFollowerCount());
        assertFalse(result.isMutualFollow());
        verify(followMapper).selectCount(any());
    }

    @Test
    void mutualFollowQueriesBothDirections() {
        prepareActiveTarget();
        when(followMapper.selectIncludingDeletedForUpdate(CURRENT_USER_ID, TARGET_USER_ID))
                .thenReturn(relation(false));
        when(followMapper.countFollowers(TARGET_USER_ID)).thenReturn(10L);
        when(followMapper.selectCount(any())).thenReturn(1L);

        FollowStateVO result = service.follow(
                CURRENT_USER_ID, TARGET_USER_ID, ActionStatus.ACTIVATE);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<UserFollow>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(followMapper, times(2)).selectCount(wrapperCaptor.capture());
        List<LambdaQueryWrapper<UserFollow>> queries = wrapperCaptor.getAllValues();

        assertEquals(
                List.of(CURRENT_USER_ID, TARGET_USER_ID),
                queryParameters(queries.get(0))
        );
        assertEquals(
                List.of(TARGET_USER_ID, CURRENT_USER_ID),
                queryParameters(queries.get(1))
        );
        assertTrue(result.isMutualFollow());
    }

    @Test
    void nullStatusIsRejectedBeforeChangingFollowState() {
        UserInfo target = new UserInfo();
        target.setId(TARGET_USER_ID);
        target.setStatus(UserInfoStatus.ACTIVE);
        // 正确实现应在查询目标用户前拒绝 null；lenient 避免修复后出现无用 Stub 报错。
        lenient().when(userInfoMapper.selectById(TARGET_USER_ID)).thenReturn(target);

        assertThrows(
                RuntimeException.class,
                () -> service.follow(CURRENT_USER_ID, TARGET_USER_ID, null)
        );

        verify(followMapper, never())
                .selectIncludingDeletedForUpdate(CURRENT_USER_ID, TARGET_USER_ID);
        verifyNoInteractions(notificationService);
    }

    @Test
    void cannotFollowSelf() {
        HomeworkException error = assertThrows(
                HomeworkException.class,
                () -> service.follow(CURRENT_USER_ID, CURRENT_USER_ID, ActionStatus.ACTIVATE)
        );

        assertEquals(ResultCodeEnum.PARAM_ERROR, error.getResultCodeEnum());
        verifyNoInteractions(userInfoMapper, followMapper, notificationService);
    }

    @Test
    void inactiveTargetIsRejected() {
        UserInfo target = new UserInfo();
        target.setId(TARGET_USER_ID);
        target.setStatus(UserInfoStatus.DISABLED);
        when(userInfoMapper.selectById(TARGET_USER_ID)).thenReturn(target);

        HomeworkException error = assertThrows(
                HomeworkException.class,
                () -> service.follow(
                        CURRENT_USER_ID, TARGET_USER_ID, ActionStatus.ACTIVATE)
        );

        assertEquals(ResultCodeEnum.APP_ACCOUNT_STATUS_ERROR, error.getResultCodeEnum());
        verifyNoInteractions(followMapper, notificationService);
    }

    @Test
    void restoreFailureRollsBackWithoutCreatingNotification() {
        prepareActiveTarget();
        when(followMapper.selectIncludingDeletedForUpdate(CURRENT_USER_ID, TARGET_USER_ID))
                .thenReturn(relation(true));
        when(followMapper.restoreById(FOLLOW_ID)).thenReturn(0);

        HomeworkException error = assertThrows(
                HomeworkException.class,
                () -> service.follow(
                        CURRENT_USER_ID, TARGET_USER_ID, ActionStatus.ACTIVATE)
        );

        assertEquals(ResultCodeEnum.PARAM_ERROR, error.getResultCodeEnum());
        verifyNoInteractions(notificationService);
        verify(followMapper, never()).countFollowers(any());
    }

    @Test
    void deactivateFailureRollsBackWithoutRemovingNotification() {
        prepareActiveTarget();
        when(followMapper.selectIncludingDeletedForUpdate(CURRENT_USER_ID, TARGET_USER_ID))
                .thenReturn(relation(false));
        when(followMapper.deactivateById(FOLLOW_ID)).thenReturn(0);

        HomeworkException error = assertThrows(
                HomeworkException.class,
                () -> service.follow(
                        CURRENT_USER_ID, TARGET_USER_ID, ActionStatus.DEACTIVATE)
        );

        assertEquals(ResultCodeEnum.PARAM_ERROR, error.getResultCodeEnum());
        verifyNoInteractions(notificationService);
        verify(followMapper, never()).countFollowers(any());
    }

    private void prepareActiveTarget() {
        UserInfo target = new UserInfo();
        target.setId(TARGET_USER_ID);
        target.setStatus(UserInfoStatus.ACTIVE);
        when(userInfoMapper.selectById(TARGET_USER_ID)).thenReturn(target);
    }

    private static UserFollow relation(boolean deleted) {
        UserFollow relation = new UserFollow();
        relation.setId(FOLLOW_ID);
        relation.setFollowerUserId(CURRENT_USER_ID);
        relation.setFolloweeUserId(TARGET_USER_ID);
        relation.setDeleted(deleted);
        return relation;
    }

    private static List<Object> queryParameters(LambdaQueryWrapper<UserFollow> query) {
        // LambdaQueryWrapper 在生成 SQL 片段时才会填充参数映射。
        query.getSqlSegment();
        return List.of(
                query.getParamNameValuePairs().get("MPGENVAL1"),
                query.getParamNameValuePairs().get("MPGENVAL2")
        );
    }

    private void verifyFollowNotificationCreated() {
        verify(notificationService).create(
                TARGET_USER_ID,
                CURRENT_USER_ID,
                UserNotificationType.FOLLOW,
                UserNotificationSendTo.USER,
                CURRENT_USER_ID,
                null,
                "新增关注",
                "有新用户关注了你"
        );
    }
}
