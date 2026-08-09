package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.common.storage.UserImageUrlResolver;
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
    private final UserImageUrlResolver userImageUrlResolver;

    /**
     * 一次性完成主页用户校验、统计数据和当前用户关系的组装，
     * 让调用方只拿到可展示且权限状态完整的公开主页数据。
     */
    @Override
    public PublicUserProfileVO getProfile(Long currentUserId, Long profileUserId) {
        // 公开主页只属于正常启用的用户，先校验可避免继续查询无效用户的关联数据。
        UserInfo user = userInfoMapper.selectById(profileUserId);
        if (user == null || user.getStatus() != UserInfoStatus.ACTIVE) {
            throw new IllegalArgumentException("用户不存在");
        }

        PublicUserProfileCountsVO counts = profileMapper.selectCounts(profileUserId);
        boolean self = Objects.equals(currentUserId, profileUserId);

        // 查看自己时没有关注关系，跳过查询既符合页面语义，也能减少不必要的数据库访问。
        boolean following = false;
        boolean mutualFollow = false;
        if (!self) {
            following = followMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                    .eq(UserFollow::getFollowerUserId, currentUserId)
                    .eq(UserFollow::getFolloweeUserId, profileUserId)) > 0;

            // 只有当前用户已经关注对方时才需要反向检查，以最少查询判断是否互相关注。
            if (following) {
                mutualFollow = followMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerUserId, profileUserId)
                        .eq(UserFollow::getFolloweeUserId, currentUserId)) > 0;
            }
        }

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
        vo.setMutualFollow(mutualFollow);
        vo.setCanFollow(!self);
        vo.setCanSendPrivateMessage(!self);

        // 私聊会话只对“查看他人主页”有意义；统一用户顺序可匹配会话表的唯一记录。
        if (!self) {
            PrivateChatbox box = chatboxMapper.selectOne(new LambdaQueryWrapper<PrivateChatbox>()
                    .eq(PrivateChatbox::getUserAId, Math.min(currentUserId, profileUserId))
                    .eq(PrivateChatbox::getUserBId, Math.max(currentUserId, profileUserId)));
            vo.setChatboxId(box == null ? null : box.getId());
        }
        return vo;
    }

    /**
     * 按分页批量读取指定分类的动态及其关联数据，再连续组装成页面模型，
     * 这样既避免逐条查询造成 N+1 问题，也能在一个方法中看清完整的数据流。
     */
    @Override
    public List<PublicUserProfileActivityVO> listActivities(Long currentUserId, Long profileUserId,
                                                           String tab, Integer pageNum, Integer pageSize) {
        // 动态只能从有效用户的公开主页进入，避免泄露已停用用户的数据。
        UserInfo profileUser = userInfoMapper.selectById(profileUserId);
        if (profileUser == null || profileUser.getStatus() != UserInfoStatus.ACTIVE) {
            throw new IllegalArgumentException("用户不存在");
        }

        if (!TABS.contains(tab)) {
            throw new IllegalArgumentException("未知公开主页分类");
        }

        // 在服务端兜底页码和每页大小，防止异常参数形成无效分页或过大的单次查询。
        long page = pageNum == null ? 1 : Math.max(pageNum, 1);
        long size = pageSize == null ? 20 : Math.min(Math.max(pageSize, 1), 100);
        List<PublicUserProfileActivityRowVO> rows =
                profileMapper.listActivities(profileUserId, tab, (page - 1) * size, size);
        if (rows.isEmpty()) {
            return List.of();
        }

        // 先收集本页所有外键再批量查询，避免为每一条动态分别访问数据库。
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

        // 当前用户的点赞、收藏、转发状态需要随动态返回，批量读取后按帖子分组便于组装。
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

        // 按原查询顺序组装页面结果，使每条动态的帖子、评论和交互状态都在同一段代码中可见。
        List<PublicUserProfileActivityVO> activities = new ArrayList<>(rows.size());
        for (PublicUserProfileActivityRowVO row : rows) {
            PublicUserProfileActivityVO activity = new PublicUserProfileActivityVO();
            activity.setActivityType(row.getActivityType());
            activity.setActivityTime(row.getActivityTime());

            HitPost post = posts.get(row.getPostId());
            if (post != null) {
                UserInfo postAuthor = users.get(post.getPostUserId());
                Set<HitActionType> postActions =
                        currentActions.getOrDefault(post.getId(), Set.of());

                HitPostVO postVO = new HitPostVO();
                postVO.setPostId(post.getId());
                postVO.setUserId(post.getPostUserId());
                postVO.setDisplayName(postAuthor == null ? "该用户已注销" : postAuthor.getDisplayName());
                postVO.setAvatar(postAuthor == null ? null : resolveAvatar(postAuthor));
                postVO.setContent(post.getContent());

                // 标签字段是 JSON；脏数据不应阻断整页动态，因此解析失败时按无标签展示。
                List<String> tags = List.of();
                String tagsJson = post.getTagsJson();
                if (tagsJson != null && !tagsJson.isBlank()) {
                    try {
                        tags = objectMapper.readValue(
                                tagsJson,
                                objectMapper.getTypeFactory()
                                        .constructCollectionType(List.class, String.class));
                    } catch (JsonProcessingException ignored) {
                        tags = List.of();
                    }
                }
                postVO.setTags(tags);
                postVO.setCommentCount(post.getCommentCount());
                postVO.setLikeCount(post.getLikeCount());
                postVO.setFavoriteCount(post.getFavoriteCount());
                postVO.setRepostCount(post.getRepostCount());
                postVO.setLiked(postActions.contains(HitActionType.LIKE));
                postVO.setFavorited(postActions.contains(HitActionType.FAVORITE));
                postVO.setReposted(postActions.contains(HitActionType.REPOST));
                postVO.setCreatedTime(post.getCreatedTime());
                activity.setPost(postVO);
            }

            if (row.getCommentId() != null) {
                HitComment comment = comments.get(row.getCommentId());
                if (comment != null) {
                    UserInfo commentAuthor = users.get(comment.getCommentUserId());

                    HitCommentVO commentVO = new HitCommentVO();
                    commentVO.setCommentId(comment.getId());
                    commentVO.setPostId(comment.getPostId());
                    commentVO.setCommentUserId(comment.getCommentUserId());
                    commentVO.setDisplayName(commentAuthor == null
                            ? "该用户已注销" : commentAuthor.getDisplayName());
                    commentVO.setAvatar(commentAuthor == null ? null : resolveAvatar(commentAuthor));
                    commentVO.setParentCommentId(comment.getParentCommentId());
                    commentVO.setComment(comment.getComment());
                    commentVO.setLikeCount(comment.getLikeCount() == null ? 0 : comment.getLikeCount());
                    commentVO.setLiked(currentCommentLikes.contains(comment.getId()));
                    commentVO.setCreatedTime(comment.getCreatedTime());
                    activity.setComment(commentVO);
                }
            }
            activities.add(activity);
        }
        return activities;
    }

    /**
     * 只搜索可被当前用户提及的有效用户，并限制最大返回数量，
     * 以保证联想列表相关、响应稳定且不会把当前用户自己重复列出。
     */
    @Override
    public List<MentionUserVO> searchUsers(Long currentUserId, String keyword, Integer limit) {
        String value = keyword == null ? "" : keyword.trim();
        if (value.isEmpty()) {
            return List.of();
        }

        // 联想搜索默认返回 10 条且最多 20 条，兼顾输入体验和数据库查询成本。
        int size = Math.min(limit == null ? 10 : Math.max(limit, 1), 20);
        return userInfoMapper.selectList(new LambdaQueryWrapper<UserInfo>()
                        .eq(UserInfo::getStatus, UserInfoStatus.ACTIVE)
                        .ne(UserInfo::getId, currentUserId)
                        .and(wrapper -> wrapper.likeRight(UserInfo::getAccountNo, value)
                                .or().like(UserInfo::getDisplayName, value))
                        .last("LIMIT " + size))
                .stream()
                .map(user -> {
                    MentionUserVO vo = new MentionUserVO();
                    vo.setUserId(user.getId());
                    vo.setAccountNo(user.getAccountNo());
                    vo.setDisplayName(user.getDisplayName());
                    vo.setAvatar(resolveAvatar(user));
                    return vo;
                })
                .toList();
    }
    private String resolveAvatar(UserInfo userInfo) {
        return userImageUrlResolver.resolveAvatar(userInfo.getAvatarObjectKey());
    }
}
