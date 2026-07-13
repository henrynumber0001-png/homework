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
import com.homework.web.app.mapper.*;
import com.homework.web.app.service.HitService;
import com.homework.web.app.vo.HitCommentVO;
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
    private final UserNotificationMapper userNotificationMapper;
    private final ObjectMapper objectMapper;

    /**
     * 匹配以 # 开头的标签。
     *
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
                .eq(HitAction::getUserId, viewerUserId);

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

        // 先确认动态存在且处于已发布状态，避免查询隐藏或已删除动态的评论。
        if(postId == null){
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
        if(comments.isEmpty()){
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
            //因为userInfo == null，主要影响的是avatar和displayName的设置，所以在这个地方做if判断即可
            if (userInfo == null) {
                vo.setAvatar(null);
            }else {
                vo.setAvatar(userInfo.getAvatar());
            }
            vo.setDisplayName(userInfo == null ? "该用户已注销" : userInfo.getDisplayName());
            hitCommentVOs.add(vo);
        });
        return hitCommentVOs;

    }


    //发布HIT
    @Override
    public Long publish(String content) {

        Long postUserId = LoginUserHolder.getUserId();

        String normalizeContent = normalizeContent(content, MAX_POST_LENGTH);

        //从正文中提取tag 并转换成JSON格式的字符串，例如：["React","Hooks"]
        String tagJson = extractAndParseTags(normalizeContent);

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
        return post.getId();
    }


    //评论HIT
    @Override
    @Transactional // 任一步骤失败时回滚评论、计数和通知，防止三张表数据不一致。
    public Long comment(Long postId, HitCommentCreateDTO dto) {
        if(postId == null || dto == null){
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
        comment.setComment(dto.getComment());
        hitCommentMapper.insert(comment);
        hitPostMapper.changeCommentCount(postId, 1); // 使用原子 SQL 把动态评论数加 1，避免并发丢失计数。

        // 收到评论通知的userId
        // 如果是针对父评论，那么id就是父评论那一行的 commentUserId, 也就是父评论那一行评论的作者;
        // 如果是针对post，那么id 是这条 post 的 postUserId
        Long noticedUserId = parentComment == null ? hitPost.getPostUserId() : parentComment.getCommentUserId();

        // 创建一条“回复我的”通知。
        createNotification(noticedUserId, commentUserId, UserNotificationType.REPLY,
                UserNotificationTargetType.HIT_COMMENT, // 点击通知后应跳转到 Hit 评论。
                comment.getId(),
                "回复我的", // 通知标题。
                normalizeContent); // 通知摘要使用评论正文。
        return comment.getId(); // 返回新评论 ID。
    }

    /**
     * 点赞、收藏、转发共用一个幂等接口。
     * active=true 强制开启，active=false 强制取消，不传 active 时切换当前状态。
     * 只有互动状态真正改变时，才更新计数和通知。
     */
    @Override // 表示该方法实现 HitService.action。
    @Transactional // 互动记录、计数和通知必须同时成功或同时回滚。
    public Map<String, Object> action(Long currentUserId, Long postId, HitActionDTO dto) { // 处理当前用户对指定 Hit 的一种互动。
        requireCurrentUser(currentUserId); // 确认互动操作者已经登录。
        HitPost post = requirePublishedPost(postId); // 确认目标动态存在且可互动，并保留动态作者等信息。
        if (dto == null) { // 请求体完全缺失时无法确定互动类型。
            throw new IllegalArgumentException("互动参数不能为空"); // 返回明确的参数错误。
        } // 结束 DTO 空值判断。
        HitActionType actionType = EnumUtils.fromValue( // 把客户端整数转换成受类型约束的互动枚举。
                HitActionType.class, // 指定要转换成 HitActionActionType 枚举。
                dto.getActionType()); // 读取 1=点赞、2=收藏、3=转发；非法值会抛出异常。
        HitAction existing = hitActionMapper.selectIncludingDeletedForUpdate( // 查询包括逻辑删除记录在内的历史互动，并对该行加锁。
                postId, // 用动态 ID 限定互动记录。
                currentUserId, // 用当前用户 ID 限定互动记录。
                actionType.getValue()); // 用互动类型限定记录，三者共同对应数据库唯一键。
        boolean currentlyActive = existing != null // 首先要求数据库中存在这条互动历史。
                && !Boolean.TRUE.equals(existing.getDeleted()); // 并且 is_deleted 不是 true，才表示当前互动有效。
        boolean nextActive = dto.getActive() == null // 判断客户端是否显式指定目标状态。
                ? !currentlyActive // 未指定时采用按钮切换语义，即对当前状态取反。
                : dto.getActive(); // 已指定时使用客户端目标状态，使重复请求保持幂等。

        boolean changed = false; // 默认互动状态没有变化，后续只有实际写库成功才改为 true。
        if (nextActive && !currentlyActive) { // 目标是开启且当前未开启，需要新增或恢复互动。
            if (existing == null) { // 从未产生过该互动，数据库中连逻辑删除记录也不存在。
                HitAction action = new HitAction(); // 创建新的互动实体。
                action.setPostId(postId); // 设置互动所属动态。
                action.setUserId(currentUserId); // 设置互动用户为当前登录用户。
                action.setActionType(actionType); // 设置互动类型为点赞、收藏或转发。
                hitActionMapper.insert(action); // 插入互动记录；数据库唯一键同时防止重复互动。
                changed = true; // 插入成功说明状态已经从关闭变成开启。
            } else { // 存在历史记录但它已被逻辑删除。
                changed = hitActionMapper.restoreById(existing.getId()) == 1; // 把 is_deleted 恢复为 0，并根据受影响行数判断是否成功。
            } // 结束新增或恢复互动的分支。
        } else if (!nextActive && currentlyActive) { // 目标是取消且当前有效，需要逻辑删除互动。
            changed = hitActionMapper.deactivateById(existing.getId()) == 1; // 只删除当前有效记录，重复取消不会重复减计数。
        } // 其他情况表示目标状态与当前状态相同，不执行数据库写入。

        if (changed) { // 只有互动状态真实变化时才同步计数与通知。
            int delta = nextActive ? 1 : -1; // 开启互动时计数加 1，取消互动时计数减 1。
            changeCounter(postId, actionType, delta); // 原子更新对应的点赞、收藏或转发计数。
            if (nextActive) { // 如果这次变化是开启互动。
                createActionNotification(post, currentUserId, actionType); // 通知动态作者收到了新的互动。
            } else { // 如果这次变化是取消互动。
                deleteActionNotification(post, currentUserId, actionType); // 撤销该互动对应的通知。
            } // 结束创建或撤销通知的分支。
        }

        HitPost updatedPost = requirePublishedPost(postId); // 重新查询动态，取得原子 SQL 更新后的最新计数。
        Map<String, Object> result = new LinkedHashMap<>(); // 使用有序 Map，让返回字段顺序稳定、便于调试。
        result.put("active", nextActive); // 告诉前端按钮最终是否处于选中状态。
        result.put("likeCount", defaultZero(updatedPost.getLikeCount())); // 返回最新点赞数，并把历史 null 值转换为 0。
        result.put("favoriteCount", defaultZero(updatedPost.getFavoriteCount())); // 返回最新收藏数。
        result.put("repostCount", defaultZero(updatedPost.getRepostCount())); // 返回最新转发数。
        return result; // 返回互动后的最终状态和三种计数。
    }

    /**
     * 根据互动类型构造三个计数增量，并交给数据库执行一次原子 UPDATE。
     */
    private void changeCounter(Long postId, HitActionType actionType, int delta) { // 接收动态、互动类型和 +1/-1 增量。
        int likeDelta = actionType == HitActionType.LIKE ? delta : 0; // 点赞操作只改变 like_count，否则增量为 0。
        int favoriteDelta = actionType == HitActionType.FAVORITE ? delta : 0; // 收藏操作只改变 favorite_count。
        int repostDelta = actionType == HitActionType.REPOST ? delta : 0; // 转发操作只改变 repost_count。
        hitPostMapper.changeActionCounters( // 执行原子计数 SQL，避免“先查询后更新”造成并发覆盖。
                postId, // 指定要更新的动态。
                likeDelta, // 传入点赞计数变化量。
                favoriteDelta, // 传入收藏计数变化量。
                repostDelta); // 传入转发计数变化量。
    }

    /**
     * 为新产生的点赞、收藏或转发创建相应通知。
     */
    private void createActionNotification(HitPost post, Long senderUserId, HitActionType actionType) { // 接收动态、互动者和互动类型。
        UserNotificationType notificationType = switch (actionType) { // 把 Hit 互动类型映射为通知类型。
            case LIKE -> UserNotificationType.LIKE; // 点赞对应 LIKE 通知。
            case FAVORITE -> UserNotificationType.FAVORITE; // 收藏对应 FAVORITE 通知。
            case REPOST -> UserNotificationType.REPOST; // 转发对应 REPOST 通知。
        }; // 完成通知类型映射。
        String title = switch (actionType) { // 根据互动类型生成用户可读的通知标题。
            case LIKE -> "收到的赞"; // 点赞通知标题。
            case FAVORITE -> "动态被收藏"; // 收藏通知标题。
            case REPOST -> "动态被转发"; // 转发通知标题。
        }; // 完成通知标题映射。
        createNotification( // 调用统一通知创建方法，避免重复填写通知实体。
                post.getUserId(), // 接收者是动态发布者。
                senderUserId, // 发送者是执行互动的用户。
                notificationType, // 使用上面映射出的通知类型。
                UserNotificationTargetType.HIT_POST, // 点击通知后跳转到 Hit 动态。
                post.getId(), // 跳转目标是当前动态 ID。
                title, // 使用对应的中文标题。
                post.getContent()); // 使用动态正文作为通知摘要。
    }

    /**
     * 用户取消互动时，逻辑删除这次互动此前生成的通知。
     */
    private void deleteActionNotification(HitPost post, Long senderUserId, HitActionType actionType) { // 接收动态、互动者和被取消的类型。
        UserNotificationType notificationType = switch (actionType) { // 把互动类型映射为需要删除的通知类型。
            case LIKE -> UserNotificationType.LIKE; // 取消点赞时删除 LIKE 通知。
            case FAVORITE -> UserNotificationType.FAVORITE; // 取消收藏时删除 FAVORITE 通知。
            case REPOST -> UserNotificationType.REPOST; // 取消转发时删除 REPOST 通知。
        }; // 完成通知类型映射。
        userNotificationMapper.delete( // MyBatis-Plus 会按照实体的逻辑删除配置更新 is_deleted，而非物理删除。
                new QueryWrapper<UserNotification>() // 创建精确定位原互动通知的查询条件。
                        .eq("receiver_user_id", post.getUserId()) // 限定接收者为动态作者。
                        .eq("sender_user_id", senderUserId) // 限定发送者为当前互动用户。
                        .eq("notification_type", notificationType.getValue()) // 限定为被取消的互动通知类型。
                        .eq("target_type", UserNotificationTargetType.HIT_POST.getValue()) // 限定通知目标类型为 Hit 动态。
                        .eq("target_id", post.getId())); // 限定通知目标为当前动态，避免误删其他动态通知。
    }

    /**
     * 创建统一格式的用户通知，并过滤自己通知自己的情况。
     */
    private void createNotification(
                                     Long noticedUserId, // 通知接收者用户 ID。
                                     Long senderUserId, // 触发通知的用户 ID。
                                     UserNotificationType notificationType, // 通知业务类型。
                                     UserNotificationTargetType targetType, // 点击通知后的跳转目标类型。
                                     Long targetId, // 点击通知后的跳转目标 ID。
                                     String title, // 通知标题。
                                     String content) { // 通知摘要正文。
        if (noticedUserId == null || Objects.equals(noticedUserId, senderUserId)) { // 接收者不存在或自己操作自己的内容时无需通知。
            return; // 直接结束方法，避免产生无意义的自通知。
        }

        UserNotification notification = new UserNotification();
        notification.setReceiverUserId(noticedUserId);
        notification.setSenderUserId(senderUserId);
        notification.setNotificationType(notificationType);
        notification.setTargetType(targetType); // 设置跳转资源类型。
        notification.setTargetId(targetId); // 设置跳转资源 ID。
        notification.setTitle(title); // 设置前端展示的通知标题。
        notification.setContent(content);
        notification.setReadStatus(UserNotificationReadStatus.UNREAD);
        userNotificationMapper.insert(notification);
    }

    /**
     * 从完整正文中提取所有以 # 开头的标签。
     * 不会修改原始正文。
     */
    private String extractAndParseTags(String content) {

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

            if (tagSet.size() > MAX_TAG_COUNT) {
                throw new HomeworkException(ResultCodeEnum.HIT_TAG_COUNT_ERROR);
            }
        }
        if (tagSet.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(tagSet); // 把有序标签 Set 转换为 JSON 数组字符串。
        } catch (JsonProcessingException e) {
            throw new HomeworkException(ResultCodeEnum.HIT_TAG_FORMAT_ERROR);
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

    private String normalizeComment(String comment,int maxLength){
        if(comment == null || comment.isEmpty()){
            throw new HomeworkException(ResultCodeEnum.HIT_CONTENT_EMPTY_ERROR);
        }
        String normalizedComment = comment.trim();
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

    /**
     * 将历史数据中可能出现的 null 计数安全转换为 0。
     */
    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }
}
