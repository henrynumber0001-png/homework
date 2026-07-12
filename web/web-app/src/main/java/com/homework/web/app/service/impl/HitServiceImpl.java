package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.model.entity.*;
import com.homework.model.enums.*;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.dto.HitActionDTO;
import com.homework.web.app.dto.HitCommentCreateDTO;
import com.homework.web.app.dto.HitPostCreateDTO;
import com.homework.web.app.mapper.*;
import com.homework.web.app.service.HitService;
import com.homework.web.app.vo.HitCommentVO;
import com.homework.web.app.vo.HitPostVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HitServiceImpl implements HitService {

    private static final int MAX_POST_LENGTH = 140;
    private static final int MAX_COMMENT_LENGTH = 500;
    private static final int MAX_TAG_COUNT = 10;
    private static final int MAX_TAG_LENGTH = 30;

    private final HitPostMapper hitPostMapper;
    private final HitCommentMapper hitCommentMapper;
    private final HitActionMapper hitActionMapper;
    private final UserInfoMapper userInfoMapper;
    private final UserNotificationMapper userNotificationMapper;
    private final ObjectMapper objectMapper;

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
        Set<Long> postUserIds = posts.stream().map(HitPost::getUserId).collect(Collectors.toSet());
        List<Long> postIds = posts.stream().map(HitPost::getId).toList();

        // 一次批量查询全部user，并整理成 userId -> UserInfo 的 Map。
        List<UserInfo> userInfos = userInfoMapper.selectByIds(postUserIds);
        Map<Long, UserInfo> postUserMap = userInfos.stream().collect(Collectors.toMap(UserInfo::getId, Function.identity()));


        //查询当前查看者对这一页每条动态做过哪些互动。
        //Map<postId, Set<HitActionType>>
        Map<Long, Set<HitActionType>> viewerActionMap = getViewerActions(viewerUserId, postIds);
        //允许 viewerActionMap 为空集合（注意：空集合 = Map.of(), 不等于 null）

        List<HitPostVO> hitPostVOs = posts.stream()
                .map(post -> toPostVO(post, postUserMap.get(post.getUserId()), viewerActionMap.getOrDefault(post.getId(), Set.of()))).toList();
        //根据 postId 获取当前用户对这条 Hit 的互动记录；如果没有记录，就返回一个空的 Set。
        //当 viewerActionMap 为空集合，viewerActionMap.getOrDefault(post.getId(), Set.of())) 就返回 Set.of() 空set集合

        return hitPostVOs;
    }

    /**
     * 查询某条 Hit 的评论。
     * 评论默认按从早到晚排列，便于前端按照自然对话顺序渲染回复。
     */
    @Override // 表示该方法实现 HitService.listComments。
    public List<HitCommentVO> listComments(Long postId, Integer pageNum, Integer pageSize) { // 接收动态 ID 和分页参数，返回评论展示列表。
        requirePublishedPost(postId); // 先确认动态存在且处于已发布状态，避免查询隐藏或已删除动态的评论。
        Page<HitComment> page = new Page<>( // 创建评论分页对象。
                normalizePage(pageNum), // 把空页码或小于 1 的页码修正为第 1 页。
                normalizePageSize(pageSize), // 把每页数量限制在 1～100 之间。
                false); // 不查询评论总数，减少一次 COUNT SQL。
        List<HitComment> comments = hitCommentMapper.selectPage( // 调用评论 Mapper 执行分页查询。
                        page, // 传入上面构造的分页设置。
                        new QueryWrapper<HitComment>() // 创建评论查询条件。
                                .eq("post_id", postId) // 只查询属于当前 Hit 的评论。
                                .orderByAsc("created_time") // 先按创建时间升序，让早期评论排在前面。
                                .orderByAsc("id")) // 同一时间创建时按 ID 升序，保证展示顺序稳定。
                .getRecords(); // 从分页结果中取出当前页的评论实体。
        Map<Long, UserInfo> users = loadUsers( // 批量加载本页评论作者，供后续组装昵称和头像。
                comments.stream() // 遍历评论列表。
                        .map(HitComment::getUserId) // 提取每条评论的作者 ID。
                        .collect(Collectors.toSet())); // 去重后形成用户 ID 集合。
        return comments.stream() // 遍历当前页所有评论。
                .map(comment -> toCommentVO( // 将每个评论实体转换成展示对象。
                        comment, // 传入评论本身。
                        users.get(comment.getUserId()))) // 根据评论者 ID 取得用户信息。
                .toList(); // 收集并返回评论展示列表。
    } // 结束评论列表查询方法。

    /**
     * 发布一条新的 Hit。
     * 发布者 ID 由 Controller 从 JWT 登录上下文传入，客户端不能伪造其他用户身份。
     */
    @Override // 表示该方法实现 HitService.publish。
    public Long publish(Long currentUserId, HitPostCreateDTO dto) { // 接收登录用户 ID 和发布内容，返回新动态主键。
        requireCurrentUser(currentUserId); // 检查登录上下文中是否存在有效 userId。
        String content = normalizeRequiredText( // 清理正文首尾空格，并统一校验非空和最大长度。
                dto == null ? null : dto.getContent(), // DTO 为空时传入 null，避免直接 dto.getContent() 导致空指针。
                MAX_POST_LENGTH, // 指定正文长度上限为 140 个 Unicode 字符。
                "Hit 内容不能为空", // 内容为空时抛出的参数错误提示。
                "Hit 内容最多 140 字"); // 内容超长时抛出的参数错误提示。

        HitPost post = new HitPost(); // 创建一个尚未写入数据库的 Hit 实体。
        post.setUserId(currentUserId); // 将当前登录用户设置为动态发布者。
        post.setContent(content); // 保存已经去除首尾空格并通过校验的正文。
        post.setTagsJson(toTagsJson(dto.getTags())); // 规范化标签并序列化成 JSON 字符串；没有标签时保存 null。
        post.setPostStatus(HitPostStatus.PUBLISHED); // 新发布的 Hit 默认立即进入公共时间线。
        post.setCommentCount(0); // 新动态还没有评论，因此评论数初始化为 0。
        post.setLikeCount(0); // 新动态还没有点赞，因此点赞数初始化为 0。
        post.setFavoriteCount(0); // 新动态还没有收藏，因此收藏数初始化为 0。
        post.setRepostCount(0); // 新动态还没有转发，因此转发数初始化为 0。
        hitPostMapper.insert(post); // 执行 INSERT；MyBatis-Plus 会把数据库生成的主键回填到 post.id。
        return post.getId(); // 返回新动态 ID，前端可据此跳转到动态详情或更新列表。
    } // 结束发布 Hit 方法。

    /**
     * 创建顶级评论或回复另一条评论。
     * 写评论、增加计数和创建通知必须处于同一事务中。
     */
    @Override // 表示该方法实现 HitService.comment。
    @Transactional // 任一步骤失败时回滚评论、计数和通知，防止三张表数据不一致。
    public Long comment(Long currentUserId, Long postId, HitCommentCreateDTO dto) { // 接收评论者、动态 ID 和评论内容，返回评论 ID。
        requireCurrentUser(currentUserId); // 确认请求来自已登录用户。
        HitPost post = requirePublishedPost(postId); // 查询并取得可评论的已发布动态，同时用于确定通知接收者。
        String content = normalizeRequiredText( // 统一清理并校验评论正文。
                dto == null ? null : dto.getContent(), // DTO 为空时安全地传入 null。
                MAX_COMMENT_LENGTH, // 评论上限为 500 个 Unicode 字符。
                "评论内容不能为空", // 空评论的错误提示。
                "评论内容最多 500 字"); // 超长评论的错误提示。

        HitComment parent = null; // 默认没有父评论，表示这是一条顶级评论。
        if (dto.getParentId() != null) { // parentId 非空表示用户正在回复另一条评论。
            parent = hitCommentMapper.selectById(dto.getParentId()); // 查询被回复的父评论。
            if (parent == null || !Objects.equals(parent.getPostId(), postId)) { // 检查父评论存在且确实属于当前 Hit。
                throw new IllegalArgumentException("被回复的评论不存在或不属于当前 Hit"); // 阻止跨动态伪造回复关系。
            } // 结束父评论合法性判断。
        } // 结束父评论查询逻辑。

        HitComment comment = new HitComment(); // 创建新的评论实体。
        comment.setPostId(postId); // 记录评论属于哪一条 Hit。
        comment.setUserId(currentUserId); // 记录评论作者为当前登录用户。
        comment.setParentId(dto.getParentId()); // 保存父评论 ID；顶级评论时该值为 null。
        comment.setContent(content); // 保存已清理并校验通过的评论正文。
        hitCommentMapper.insert(comment); // 将评论写入数据库，并回填 comment.id。
        hitPostMapper.changeCommentCount(postId, 1); // 使用原子 SQL 把动态评论数加 1，避免并发丢失计数。

        // 回复父评论时通知父评论作者；顶级评论则通知动态作者。
        Long receiverUserId = parent == null // 根据是否存在父评论选择通知接收者。
                ? post.getUserId() // 顶级评论通知动态发布者。
                : parent.getUserId(); // 回复评论通知被回复的评论作者。
        createNotification( // 创建一条“回复我的”通知。
                receiverUserId, // 通知的接收用户。
                currentUserId, // 触发回复的当前用户。
                UserNotificationNotificationType.REPLY, // 指定通知类别为回复。
                UserNotificationTargetType.HIT_COMMENT, // 点击通知后应跳转到 Hit 评论。
                comment.getId(), // 目标 ID 使用刚创建的评论 ID。
                "回复我的", // 通知标题。
                content); // 通知摘要使用评论正文。
        return comment.getId(); // 返回新评论 ID。
    } // 结束创建评论方法。

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
        } // 结束状态变化后的同步处理。

        HitPost updatedPost = requirePublishedPost(postId); // 重新查询动态，取得原子 SQL 更新后的最新计数。
        Map<String, Object> result = new LinkedHashMap<>(); // 使用有序 Map，让返回字段顺序稳定、便于调试。
        result.put("active", nextActive); // 告诉前端按钮最终是否处于选中状态。
        result.put("likeCount", defaultZero(updatedPost.getLikeCount())); // 返回最新点赞数，并把历史 null 值转换为 0。
        result.put("favoriteCount", defaultZero(updatedPost.getFavoriteCount())); // 返回最新收藏数。
        result.put("repostCount", defaultZero(updatedPost.getRepostCount())); // 返回最新转发数。
        return result; // 返回互动后的最终状态和三种计数。
    } // 结束互动处理方法。

    /** 根据互动类型构造三个计数增量，并交给数据库执行一次原子 UPDATE。 */
    private void changeCounter(Long postId, HitActionType actionType, int delta) { // 接收动态、互动类型和 +1/-1 增量。
        int likeDelta = actionType == HitActionType.LIKE ? delta : 0; // 点赞操作只改变 like_count，否则增量为 0。
        int favoriteDelta = actionType == HitActionType.FAVORITE ? delta : 0; // 收藏操作只改变 favorite_count。
        int repostDelta = actionType == HitActionType.REPOST ? delta : 0; // 转发操作只改变 repost_count。
        hitPostMapper.changeActionCounters( // 执行原子计数 SQL，避免“先查询后更新”造成并发覆盖。
                postId, // 指定要更新的动态。
                likeDelta, // 传入点赞计数变化量。
                favoriteDelta, // 传入收藏计数变化量。
                repostDelta); // 传入转发计数变化量。
    } // 结束互动计数更新方法。

    /** 为新产生的点赞、收藏或转发创建相应通知。 */
    private void createActionNotification(HitPost post, Long senderUserId, HitActionType actionType) { // 接收动态、互动者和互动类型。
        UserNotificationNotificationType notificationType = switch (actionType) { // 把 Hit 互动类型映射为通知类型。
            case LIKE -> UserNotificationNotificationType.LIKE; // 点赞对应 LIKE 通知。
            case FAVORITE -> UserNotificationNotificationType.FAVORITE; // 收藏对应 FAVORITE 通知。
            case REPOST -> UserNotificationNotificationType.REPOST; // 转发对应 REPOST 通知。
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
    } // 结束创建互动通知方法。

    /** 用户取消互动时，逻辑删除这次互动此前生成的通知。 */
    private void deleteActionNotification(HitPost post, Long senderUserId, HitActionType actionType) { // 接收动态、互动者和被取消的类型。
        UserNotificationNotificationType notificationType = switch (actionType) { // 把互动类型映射为需要删除的通知类型。
            case LIKE -> UserNotificationNotificationType.LIKE; // 取消点赞时删除 LIKE 通知。
            case FAVORITE -> UserNotificationNotificationType.FAVORITE; // 取消收藏时删除 FAVORITE 通知。
            case REPOST -> UserNotificationNotificationType.REPOST; // 取消转发时删除 REPOST 通知。
        }; // 完成通知类型映射。
        userNotificationMapper.delete( // MyBatis-Plus 会按照实体的逻辑删除配置更新 is_deleted，而非物理删除。
                new QueryWrapper<UserNotification>() // 创建精确定位原互动通知的查询条件。
                        .eq("receiver_user_id", post.getUserId()) // 限定接收者为动态作者。
                        .eq("sender_user_id", senderUserId) // 限定发送者为当前互动用户。
                        .eq("notification_type", notificationType.getValue()) // 限定为被取消的互动通知类型。
                        .eq("target_type", UserNotificationTargetType.HIT_POST.getValue()) // 限定通知目标类型为 Hit 动态。
                        .eq("target_id", post.getId())); // 限定通知目标为当前动态，避免误删其他动态通知。
    } // 结束撤销互动通知方法。

    /** 创建统一格式的用户通知，并过滤自己通知自己的情况。 */
    private void createNotification( // 定义统一通知创建方法。
            Long receiverUserId, // 通知接收者用户 ID。
            Long senderUserId, // 触发通知的用户 ID。
            UserNotificationNotificationType notificationType, // 通知业务类型。
            UserNotificationTargetType targetType, // 点击通知后的目标资源类型。
            Long targetId, // 点击通知后的目标资源 ID。
            String title, // 通知标题。
            String content) { // 通知摘要正文。
        if (receiverUserId == null || Objects.equals(receiverUserId, senderUserId)) { // 接收者不存在或自己操作自己的内容时无需通知。
            return; // 直接结束方法，避免产生无意义的自通知。
        } // 结束无需通知的判断。
        UserNotification notification = new UserNotification(); // 创建新的通知实体。
        notification.setReceiverUserId(receiverUserId); // 设置通知接收者。
        notification.setSenderUserId(senderUserId); // 设置触发通知的用户。
        notification.setNotificationType(notificationType); // 设置回复、点赞、收藏或转发等通知类型。
        notification.setTargetType(targetType); // 设置跳转资源类型。
        notification.setTargetId(targetId); // 设置跳转资源 ID。
        notification.setTitle(title); // 设置前端展示的通知标题。
        notification.setContent(content); // 设置前端展示的内容摘要。
        notification.setReadStatus(UserNotificationReadStatus.UNREAD); // 新通知初始状态统一为未读。
        userNotificationMapper.insert(notification); // 把通知写入 user_notification 表。
    } // 结束统一通知创建方法。

    /** 查询并返回一条可公开访问的 Hit；不满足条件时统一抛出参数异常。 */
    private HitPost requirePublishedPost(Long postId) { // 接收待检查的动态 ID，并返回已验证实体。
        if (postId == null) { // 没有动态 ID 就无法定位目标动态。
            throw new IllegalArgumentException("Hit ID 不能为空"); // 返回清晰的参数错误提示。
        } // 结束 ID 空值判断。
        HitPost post = hitPostMapper.selectById(postId); // 根据主键查询 Hit；逻辑删除记录会被 MyBatis-Plus 自动过滤。
        if (post == null || post.getPostStatus() != HitPostStatus.PUBLISHED) { // 检查动态存在且业务状态为已发布。
            throw new IllegalArgumentException("Hit 不存在或不可访问"); // 隐藏、删除或不存在的动态都不允许继续操作。
        } // 结束动态可访问性判断。
        return post; // 返回已经验证过的动态，调用方无需重复查询。
    } // 结束动态校验方法。

    /** 检查 Controller 是否从 JWT 登录上下文取得了当前用户 ID。 */
    private void requireCurrentUser(Long currentUserId) { // 接收当前登录用户 ID。
        if (currentUserId == null) { // null 表示没有可用的登录用户上下文。
            throw new IllegalArgumentException("当前登录用户不存在"); // 阻止匿名用户发布、评论或互动。
        } // 结束当前用户判断。
    } // 结束登录用户校验方法。

//    /** 批量加载用户，并转换成 userId -> UserInfo 的 Map。 */
//    private Map<Long, UserInfo> loadUsers(Set<Long> userIds) {
//        if (userIds.isEmpty()) {
//            return Map.of();
//        }
//        return userInfoMapper.selectByIds(userIds).stream().collect(Collectors.toMap(UserInfo::getId, Function.identity()));
//    }

    /** 批量查询当前查看者对本页 Hit 做过的所有有效互动。 */
    private Map<Long, Set<HitActionType>> getViewerActions(Long viewerUserId, List<Long> postIds) {
        if (viewerUserId == null || postIds.isEmpty()) {
            return Map.of(); // 返回空 Map，所有互动按钮默认未选中。
        }

        LambdaQueryWrapper<HitAction> hitActionQueryWrapper = new LambdaQueryWrapper<>();
        hitActionQueryWrapper.in(HitAction::getPostId, postIds)
                .eq(HitAction::getUserId, viewerUserId);

        //当前用户在这些postIds里面，有哪些hitActions
        //没查到任何行数据，允许返回空列表
        List<HitAction> hitActions = hitActionMapper.selectList(hitActionQueryWrapper);

        //如果hitActions.isEmpty(),那么hitActionMap也是空map集合，因为没有查到任何行数据，就不可能有匹配viewerUserId的postId
        //因为viewer没有 like,favorite,repost动作，就不会有 hit_action的行数据，就不会有postId
        Map<Long, Set<HitActionType>> hitActionMap = hitActions.stream()
                .collect(Collectors.groupingBy(HitAction::getPostId, Collectors.mapping(HitAction::getActionType, Collectors.toSet())));
        /*
        使用了两个 Collectors 收集器（只希望保存每条操作的 actionType，所以使用了第二个收集器 mapping()）
        groupingBy()：按照 postId 分组
        mapping()：把 HitAction 转换成 HitActionType：根据每个查询出来的 hitAction，提取这个 viewerId 下的每个 postId 的 HitAction 对象中的 actionType，然后按照 postId为一组，放入 Set 集合。
        toSet()：把操作类型收集为 Set
         */

        return hitActionMap;
    }


    //组装HitPostVO
    private HitPostVO toPostVO(HitPost post, UserInfo user, Set<HitActionType> actions) { // 当前查看者对该动态的互动类型集合(like,favorite,repost)
        HitPostVO vo = new HitPostVO();
        vo.setId(post.getId());
        vo.setUserId(post.getUserId());
        vo.setDisplayName(user == null ? null : user.getDisplayName());
        vo.setAvatar(user == null ? null : user.getAvatar());
        vo.setTags(parseTags(post.getTagsJson()));
        vo.setCommentCount(defaultZero(post.getCommentCount()));
        vo.setLikeCount(defaultZero(post.getLikeCount()));
        vo.setFavoriteCount(defaultZero(post.getFavoriteCount()));
        vo.setRepostCount(defaultZero(post.getRepostCount()));
        vo.setLiked(actions.contains(HitActionType.LIKE)); // 针对这一条HitPost, 当前用户是否有 LIKE 记录，有则让点赞按钮显示选中。
        vo.setFavorited(actions.contains(HitActionType.FAVORITE));
        vo.setReposted(actions.contains(HitActionType.REPOST));
        vo.setCreatedTime(post.getCreatedTime()); // 设置动态发布时间，供前端显示“2 分钟前”等文本。
        return vo;
    }

    /** 把评论实体和作者信息组合成评论 VO。 */
    private HitCommentVO toCommentVO(HitComment comment, UserInfo user) { // 接收评论实体和可能为空的作者信息。
        HitCommentVO vo = new HitCommentVO(); // 创建空评论展示对象。
        vo.setId(comment.getId()); // 设置评论主键。
        vo.setPostId(comment.getPostId()); // 设置评论所属 Hit ID。
        vo.setUserId(comment.getUserId()); // 设置评论作者 ID。
        vo.setDisplayName(user == null ? null : user.getDisplayName()); // 设置作者昵称，并安全处理用户缺失情况。
        vo.setAvatar(user == null ? null : user.getAvatar()); // 设置作者头像，并安全处理用户缺失情况。
        vo.setParentId(comment.getParentId()); // 设置父评论 ID；null 表示顶级评论。
        vo.setContent(comment.getContent()); // 设置评论正文。
        vo.setCreatedTime(comment.getCreatedTime()); // 设置评论创建时间。
        return vo; // 返回组装完成的评论展示对象。
    } // 结束评论 VO 转换方法。

    /** 规范化标签并将其序列化为数据库保存的 JSON 字符串。 */
    private String toTagsJson(List<String> tags) { // 接收客户端提交的原始标签列表。
        if (tags == null || tags.isEmpty()) { // 没有提交标签或提交空列表时无需保存 JSON。
            return null; // 数据库 tags_json 保存 null，减少无意义的 "[]"。
        } // 结束原始标签空值判断。
        LinkedHashSet<String> normalized = new LinkedHashSet<>(); // 使用有序 Set 同时去重并保留用户输入顺序。
        for (String tag : tags) { // 逐个处理客户端提交的标签。
            if (tag == null || tag.isBlank()) { // 忽略 null、空字符串和纯空格标签。
                continue; // 跳过当前无效标签，继续处理下一个。
            } // 结束空标签判断。
            String value = tag.trim(); // 删除标签首尾多余空格。
            if (value.startsWith("#")) { // 允许前端提交“#React”或“React”两种形式。
                value = value.substring(1).trim(); // 去掉开头的 #，数据库统一只保存标签文本。
            } // 结束 # 前缀处理。
            if (value.isEmpty()) { // “#”去除后可能只剩空字符串。
                continue; // 忽略这种无实际内容的标签。
            } // 结束规范化后空值判断。
            if (codePointLength(value) > MAX_TAG_LENGTH) { // 按 Unicode 字符数检查单个标签长度。
                throw new IllegalArgumentException("单个标签最多 30 字"); // 超过 30 字时拒绝发布并返回提示。
            } // 结束标签长度判断。
            normalized.add(value); // 把有效标签加入有序 Set；重复标签不会重复保存。
            if (normalized.size() > MAX_TAG_COUNT) { // 对去重后的有效标签数量进行上限检查。
                throw new IllegalArgumentException("一条 Hit 最多添加 10 个标签"); // 超过 10 个有效标签时拒绝发布。
            } // 结束标签数量判断。
        } // 结束所有标签的循环处理。
        if (normalized.isEmpty()) { // 原列表可能只包含空值或“#”，规范化后没有有效标签。
            return null; // 没有有效标签时仍保存 null。
        } // 结束有效标签空值判断。
        try { // JSON 序列化可能失败，因此进入异常保护块。
            return objectMapper.writeValueAsString(normalized); // 把有序标签 Set 转换为 JSON 数组字符串。
        } catch (JsonProcessingException e) { // 捕获 Jackson 无法序列化标签的异常。
            throw new IllegalArgumentException("Hit 标签格式不正确", e); // 转换成统一参数异常并保留原始原因。
        } // 结束 JSON 序列化异常处理。
    } // 结束标签序列化方法。

    /** 把数据库中的标签 JSON 解析成前端使用的字符串列表。 */
    private List<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return List.of();
        }
        try { // JSON 反序列化可能遇到历史脏数据，因此进入异常保护块。
            return objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {});// 明确告诉 Jackson 目标类型是 List<String>，避免泛型擦除。
        } catch (JsonProcessingException e) {
            return List.of(); // 单条脏标签不应拖垮整个时间线，因此降级为空标签列表。
        }
    }

    /** 清理必填文本，并按 Unicode 字符数校验长度。 */
    private String normalizeRequiredText(String value, int maxLength, String emptyMessage, String tooLongMessage) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(emptyMessage);
        }
        if (codePointLength(normalized) > maxLength) {
            throw new IllegalArgumentException(tooLongMessage);
        }
        return normalized;
    }

    /** 使用 Unicode code point 计数，使 emoji 不会因为占两个 Java char 而被误算成两个字。 */
    private int codePointLength(String value) { // 接收一个确定非 null 的字符串。
        return value.codePointCount(0, value.length()); // 统计从索引 0 到字符串末尾的 Unicode code point 数量。
    } // 结束 Unicode 字符计数方法。

    /** 把空页码或非法小页码修正为第 1 页。 */
    private long normalizePage(Integer pageNum) { // 接收客户端可选页码。
        return pageNum == null ? 1L : Math.max(1, pageNum); // null 返回 1，否则保证结果不会小于 1。
    } // 结束页码规范化方法。

    /** 把每页数量限制在 1～100，默认每页 20 条。 */
    private long normalizePageSize(Integer pageSize) { // 接收客户端可选的每页条数。
        return pageSize == null // 判断客户端是否省略 pageSize。
                ? 20L // 省略时使用默认值 20。
                : Math.max(1, Math.min(pageSize, 100)); // 否则先限制最大 100，再保证最小 1。
    } // 结束每页数量规范化方法。

    /** 将历史数据中可能出现的 null 计数安全转换为 0。 */
    private int defaultZero(Integer value) { // 接收可能为空的数据库计数字段。
        return value == null ? 0 : value; // null 返回 0，否则保留原计数。
    }
}
