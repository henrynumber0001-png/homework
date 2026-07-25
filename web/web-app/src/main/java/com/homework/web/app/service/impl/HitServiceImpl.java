package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.*;
import com.homework.model.enums.*;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.dto.HitActionDTO;
import com.homework.web.app.dto.HitCommentCreateDTO;
import com.homework.web.app.dto.HitPostCreateDTO;
import com.homework.web.app.dto.HitCommentLikeDTO;
import com.homework.web.app.mapper.*;
import com.homework.web.app.service.HitService;
import com.homework.web.app.service.NotificationService;
import com.homework.web.app.vo.HitCommentVO;
import com.homework.web.app.vo.HitCommentLikeResultVO;
import com.homework.web.app.vo.HitActionResultVO;
import com.homework.web.app.vo.HitPostVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HitServiceImpl implements HitService {

    private static final int MAX_POST_LENGTH = 140;
    private static final int MAX_COMMENT_LENGTH = 300;
    private static final int MAX_TAG_COUNT = 10;
    private static final int MAX_TAG_LENGTH = 30;

    private final HitPostMapper hitPostMapper;
    private final HitCommentMapper hitCommentMapper;
    private final HitActionMapper hitActionMapper;
    private final UserInfoMapper userInfoMapper;
    private final HitCommentLikeMapper hitCommentLikeMapper;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    /**
     * 匹配以 # 开头的标签。
     * <p>
     * \p{L}：所有语言的文字，包括英文和中文。
     * \p{N}：数字。
     * _：下划线。
     */
    private static final Pattern HASHTAG_PATTERN =
            Pattern.compile("#([\\p{L}\\p{N}_]+)");

    /**
     * 查询公共 Hit 时间线。
     * 只展示已发布动态，并使用 created_time、id 双重倒序保证稳定的 newest-to-oldest 顺序。
     * 同一秒发布的动态会继续按主键倒序，因此分页时不会随机换位。
     */
    @Override
    public List<HitPostVO> listHits(Integer pageNum, Integer pageSize) {

        Long viewerUserId = LoginUserHolder.getUserId();
        // normalizePage 和 normalizePageSize 会处理 null、负数以及过大的 pageSize。
        // false 表示 只执行查询当前页数据的 SQL，不再查询总记录数。
        // 创建空的分页容器
        Page<HitPost> page = new Page<>(normalizePage(pageNum), normalizePageSize(pageSize), false);

        LambdaQueryWrapper<HitPost> postQueryWrapper = new LambdaQueryWrapper<>();
        postQueryWrapper.eq(HitPost::getPostStatus, HitPostStatus.PUBLISHED)
                .orderByDesc(HitPost::getCreatedTime)
                .orderByDesc(HitPost::getId);

        //将当前页的posts 数据 转换为 List集合
        //这里是分页查询的逻辑，不是按userId去查找特定post
        List<HitPost> posts = hitPostMapper.selectPage(page, postQueryWrapper).getRecords();
        if (posts.isEmpty()) {
            return List.of(); // 返回不可修改的空列表，而不是返回 null，方便前端直接遍历。
        }

        //用Set集合的目的，是为了去重：因为这里收集的是“需要查询的 userId”，同一个用户可能发布多条 Hit，所以需要去重。
        Set<Long> postUserIds = posts.stream().map(HitPost::getPostUserId).collect(Collectors.toSet());
        List<Long> postIds = posts.stream().map(HitPost::getId).toList();

        //如果 userInfos 为空，没必要抛异常。因为影响的只是avatar 和 displayName 的值，就算为 null 也可以照常执行。
        List<UserInfo> userInfos = userInfoMapper.selectByIds(postUserIds);
        Map<Long, UserInfo> postUserMap = userInfos.stream().collect(Collectors.toMap(UserInfo::getId, Function.identity()));


        LambdaQueryWrapper<HitAction> hitActionQueryWrapper = new LambdaQueryWrapper<>();
        hitActionQueryWrapper.in(HitAction::getPostId, postIds)
                .eq(HitAction::getActionUserId, viewerUserId);

        //当前用户在这些postIds里面，有哪些hitActions
        //没查到任何行数据，允许返回空列表
        List<HitAction> hitActions = hitActionMapper.selectList(hitActionQueryWrapper);

        //如果 hitActions 是空List集合,那么 viewerActionMap 也是空map集合，相当于一个空集合.stream()，最终得到一个空Map集合
        //因为viewer没有 like,favorite,repost动作，就不会有 hit_action的行数据，就不会有postId
        //Map<postId, Set<HitActionType>> 表示：当前浏览用户 对 某一条post 做过的互动类型集合，如果一次互动都没有，就返回一个空的 Set。
        //允许 viewerActionMap 为空集合（注意：空集合 = Map.of(), 不等于 null）
        Map<Long, Set<HitActionType>> viewerActionMap = hitActions.stream()
                .collect(Collectors.groupingBy(HitAction::getPostId, Collectors.mapping(HitAction::getActionType, Collectors.toSet())));
        /*
        使用了两个 Collectors 收集器（只希望保存每条操作的 actionType，所以使用了第二个收集器 mapping()）
        groupingBy()：按照 postId 分组
        mapping()：把 HitAction 转换成 HitActionType：根据每个查询出来的 hitAction，提取这个 viewerId 下的每个 postId 的 HitAction 对象中的 actionType，然后按照 postId为一组，放入 Set 集合。
        toSet()：把操作类型收集为 Set
         */


        List<HitPostVO> hitPostVOs = new ArrayList<>();
        posts.forEach(post -> {
            HitPostVO vo = new HitPostVO();
            UserInfo userInfo = postUserMap.get(post.getPostUserId());
            Set<HitActionType> hitActionTypeSet = viewerActionMap.getOrDefault(post.getId(), Set.of());
            //正常的get(post.getId()), 如果查不到值，返回的是null，那么后面的(hitActionTypeSet.contains(HitActionType.FAVORITE)会报NPE

            vo.setPostId(post.getId());
            vo.setUserId(post.getPostUserId());
            vo.setTags(parseTags(post.getTagsJson()));
            vo.setCommentCount(post.getCommentCount());
            vo.setLikeCount(post.getLikeCount());
            vo.setFavoriteCount(post.getFavoriteCount());
            vo.setRepostCount(post.getRepostCount());
            vo.setContent(post.getContent());
            vo.setCreatedTime(post.getCreatedTime());
            vo.setAvatar(userInfo == null ? null : userInfo.getAvatar());
            vo.setDisplayName(userInfo == null ? "该用户已注销" : userInfo.getDisplayName());
            vo.setLiked(hitActionTypeSet.contains(HitActionType.LIKE));
            vo.setFavorited(hitActionTypeSet.contains(HitActionType.FAVORITE));
            vo.setReposted(hitActionTypeSet.contains(HitActionType.REPOST));
            hitPostVOs.add(vo);

        });
        return hitPostVOs;
    }

    /**
     * 查询某条 Hit 的评论。
     * 评论默认按从早到晚排列，便于前端按照自然对话顺序渲染回复。
     */
    @Override
    public List<HitCommentVO> listComments(Long postId, Integer pageNum, Integer pageSize) {

        Long viewerUserId = LoginUserHolder.getUserId();
        // 先确认动态存在且处于已发布状态，避免查询隐藏或已删除动态的评论。
        if (postId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        HitPost post = hitPostMapper.selectById(postId);
        if (post == null || post.getPostStatus() != HitPostStatus.PUBLISHED) {
            throw new HomeworkException(ResultCodeEnum.HIT_NOT_EXIST);
        }

        // 把空页码或小于 1 的页码修正为第 1 页。
        // 把每页数量限制在 1～100 之间。
        // 不查询评论总数，减少一次 COUNT SQL。
        Page<HitComment> page = new Page<>(normalizePage(pageNum), normalizePageSize(pageSize), false);

        LambdaQueryWrapper<HitComment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HitComment::getPostId, postId)
                .orderByAsc(HitComment::getCreatedTime)
                .orderByAsc(HitComment::getId);
        List<HitComment> comments = hitCommentMapper.selectPage(page, queryWrapper).getRecords();
        //允许空列表，表示这条post下面没有评论
        if (comments.isEmpty()) {
            return List.of();
        }

        //查询的是 评论这条post的用户的ID，不是post作者的ID
        //首先，如果comments不为空，那么commentUserIds也不会为空，因为commentUserId应该被设置为not null
        //但是，通过commentUserIds，是有可能查不到对应的 userInfos 的，比如用户账号被删除、数据库存在历史脏数据、commentUserId是null或错误
        Set<Long> commentUserIds = comments.stream().map(HitComment::getCommentUserId).collect(Collectors.toSet());

        //但是你没办法在这一步校验，因为不论 userInfos 是否为空，你都没必要在这一步做非空判断
        //如果为空，那么UserInfo userInfo = null；如果不为空，那么遍历中的UserInfo userInfo还是有可能等于null（因为你不能保证通过每一个commentUserId都能查到userInfo
        //所以，不用在这做非空判断
        List<UserInfo> userInfos = userInfoMapper.selectByIds(commentUserIds);
        Map<Long, UserInfo> commentUserMap = userInfos.stream().collect(Collectors.toMap(UserInfo::getId, Function.identity()));

        Set<Long> likedCommentIds = hitCommentLikeMapper.selectList(new LambdaQueryWrapper<HitCommentLike>()
                        .in(HitCommentLike::getCommentId, comments.stream().map(HitComment::getId).toList())
                        .eq(HitCommentLike::getActionUserId, viewerUserId))
                .stream().map(HitCommentLike::getCommentId).collect(Collectors.toSet());
        List<HitCommentVO> hitCommentVOs = new ArrayList<>();
        comments.forEach(comment -> {
            HitCommentVO vo = new HitCommentVO();
            UserInfo userInfo = commentUserMap.get(comment.getCommentUserId());

            vo.setPostId(postId);
            vo.setCommentId(comment.getId());
            vo.setCommentUserId(comment.getCommentUserId());
            vo.setCreatedTime(comment.getCreatedTime());
            vo.setParentCommentId(comment.getParentCommentId());
            vo.setComment(comment.getComment());
            vo.setLikeCount(comment.getLikeCount() == null ? 0 : comment.getLikeCount());
            vo.setLiked(likedCommentIds.contains(comment.getId()));
            //因为userInfo == null，主要影响的是avatar和displayName的设置，所以在这个地方做if判断即可
            if (userInfo == null) {
                vo.setAvatar(null);
            } else {
                vo.setAvatar(userInfo.getAvatar());
            }
            vo.setDisplayName(userInfo == null ? "该用户已注销" : userInfo.getDisplayName());
            hitCommentVOs.add(vo);
        });
        return hitCommentVOs;

    }


    //发布HIT
    @Override
    @Transactional
    public Long publish(HitPostCreateDTO dto) {

        Long postUserId = LoginUserHolder.getUserId();

        if (dto == null) throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        String normalizeContent = normalizeContent(dto.getContent(), MAX_POST_LENGTH);

        //从正文中提取tag 然后序列化成 JSON，例如：["React","Hooks"]
        String tagJson = extractAndSerializeTags(normalizeContent);

        HitPost post = new HitPost();
        post.setPostUserId(postUserId);
        post.setContent(normalizeContent);
        post.setTagsJson(tagJson);
        post.setPostStatus(HitPostStatus.PUBLISHED);
        post.setCommentCount(0);
        post.setLikeCount(0);
        post.setFavoriteCount(0);
        post.setRepostCount(0);
        hitPostMapper.insert(post);
        createMentionNotifications(dto.getMentionedUserIds(), postUserId,
                UserNotificationSendTo.HIT_POST, post.getId(), post.getId(), normalizeContent, Set.of());
        return post.getId();
    }


    //评论HIT
    @Override
    @Transactional
    public Long comment(Long postId, HitCommentCreateDTO dto) {
        if (postId == null || dto == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        Long commentUserId = LoginUserHolder.getUserId();

        // 检查要评论的 HIT 是否存在
        LambdaQueryWrapper<HitPost> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HitPost::getId, postId)
                .eq(HitPost::getPostStatus, HitPostStatus.PUBLISHED);

        HitPost hitPost = hitPostMapper.selectOne(queryWrapper);
        if (hitPost == null) {
            throw new HomeworkException(ResultCodeEnum.HIT_NOT_EXIST);
        }

        String normalizeContent = normalizeComment(dto.getComment(), MAX_COMMENT_LENGTH);

        HitComment parentComment = null; // 默认没有父评论，表示这条评论回复的是post
        if (dto.getParentCommentId() != null) {

            parentComment = hitCommentMapper.selectById(dto.getParentCommentId()); //因为子评论的 parent_comment_id = 父评论的 id，所以通过查询外键，获得 parentComment

            if (parentComment == null || !Objects.equals(parentComment.getPostId(), postId)) { // 检查父评论存在且确实属于当前post
                throw new HomeworkException(ResultCodeEnum.COMMENT_NOT_EXIST);
            }
        }

        HitComment comment = new HitComment();
        comment.setPostId(postId);
        comment.setCommentUserId(commentUserId);
        comment.setParentCommentId(parentComment == null ? null : dto.getParentCommentId()); //有 parent_comment_id，说明回复的是父评论，而非po主
        comment.setComment(normalizeContent);
        hitCommentMapper.insert(comment);
        int result = hitPostMapper.changeCommentCount(postId, 1); // 使用原子 SQL 把动态评论数加 1，避免并发丢失计数。
        if(result != 1){//如果 Post 在校验后被隐藏或删除，评论/互动可能已经写入，但计数没有更新。
            throw new HomeworkException(ResultCodeEnum.HIT_NOT_EXIST);
        }

        // 收到评论通知的userId
        // 如果是针对父评论，那么id就是父评论那一行的 commentUserId, 也就是父评论那一行评论的作者;
        // 如果是针对post，那么id 是这条 post 的 postUserId
        Long noticedUserId = parentComment == null ? hitPost.getPostUserId() : parentComment.getCommentUserId();

        // 创建一条“回复我的”通知。
        createNotification(noticedUserId, commentUserId, UserNotificationType.COMMENT,
                UserNotificationSendTo.HIT_COMMENT, // 点击通知后应跳转到 Hit 评论。
                comment.getId(),
                "回复我的", // 通知标题。
                normalizeContent); // 通知摘要使用评论正文。
        createMentionNotifications(dto.getMentionedUserIds(), commentUserId,
                UserNotificationSendTo.HIT_COMMENT, comment.getId(), postId,
                normalizeContent, Set.of(noticedUserId));
        return comment.getId();
    }

    @Override
    @Transactional
    public HitCommentLikeResultVO commentLike(Long postId, Long commentId, HitCommentLikeDTO dto) {
        if (dto == null || dto.getActionStatus() == null) throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        HitPost post = hitPostMapper.selectById(postId);
        if (post == null || post.getPostStatus() != HitPostStatus.PUBLISHED) {
            throw new HomeworkException(ResultCodeEnum.HIT_NOT_EXIST);
        }
        HitComment comment = hitCommentMapper.selectById(commentId);
        if (comment == null || !Objects.equals(comment.getPostId(), postId) || hitCommentMapper.lockActive(commentId) == null) {
            throw new HomeworkException(ResultCodeEnum.COMMENT_NOT_EXIST);
        }
        Long actionUserId = LoginUserHolder.getUserId();
        HitCommentLike existing = hitCommentLikeMapper.selectIncludingDeletedForUpdate(commentId, actionUserId);
        boolean activate = dto.getActionStatus() == ActionStatus.ACTIVATE;
        boolean changed = false;
        if (activate && existing == null) {
            HitCommentLike like = new HitCommentLike();
            like.setCommentId(commentId);
            like.setActionUserId(actionUserId);
            hitCommentLikeMapper.insert(like);
            changed = true;
        } else if (activate && Boolean.TRUE.equals(existing.getDeleted())) {
            changed = hitCommentLikeMapper.restoreById(existing.getId()) == 1;
        } else if (!activate && existing != null && !Boolean.TRUE.equals(existing.getDeleted())) {
            changed = hitCommentLikeMapper.deactivateById(existing.getId()) == 1;
        }
        if (changed) {
            if (hitCommentMapper.changeLikeCount(commentId, activate ? 1 : -1) != 1) {
                throw new HomeworkException(ResultCodeEnum.COMMENT_NOT_EXIST);
            }
            if (activate) {
                notificationService.create(comment.getCommentUserId(), actionUserId, UserNotificationType.LIKE,
                        UserNotificationSendTo.HIT_COMMENT, commentId, postId, "评论被点赞", comment.getComment());
            } else {
                notificationService.remove(comment.getCommentUserId(), actionUserId, UserNotificationType.LIKE,
                        UserNotificationSendTo.HIT_COMMENT, commentId);
            }
        }
        HitComment updated = hitCommentMapper.selectById(commentId);
        HitCommentLikeResultVO result = new HitCommentLikeResultVO();
        result.setLiked(activate);
        result.setLikeCount(updated == null || updated.getLikeCount() == null ? 0 : updated.getLikeCount());
        return result;
    }

    //这是一个 点赞/收藏/转发 操作的接口，是当前操作用户角度出发的，不是查询这条post下有多少个actionType
    @Override
    @Transactional
    public HitActionResultVO action(Long postId, HitActionDTO dto) {
        if (postId == null || dto == null || dto.getActionType() == null || dto.getActionStatus() == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        Long actionUserId = LoginUserHolder.getUserId();

        // 只有仍处于发布状态的 Hit 才能被互动。
        LambdaQueryWrapper<HitPost> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HitPost::getId, postId)
                .eq(HitPost::getPostStatus, HitPostStatus.PUBLISHED);
        HitPost hitPost = hitPostMapper.selectOne(queryWrapper);
        if (hitPost == null) {
            throw new HomeworkException(ResultCodeEnum.HIT_NOT_EXIST);
        }

        HitActionType actionType = dto.getActionType();
        ActionStatus actionStatus = dto.getActionStatus();

        if(hitPostMapper.lockPublishedPost(postId) == null){
            throw new HomeworkException(ResultCodeEnum.HIT_NOT_EXIST);
        }

        // 查询有效或已取消的历史记录，并在当前事务内锁定它。
        HitAction existing = hitActionMapper.selectIncludingDeletedForUpdate(postId, actionUserId, actionType.getValue());

        boolean changed = false;
        if (existing != null && Boolean.TRUE.equals(existing.getDeleted())) {
            if (actionStatus == ActionStatus.ACTIVATE) {
                int result = hitActionMapper.restoreById(existing.getId());
                if (result != 1) {
                    throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
                }
                changed = true;
            }
        } else if (existing != null && !Boolean.TRUE.equals(existing.getDeleted())) {
            if (actionStatus == ActionStatus.DEACTIVATE) {
                int result = hitActionMapper.deactivateById(existing.getId());
                if (result != 1) {
                    throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
                }
                changed = true;
            }
        } else {
            if (actionStatus == ActionStatus.ACTIVATE) {
                HitAction action = new HitAction();
                action.setPostId(postId);
                action.setActionUserId(actionUserId);
                action.setActionType(actionType);
                int result = hitActionMapper.insert(action);
                if (result != 1) {
                    throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
                }
                changed = true;
            }
        }

        // 只有数据库状态真实改变时才更新计数和通知，重复指令不会重复加减。
        if (changed) {
            //更新这条post的对应的 点赞/收藏/转发 数量的加减变化
            if (actionStatus == ActionStatus.ACTIVATE) {
                changeCounter(postId, actionType, 1);
                //加减变化之后，要通知postUser和actionUser
                createActionNotification(hitPost, actionUserId, actionType);
            } else {
                changeCounter(postId, actionType, -1);
                deleteActionNotification(hitPost, actionUserId, actionType);
            }
        }

        //当 点赞/收藏/转发 的数据加减 已经记录到数据库，并且消息已通知收发人
        //接下来是把更新好的数据反馈给前端一份
        HitPost updatedPost = hitPostMapper.selectById(postId);
        if(updatedPost == null){
            throw new HomeworkException(ResultCodeEnum.HIT_NOT_EXIST);
        }

        HitActionResultVO result = new HitActionResultVO();
        result.setActionType(actionType);
        result.setActionStatus(actionStatus);
        result.setLikeCount(updatedPost.getLikeCount() == null ? 0 : updatedPost.getLikeCount());
        result.setFavoriteCount(updatedPost.getFavoriteCount() == null ? 0 : updatedPost.getFavoriteCount());
        result.setRepostCount(updatedPost.getRepostCount() == null ? 0 : updatedPost.getRepostCount());
        return result;
        //如果数据库当前状态已经符合请求目标，返回相同信息的主要作用是：明确告诉前端：请求成功，并且最终状态已经是你要求的状态。
    }

    /**
     * 根据互动类型构造三个计数增量，并交给数据库执行一次原子 UPDATE。
     */
    private void changeCounter(Long postId, HitActionType actionType, int delta) {
        int likeDelta = actionType == HitActionType.LIKE ? delta : 0;
        int favoriteDelta = actionType == HitActionType.FAVORITE ? delta : 0;
        int repostDelta = actionType == HitActionType.REPOST ? delta : 0;

        // 执行原子计数 SQL，避免“先查询后更新”造成并发覆盖。
        int result = hitPostMapper.changeActionCounters(postId, likeDelta, favoriteDelta, repostDelta);
        if(result != 1) { //没更新成功，要报错回滚
            throw new HomeworkException(ResultCodeEnum.HIT_NOT_EXIST);
        }
        //如果 Post 在校验后被隐藏或删除，评论/互动可能已经写入，但计数没有更新。
    }

    /**
     * 为新产生的点赞、收藏或转发创建相应通知。
     */
    private void createActionNotification(HitPost post, Long senderUserId, HitActionType actionType) {
        UserNotificationType notificationType =
                switch (actionType) {
                    case LIKE -> UserNotificationType.LIKE;
                    case FAVORITE -> UserNotificationType.FAVORITE;
                    case REPOST -> UserNotificationType.REPOST;
                };
        String title = switch (actionType) {
            case LIKE -> "收到的赞";
            case FAVORITE -> "动态被收藏";
            case REPOST -> "动态被转发";
        };
        createNotification(
                post.getPostUserId(),
                senderUserId,
                notificationType,
                UserNotificationSendTo.HIT_POST,
                post.getId(),
                title,
                post.getContent());
    }

    /**
     * 用户取消互动时，逻辑删除这次互动此前生成的通知。
     */
    private void deleteActionNotification(HitPost post, Long senderUserId, HitActionType actionType) {
        UserNotificationType notificationType = switch (actionType) {
            case LIKE -> UserNotificationType.LIKE;
            case FAVORITE -> UserNotificationType.FAVORITE;
            case REPOST -> UserNotificationType.REPOST;
        };

        notificationService.remove(post.getPostUserId(), senderUserId, notificationType,
                UserNotificationSendTo.HIT_POST, post.getId());
    }

    /**
     * 创建统一格式的用户通知，并过滤自己通知自己的情况。
     */
    private void createNotification(
            Long noticedUserId,
            Long senderUserId,
            UserNotificationType notificationType,
            UserNotificationSendTo sendTo,
            Long itemId,
            String title,
            String content) {
        Long postId = sendTo == UserNotificationSendTo.HIT_POST ? itemId
                : Optional.ofNullable(hitCommentMapper.selectIncludingDeleted(itemId))
                .map(HitComment::getPostId).orElse(null);
        notificationService.create(noticedUserId, senderUserId, notificationType,
                sendTo, itemId, postId, title, content);
    }

    private void createMentionNotifications(List<Long> mentionedUserIds, Long actionUserId,
                                            UserNotificationSendTo sendTo, Long itemId,
                                            Long postId, String content, Set<Long> excluded) {
        if (mentionedUserIds == null) return;
        Set<Long> receiverIds = mentionedUserIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> !Objects.equals(id, actionUserId))
                .filter(id -> !excluded.contains(id))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (receiverIds.size() > 10) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        if (receiverIds.isEmpty()) return;
        Set<Long> activeUserIds = userInfoMapper.selectByIds(receiverIds).stream()
                .filter(user -> user.getStatus() == UserInfoStatus.ACTIVE)
                .filter(user -> user.getUserRole() == UserInfoUserRole.USER)
                .map(UserInfo::getId)
                .collect(Collectors.toSet());
        if (activeUserIds.size() != receiverIds.size()) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        receiverIds.stream()
                .forEach(id -> notificationService.create(id, actionUserId, UserNotificationType.MENTION,
                        sendTo, itemId, postId, "@了你", content));
    }

    /**
     * 从完整正文中提取所有以 # 开头的标签。
     * 不会修改原始正文。
     */
    private String extractAndSerializeTags(String content) {

        LinkedHashSet<String> tagSet = new LinkedHashSet<>(); //标签为了去重，用set集合

        Matcher matcher = HASHTAG_PATTERN.matcher(content);

        while (matcher.find()) {
            String tag = matcher.group(1); //group(1) 取得不包含 # 的标签文本。

            String normalizedTag = tag.trim();
            int tagLength = normalizedTag.codePointCount(0, normalizedTag.length());
            if (tagLength > MAX_TAG_LENGTH) {
                throw new HomeworkException(ResultCodeEnum.HIT_TAG_TOO_LONG_ERROR);
            }

            tagSet.add(normalizedTag);

            if (tagSet.size() > MAX_TAG_COUNT) { //同一条 Post 去重后的有效标签不能超过 10 个。
                throw new HomeworkException(ResultCodeEnum.HIT_TAG_COUNT_ERROR);
            }
        }
        if (tagSet.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(tagSet); // 把有序标签 Set 转换为 JSON 数组字符串。
        } catch (JsonProcessingException e) {
            throw new HomeworkException(ResultCodeEnum.HIT_TAG_FORMAT_ERROR, e);
        }
    }


    /**
     * 把数据库中的标签 JSON 解析成前端使用的字符串列表。
     */
    private List<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return List.of();
        }
        try { // JSON 反序列化可能遇到历史脏数据，因此进入异常保护块。
            return objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {
            });// 明确告诉 Jackson 目标类型是 List<String>，避免泛型擦除。
        } catch (JsonProcessingException e) {
            return List.of(); // 单条脏标签不应拖垮整个时间线，因此降级为空标签列表。
        }
    }

    /**
     * 清理必填文本，并按 Unicode 字符数校验长度。
     */
    private String normalizeContent(String content, int maxLength) {

        if (content == null || content.isEmpty()) { //null要写在empty前面,避免content空指针异常
            throw new HomeworkException(ResultCodeEnum.HIT_CONTENT_EMPTY_ERROR);
        }
        String normalizedContent = content.trim();
        int contentLength = normalizedContent.codePointCount(0, normalizedContent.length());
        if (contentLength > maxLength) {
            throw new HomeworkException(ResultCodeEnum.HIT_CONTENT_TOO_LONG_ERROR);
        }
        return normalizedContent;
    }

    private String normalizeComment(String comment, int maxLength) {
        if (comment == null) {
            throw new HomeworkException(ResultCodeEnum.HIT_CONTENT_EMPTY_ERROR);
        }
        String normalizedComment = comment.strip();
        if (normalizedComment.isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.HIT_CONTENT_EMPTY_ERROR);
        }

        int commentLength = normalizedComment.codePointCount(0, normalizedComment.length());
        if (commentLength > maxLength) {
            throw new HomeworkException(ResultCodeEnum.HIT_COMMENT_TOO_LONG_ERROR);
        }
        return normalizedComment;
    }


    /**
     * 把空页码或非法小页码修正为第 1 页。
     */
    private long normalizePage(Integer pageNum) { // 接收客户端可选页码。
        return pageNum == null ? 1L : Math.max(1, pageNum); // null 返回 1，否则保证结果不会小于 1。
    } // 结束页码规范化方法。

    /**
     * 把每页数量限制在 1～100，默认每页 20 条。
     */
    private long normalizePageSize(Integer pageSize) { // 接收客户端可选的每页条数。
        return pageSize == null // 判断客户端是否省略 pageSize。
                ? 20L // 省略时使用默认值 20。
                : Math.max(1, Math.min(pageSize, 100)); // 否则先限制最大 100，再保证最小 1。
    }
}
