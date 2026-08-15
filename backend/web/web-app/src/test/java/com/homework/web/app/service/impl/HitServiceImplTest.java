package com.homework.web.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.common.storage.UserImageUrlResolver;
import com.homework.model.entity.HitAction;
import com.homework.model.entity.HitComment;
import com.homework.model.entity.HitCommentLike;
import com.homework.model.entity.HitPost;
import com.homework.model.enums.ActionStatus;
import com.homework.model.enums.HitActionType;
import com.homework.model.enums.HitPostStatus;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.dto.HitActionDTO;
import com.homework.web.app.dto.HitCommentLikeDTO;
import com.homework.web.app.dto.HitPostCreateDTO;
import com.homework.web.app.mapper.*;
import com.homework.web.app.service.NotificationService;
import com.homework.web.app.service.CommunityAccessService;
import com.homework.web.app.vo.HitCommentLikeResultVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HitServiceImplTest {

    @Mock
    private HitPostMapper hitPostMapper;
    @Mock
    private HitCommentMapper hitCommentMapper;
    @Mock
    private HitActionMapper hitActionMapper;
    @Mock
    private UserInfoMapper userInfoMapper;
    @Mock
    private HitCommentLikeMapper hitCommentLikeMapper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private CommunityAccessService communityAccessService;
    @Mock
    private UserImageUrlResolver userImageUrlResolver;

    private HitServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HitServiceImpl(hitPostMapper, hitCommentMapper, hitActionMapper,
                userInfoMapper, hitCommentLikeMapper, notificationService, new ObjectMapper(),
                communityAccessService, userImageUrlResolver);
        LoginUserHolder.setUserId(7L);
    }

    @AfterEach
    void tearDown() {
        LoginUserHolder.removeUserId();
    }

    @Test
    void publishCountsEmojiAsOneUnicodeCharacter() {
        // 139 个汉字加一个补充平面 emoji：Java char 长度为 141，但用户感知长度是 140。
        String content = "学".repeat(139) + "🚀";
        when(hitPostMapper.insert(any(HitPost.class))).thenAnswer(invocation -> {
            HitPost post = invocation.getArgument(0);
            post.setId(100L);
            return 1;
        });

        Long id = service.publish(post(content));

        assertEquals(100L, id);
        verify(hitPostMapper).insert(org.mockito.ArgumentMatchers.<HitPost>argThat(
                post -> post.getPostStatus() == HitPostStatus.PUBLISHED
                && post.getContent().codePointCount(0, post.getContent().length()) == 140));
    }

    @Test
    void publishRejectsMoreThan140UnicodeCharacters() {
        String content = "打".repeat(141);

        HomeworkException error = assertThrows(HomeworkException.class,
                () -> service.publish(post(content)));

        assertEquals(ResultCodeEnum.HIT_CONTENT_TOO_LONG_ERROR, error.getResultCodeEnum());
        verifyNoInteractions(hitPostMapper);
    }

    @Test
    void activateLikeInsertsActionAndIncreasesCounter() {
        HitPost post = publishedPost(100L, 9L, 1);
        when(hitPostMapper.selectOne(any())).thenReturn(post);
        when(hitPostMapper.lockPublishedPost(100L)).thenReturn(100L);
        when(hitActionMapper.selectIncludingDeletedForUpdate(100L, 7L, 1)).thenReturn(null);
        when(hitActionMapper.insert(any(HitAction.class))).thenReturn(1);
        when(hitPostMapper.changeActionCounters(100L, 1, 0, 0)).thenReturn(1);
        when(hitPostMapper.selectById(100L)).thenReturn(post);

        HitActionDTO dto = new HitActionDTO();
        dto.setActionType(HitActionType.LIKE);
        dto.setActionStatus(ActionStatus.ACTIVATE);

        service.action(100L, dto);

        verify(hitActionMapper).insert(org.mockito.ArgumentMatchers.<HitAction>argThat(action ->
                action.getPostId().equals(100L)
                        && action.getActionUserId().equals(7L)
                        && action.getActionType() == HitActionType.LIKE));
        verify(hitPostMapper).changeActionCounters(100L, 1, 0, 0);
    }

    @Test
    void deactivateLikeDeletesActionAndDecreasesCounter() {
        HitPost post = publishedPost(100L, 9L, 0);
        HitAction existing = new HitAction();
        existing.setId(50L);
        existing.setPostId(100L);
        existing.setActionUserId(7L);
        existing.setActionType(HitActionType.LIKE);
        existing.setDeleted(false);

        when(hitPostMapper.selectOne(any())).thenReturn(post);
        when(hitPostMapper.lockPublishedPost(100L)).thenReturn(100L);
        when(hitActionMapper.selectIncludingDeletedForUpdate(100L, 7L, 1)).thenReturn(existing);
        when(hitActionMapper.deactivateById(50L)).thenReturn(1);
        when(hitPostMapper.changeActionCounters(100L, -1, 0, 0)).thenReturn(1);
        when(hitPostMapper.selectById(100L)).thenReturn(post);

        HitActionDTO dto = new HitActionDTO();
        dto.setActionType(HitActionType.LIKE);
        dto.setActionStatus(ActionStatus.DEACTIVATE);

        service.action(100L, dto);

        verify(hitActionMapper).deactivateById(50L);
        verify(hitPostMapper).changeActionCounters(100L, -1, 0, 0);
    }

    @Test
    void activateDeletedLikeRestoresExistingRowInsteadOfInsertingDuplicate() {
        HitPost post = publishedPost(100L, 9L, 0);
        HitAction deletedAction = new HitAction();
        deletedAction.setId(50L);
        deletedAction.setPostId(100L);
        deletedAction.setActionUserId(7L);
        deletedAction.setActionType(HitActionType.LIKE);
        deletedAction.setDeleted(true);

        when(hitPostMapper.selectOne(any())).thenReturn(post);
        when(hitPostMapper.lockPublishedPost(100L)).thenReturn(100L);
        when(hitActionMapper.selectIncludingDeletedForUpdate(100L, 7L, 1)).thenReturn(deletedAction);
        when(hitActionMapper.restoreById(50L)).thenReturn(1);
        when(hitPostMapper.changeActionCounters(100L, 1, 0, 0)).thenReturn(1);
        when(hitPostMapper.selectById(100L)).thenReturn(post);

        HitActionDTO dto = new HitActionDTO();
        dto.setActionType(HitActionType.LIKE);
        dto.setActionStatus(ActionStatus.ACTIVATE);

        service.action(100L, dto);

        verify(hitActionMapper).restoreById(50L);
        verify(hitActionMapper, never()).insert(any(HitAction.class));
        verify(hitPostMapper).changeActionCounters(100L, 1, 0, 0);
    }

    @Test
    void commentLikeCreatesReceivedNotificationWithContainingPost() {
        HitPost post = publishedPost(100L, 3L, 0);
        HitComment comment = new HitComment();
        comment.setId(44L);
        comment.setPostId(100L);
        comment.setCommentUserId(9L);
        comment.setComment("有帮助");
        comment.setLikeCount(0);
        comment.setCommentStatus(HitPostStatus.PUBLISHED);
        when(hitPostMapper.selectById(100L)).thenReturn(post);
        when(hitCommentMapper.selectById(44L)).thenReturn(comment);
        when(hitCommentMapper.lockActive(44L)).thenReturn(44L);
        when(hitCommentLikeMapper.selectIncludingDeletedForUpdate(44L, 7L)).thenReturn(null);
        when(hitCommentLikeMapper.insert(any(HitCommentLike.class))).thenReturn(1);
        when(hitCommentMapper.changeLikeCount(44L, 1)).thenAnswer(invocation -> {
            comment.setLikeCount(1);
            return 1;
        });

        HitCommentLikeDTO dto = new HitCommentLikeDTO();
        dto.setActionStatus(ActionStatus.ACTIVATE);
        HitCommentLikeResultVO result = service.commentLike(100L, 44L, dto);

        assertTrue(result.isLiked());
        assertEquals(1, result.getLikeCount());
        verify(notificationService).create(9L, 7L, com.homework.model.enums.UserNotificationType.LIKE,
                com.homework.model.enums.UserNotificationSendTo.HIT_COMMENT,
                44L, 100L, "评论被点赞", "有帮助");
    }

    private HitPost publishedPost(Long postId, Long postUserId, int likeCount) {
        HitPost post = new HitPost();
        post.setId(postId);
        post.setPostUserId(postUserId);
        post.setPostStatus(HitPostStatus.PUBLISHED);
        post.setContent("测试 Hit #Java");
        post.setLikeCount(likeCount);
        post.setFavoriteCount(0);
        post.setRepostCount(0);
        return post;
    }

    private HitPostCreateDTO post(String content) {
        HitPostCreateDTO dto = new HitPostCreateDTO();
        dto.setContent(content);
        return dto;
    }
}
