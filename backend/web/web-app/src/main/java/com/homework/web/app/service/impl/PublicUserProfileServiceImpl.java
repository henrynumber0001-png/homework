package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.model.entity.*;
import com.homework.model.enums.*;
import com.homework.web.app.mapper.*;
import com.homework.web.app.service.PublicUserProfileService;
import com.homework.web.app.service.UserCenterService;
import com.homework.web.app.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicUserProfileServiceImpl implements PublicUserProfileService {
    private static final Set<String> TABS = Set.of("posts", "commented", "liked", "favorite");

    private final UserInfoMapper userInfoMapper;
    private final UserFollowMapper followMapper;
    private final PrivateChatboxMapper chatboxMapper;
    private final PublicUserProfileMapper profileMapper;
    private final HitPostMapper postMapper;
    private final HitCommentMapper commentMapper;
    private final HitActionMapper actionMapper;
    private final HitCommentLikeMapper commentLikeMapper;
    private final UserCenterService userCenterService;
    private final ObjectMapper objectMapper;

    @Override
    public PublicUserProfileVO getProfile(Long currentUserId, Long profileUserId) {
        UserInfo user = activeUser(profileUserId);
        PublicUserProfileCountsVO counts = profileMapper.selectCounts(profileUserId);
        boolean self = Objects.equals(currentUserId, profileUserId);
        boolean following = !self && follows(currentUserId, profileUserId);

        PublicUserProfileVO vo = new PublicUserProfileVO();
        vo.setUserId(user.getId());
        vo.setMembershipInfoVO(userCenterService.getMembershipInfo(profileUserId));
        vo.setFollowerCount(counts.getFollowerCount());
        vo.setFollowingCount(counts.getFollowingCount());
        vo.setPostCount(counts.getPostCount());
        vo.setAnsweredQuestionCount(counts.getAnsweredQuestionCount());
        vo.setLearnedBankCount(counts.getLearnedBankCount());
        vo.setStudyHours(Math.round(counts.getStudySeconds() / 3600.0));
        vo.setReceivedTotalActionCount(counts.getReceivedTotalActionCount());
        vo.setSelf(self);
        vo.setFollowedByCurrentUser(self ? null : following);
        vo.setMutualFollow(!self && following && follows(profileUserId, currentUserId));
        vo.setCanFollow(!self);
        vo.setCanSendPrivateMessage(!self);
        if (!self) {
            PrivateChatbox box = chatboxMapper.selectOne(new LambdaQueryWrapper<PrivateChatbox>()
                    .eq(PrivateChatbox::getUserAId, Math.min(currentUserId, profileUserId))
                    .eq(PrivateChatbox::getUserBId, Math.max(currentUserId, profileUserId)));
            vo.setChatboxId(box == null ? null : box.getId());
        }
        return vo;
    }

    @Override
    public List<PublicUserProfileActivityVO> listActivities(Long currentUserId, Long profileUserId,
                                                           String tab, Integer pageNum, Integer pageSize) {
        activeUser(profileUserId);
        if (!TABS.contains(tab)) throw new IllegalArgumentException("未知公开主页分类");
        long page = pageNum == null ? 1 : Math.max(pageNum, 1);
        long size = pageSize == null ? 20 : Math.min(Math.max(pageSize, 1), 100);
        List<PublicUserProfileActivityRowVO> rows =
                profileMapper.listActivities(profileUserId, tab, (page - 1) * size, size);
        if (rows.isEmpty()) return List.of();

        Set<Long> postIds = rows.stream().map(PublicUserProfileActivityRowVO::getPostId)
                .collect(Collectors.toSet());
        Set<Long> commentIds = rows.stream().map(PublicUserProfileActivityRowVO::getCommentId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, HitPost> posts = postMapper.selectByIds(postIds).stream()
                .collect(Collectors.toMap(HitPost::getId, post -> post));
        Map<Long, HitComment> comments = commentIds.isEmpty() ? Map.of()
                : commentMapper.selectByIds(commentIds).stream()
                .collect(Collectors.toMap(HitComment::getId, comment -> comment));

        Set<Long> authorIds = posts.values().stream().map(HitPost::getPostUserId)
                .collect(Collectors.toSet());
        comments.values().stream().map(HitComment::getCommentUserId).forEach(authorIds::add);
        Map<Long, UserInfo> users = authorIds.isEmpty() ? Map.of()
                : userInfoMapper.selectByIds(authorIds).stream()
                .collect(Collectors.toMap(UserInfo::getId, user -> user));

        Map<Long, Set<HitActionType>> currentActions = actionMapper.selectList(new LambdaQueryWrapper<HitAction>()
                        .eq(HitAction::getActionUserId, currentUserId)
                        .in(HitAction::getPostId, postIds))
                .stream().collect(Collectors.groupingBy(
                        HitAction::getPostId,
                        Collectors.mapping(HitAction::getActionType, Collectors.toSet())));
        Set<Long> currentCommentLikes = commentIds.isEmpty() ? Set.of()
                : commentLikeMapper.selectList(new LambdaQueryWrapper<HitCommentLike>()
                        .eq(HitCommentLike::getActionUserId, currentUserId)
                        .in(HitCommentLike::getCommentId, commentIds))
                .stream().map(HitCommentLike::getCommentId)
                .collect(Collectors.toSet());

        return rows.stream().map(row -> activityVO(row, posts, comments, users,
                currentActions, currentCommentLikes)).toList();
    }

    @Override
    public List<MentionUserVO> searchUsers(Long currentUserId, String keyword, Integer limit) {
        String value = keyword == null ? "" : keyword.trim();
        if (value.isEmpty()) return List.of();
        int size = Math.min(limit == null ? 10 : Math.max(limit, 1), 20);
        return userInfoMapper.selectList(new LambdaQueryWrapper<UserInfo>()
                        .eq(UserInfo::getStatus, UserInfoStatus.ACTIVE)
                        .eq(UserInfo::getUserRole, UserInfoUserRole.USER)
                        .ne(UserInfo::getId, currentUserId)
                        .and(w -> w.likeRight(UserInfo::getAccountNo, value)
                                .or().like(UserInfo::getDisplayName, value))
                        .last("LIMIT " + size))
                .stream().map(user -> {
                    MentionUserVO vo = new MentionUserVO();
                    vo.setUserId(user.getId());
                    vo.setAccountNo(user.getAccountNo());
                    vo.setDisplayName(user.getDisplayName());
                    vo.setAvatar(user.getAvatar());
                    return vo;
                }).toList();
    }

    private PublicUserProfileActivityVO activityVO(
            PublicUserProfileActivityRowVO row,
            Map<Long, HitPost> posts,
            Map<Long, HitComment> comments,
            Map<Long, UserInfo> users,
            Map<Long, Set<HitActionType>> currentActions,
            Set<Long> currentCommentLikes) {
        PublicUserProfileActivityVO vo = new PublicUserProfileActivityVO();
        vo.setActivityType(row.getActivityType());
        vo.setActivityTime(row.getActivityTime());
        HitPost post = posts.get(row.getPostId());
        if (post != null) {
            vo.setPost(postVO(post, users.get(post.getPostUserId()),
                    currentActions.getOrDefault(post.getId(), Set.of())));
        }
        if (row.getCommentId() != null) {
            HitComment comment = comments.get(row.getCommentId());
            if (comment != null) {
                vo.setComment(commentVO(comment, users.get(comment.getCommentUserId()),
                        currentCommentLikes.contains(comment.getId())));
            }
        }
        return vo;
    }

    private HitPostVO postVO(HitPost post, UserInfo author, Set<HitActionType> currentActions) {
        HitPostVO vo = new HitPostVO();
        vo.setPostId(post.getId());
        vo.setUserId(post.getPostUserId());
        vo.setDisplayName(author == null ? "该用户已注销" : author.getDisplayName());
        vo.setAvatar(author == null ? null : author.getAvatar());
        vo.setContent(post.getContent());
        vo.setTags(parseTags(post.getTagsJson()));
        vo.setCommentCount(post.getCommentCount());
        vo.setLikeCount(post.getLikeCount());
        vo.setFavoriteCount(post.getFavoriteCount());
        vo.setRepostCount(post.getRepostCount());
        vo.setLiked(currentActions.contains(HitActionType.LIKE));
        vo.setFavorited(currentActions.contains(HitActionType.FAVORITE));
        vo.setReposted(currentActions.contains(HitActionType.REPOST));
        vo.setCreatedTime(post.getCreatedTime());
        return vo;
    }

    private HitCommentVO commentVO(HitComment comment, UserInfo author, boolean liked) {
        HitCommentVO vo = new HitCommentVO();
        vo.setCommentId(comment.getId());
        vo.setPostId(comment.getPostId());
        vo.setCommentUserId(comment.getCommentUserId());
        vo.setDisplayName(author == null ? "该用户已注销" : author.getDisplayName());
        vo.setAvatar(author == null ? null : author.getAvatar());
        vo.setParentCommentId(comment.getParentCommentId());
        vo.setComment(comment.getComment());
        vo.setLikeCount(comment.getLikeCount() == null ? 0 : comment.getLikeCount());
        vo.setLiked(liked);
        vo.setCreatedTime(comment.getCreatedTime());
        return vo;
    }

    private List<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException ignored) {
            return List.of();
        }
    }

    private UserInfo activeUser(Long userId) {
        UserInfo user = userInfoMapper.selectById(userId);
        if (user == null || user.getStatus() != UserInfoStatus.ACTIVE
                || user.getUserRole() != UserInfoUserRole.USER) {
            throw new IllegalArgumentException("用户不存在");
        }
        return user;
    }

    private boolean follows(Long follower, Long followee) {
        return followMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerUserId, follower)
                .eq(UserFollow::getFolloweeUserId, followee)) > 0;
    }
}
