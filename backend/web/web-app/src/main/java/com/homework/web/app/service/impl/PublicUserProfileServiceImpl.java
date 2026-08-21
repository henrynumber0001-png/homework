package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.Result;
import com.homework.common.result.ResultCodeEnum;
import com.homework.common.storage.UserImageUrlResolver;
import com.homework.model.entity.*;
import com.homework.model.enums.*;
import com.homework.web.app.dto.BlockActionDTO;
import com.homework.web.app.mapper.*;
import com.homework.web.app.service.MembershipAccessService;
import com.homework.web.app.service.MembershipAccessSnapshot;
import com.homework.web.app.service.PublicUserProfileService;
import com.homework.web.app.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicUserProfileServiceImpl implements PublicUserProfileService {
    private final UserInfoMapper userInfoMapper;
    private final UserFollowMapper followMapper;
    private final PrivateChatboxMapper chatboxMapper;
    private final HitPostMapper postMapper;
    private final HitActionMapper actionMapper;
    private final ObjectMapper objectMapper;
    private final UserImageUrlResolver userImageUrlResolver;
    private final MembershipAccessService membershipAccessService;
    private final UserQuestionAnswerMapper userQuestionAnswerMapper;
    private final UserLearningStatDailyMapper userLearningStatDailyMapper;
    private final UserBlockMapper userBlockMapper;


    @Override
    public PublicUserProfileVO getProfile(Long currentUserId, Long profileUserId) {

        // 先校验 profileUserId 是否存在合法状态账户
        UserInfo profileUser = userInfoMapper.selectById(profileUserId);

        if (profileUser == null || profileUser.getStatus() != UserInfoStatus.ACTIVE) {
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_USER_NOT_EXIST);
        }

        if(currentUserId == null){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_NOT_AUTH);
        }

        boolean self = Objects.equals(currentUserId, profileUserId);

        // 查看自己时没有关注关系，跳过查询既符合页面语义，也能减少不必要的数据库访问。
        boolean following = false;
        boolean mutualFollow = false;
        if (!self) {
            following = followMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                    .eq(UserFollow::getFollowerUserId, currentUserId)
                    .eq(UserFollow::getFolloweeUserId, profileUserId)) > 0;

            // 更进一步：如果双方角色交换，依然有记录，那就是 mutual 了
            if (following) {
                mutualFollow = followMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerUserId, profileUserId)
                        .eq(UserFollow::getFolloweeUserId, currentUserId)) > 0;
            }
        }

        boolean blocked = false;
        boolean blockedByCurrentUser = false;
        if (!self) {
            LambdaQueryWrapper<UserBlock> blockQuery = new LambdaQueryWrapper<>();
            blockQuery.and(
                    query -> query
                    .eq(UserBlock::getBlockerUserId, currentUserId)
                    .eq(UserBlock::getBlockedUserId, profileUserId)
                    .or(
                            reverse -> reverse
                            .eq(UserBlock::getBlockerUserId, profileUserId)
                            .eq(UserBlock::getBlockedUserId, currentUserId)
                    )
            );
            blocked = userBlockMapper.selectCount(blockQuery) > 0;

            //从业务逻辑上看，所有的拉黑相关操作，都应该在 !self 情况下进行，也就是不允许也不提供自己对自己拉黑
            //这不是有没有影响的问题，而是业务逻辑设计的问题
            LambdaQueryWrapper<UserBlock> userBlockQuery = new LambdaQueryWrapper<>();
            userBlockQuery.eq(UserBlock::getBlockerUserId, currentUserId)
                    .eq(UserBlock::getBlockedUserId, profileUserId);

            blockedByCurrentUser = userBlockMapper.selectCount(userBlockQuery) > 0;
        }

        //用户展示名称 和 AccountNo 不能为空或null
        if (!StringUtils.hasText(profileUser.getDisplayName()) || !StringUtils.hasText(profileUser.getAccountNo())) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }
        // 拉黑关系存在时只组装允许公开的头像、封面和展示名称。
        PublicUserInfoVO userInfoVO = new PublicUserInfoVO();
        userInfoVO.setAvatarUrl(userImageUrlResolver.resolveAvatar(profileUser.getAvatarObjectKey()));
        userInfoVO.setBannerUrl(userImageUrlResolver.resolveBanner(profileUser.getBannerObjectKey()));
        userInfoVO.setDisplayName(profileUser.getDisplayName());
        if (!blocked) {
            userInfoVO.setAccountNo(profileUser.getAccountNo());
            userInfoVO.setGender(profileUser.getGender());
            userInfoVO.setIntroduction(profileUser.getIntroduction());
            userInfoVO.setCompanyOrSchool(profileUser.getCompanyOrSchool());
            userInfoVO.setSubTechDirectionId(profileUser.getSubTechDirectionId());
        }

        //获得会员信息
        MembershipAccessSnapshot membership = membershipAccessService.getAccess(profileUserId);

        //组装followerCount
        LambdaQueryWrapper<UserFollow> userFollowQueryWrapper = new LambdaQueryWrapper<>();
        userFollowQueryWrapper.eq(UserFollow::getFolloweeUserId, profileUserId);
        Long followerCount = followMapper.selectCount(userFollowQueryWrapper);

        //组装followingCount
        LambdaQueryWrapper<UserFollow> userFollowingQueryWrapper = new LambdaQueryWrapper<>();
        userFollowingQueryWrapper.eq(UserFollow::getFollowerUserId, profileUserId);
        Long followingCount = followMapper.selectCount(userFollowingQueryWrapper);

        PublicUserProfileVO vo = new PublicUserProfileVO();
        vo.setUserId(profileUser.getId());
        vo.setUserInfo(userInfoVO);
        vo.setMembershipStatus(membership.status());
        vo.setMembershipType(membership.membershipType());

        vo.setFollowerCount(followerCount);
        vo.setFollowingCount(followingCount);
        vo.setSelf(self);
        vo.setBlocked(blocked);
        vo.setBlockedByCurrentUser(blockedByCurrentUser); //这个判断用于前端决定是否显示 拉黑/接触拉黑 按钮
        vo.setCanSendPrivateMessage(!self && !blocked);
        if (!self) {
            vo.setFollowedByCurrentUser(following); //用于判断展示 follow/following 按钮
            vo.setMutualFollow(mutualFollow); //用于判断展示 following/mutual 按钮
            //注意：即使是拉黑关系，也可以显示 following/mutual 按钮，这样用户可以选择拉黑之后取消关注对方

            if (blocked) {
                return vo;
            }

            //查看两个人之间是否有chatbox记录
            PrivateChatbox box = chatboxMapper.selectOne(new LambdaQueryWrapper<PrivateChatbox>()
                    .eq(PrivateChatbox::getUserAId, Math.min(currentUserId, profileUserId))
                    .eq(PrivateChatbox::getUserBId, Math.max(currentUserId, profileUserId)));
            vo.setChatboxId(box == null ? null : box.getId());
        }

        // 这些学习统计不属于拉黑状态下允许公开的信息。
        LambdaQueryWrapper<UserQuestionAnswer> userQuestionAnswerQueryWrapper = new LambdaQueryWrapper<>();
        userQuestionAnswerQueryWrapper.eq(UserQuestionAnswer::getUserId, profileUserId);
        Long answerQuestionCount = userQuestionAnswerMapper.selectCount(userQuestionAnswerQueryWrapper);

        LambdaQueryWrapper<UserQuestionAnswer> learnedBankQueryWrapper = new LambdaQueryWrapper<>();
        learnedBankQueryWrapper.eq(UserQuestionAnswer::getUserId, profileUserId);
        List<UserQuestionAnswer> userQuestionAnswers = userQuestionAnswerMapper.selectList(learnedBankQueryWrapper);
        Set<Long> learnedBankIds = userQuestionAnswers.stream().map(UserQuestionAnswer::getBankId).collect(Collectors.toSet());

        LambdaQueryWrapper<UserLearningStatDaily> dailyQueryWrapper = new LambdaQueryWrapper<>();
        dailyQueryWrapper.eq(UserLearningStatDaily::getUserId, profileUserId);
        long studySeconds = userLearningStatDailyMapper.selectList(dailyQueryWrapper).stream()
                .map(UserLearningStatDaily::getStudySeconds)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();

        vo.setAnsweredQuestionCount(answerQuestionCount);
        vo.setLearnedBankCount((long) learnedBankIds.size());
        vo.setStudyHours(Math.round(studySeconds / 3600.0));
        return vo;
    }

    /**
     * 公共主页只展示该用户自己发布的 Post，不公开评论、点赞和收藏历史。
     */
    @Override
    public List<HitPostVO> listPosts(Long currentUserId, Long profileUserId, Integer pageNum, Integer pageSize) {

        UserInfo profileUser = userInfoMapper.selectById(profileUserId);
        if (profileUser == null || profileUser.getStatus() != UserInfoStatus.ACTIVE) {
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_USER_NOT_EXIST);
        }

        boolean self = Objects.equals(currentUserId, profileUserId);
        boolean blocked = false;
        if (!self) {
            LambdaQueryWrapper<UserBlock> blockQuery = new LambdaQueryWrapper<>();
            blockQuery.and(query -> query
                    .eq(UserBlock::getBlockerUserId, currentUserId)
                    .eq(UserBlock::getBlockedUserId, profileUserId)
                    .or(reverse -> reverse
                            .eq(UserBlock::getBlockerUserId, profileUserId)
                            .eq(UserBlock::getBlockedUserId, currentUserId)));
            blocked = userBlockMapper.selectCount(blockQuery) > 0;
        }
        //拉黑之后，post 也不能看
        if (!self && blocked) {
            return List.of();
        }

        long pageNumber = pageNum == null ? 1 : Math.max(pageNum, 1);
        long pageSizeValue = pageSize == null ? 20 : Math.min(Math.max(pageSize, 1), 100);

        Page<HitPost> page = new Page<>(pageNumber, pageSizeValue, false);
        LambdaQueryWrapper<HitPost> postQuery = new LambdaQueryWrapper<>();
        postQuery.eq(HitPost::getPostUserId, profileUserId)
                .eq(HitPost::getPostStatus, HitPostStatus.PUBLISHED)
                .orderByDesc(HitPost::getCreatedTime)
                .orderByDesc(HitPost::getId);
        List<HitPost> posts = postMapper.selectPage(page, postQuery).getRecords();
        if (posts.isEmpty()) { //没查到内容，不要报错，这很有可能代表 profileUser 没发任何 post
            return List.of();
        }

        List<Long> postIds = posts.stream().map(HitPost::getId).toList();

        //查看一下 currentUser 对 profileUser 发布的 post 有没有过 互动
        LambdaQueryWrapper<HitAction> actionQuery = new LambdaQueryWrapper<>();
        actionQuery.eq(HitAction::getActionUserId, currentUserId)
                .in(HitAction::getPostId, postIds);
        List<HitAction> actions = actionMapper.selectList(actionQuery);

        //如果 currentUser 确实对 profileUser 发布的 post 有过 互动，那么做成 Map<postId, Set<HitActionType>>
        Map<Long, Set<HitActionType>> actionsMap = new HashMap<>();
        //遍历这个 互动list
        for (HitAction action : actions) { //注意，同一个 postId，可能存在多个不同的 actionType
            Long postId = action.getPostId();

            //这一步是在校验：actionsMap中，指定postId 对应的 Set<HitActionType> 是否存在
            //例如：postId = 1001, 这是第一次，那么此时 actionsMap 里是没有 这个 postId 的键值对的
            Set<HitActionType> actionsSet = actionsMap.get(postId);

            //因此要先创建一个
            if (actionsSet == null) {
                //如果是第一次， 上面的 Set<HitActionType> actionsSet 一定是 null，此时变量并没有指向 HashSet 的内存地址，因此要重新 new 一个
                actionsSet = new HashSet<>();
                //把这个postId + 空set 放进 actionsMap
                actionsMap.put(postId, actionsSet);
            }
            //然后再向 actionsSet 里添加 互动的 actionType
            HitActionType actionType = action.getActionType();
            actionsSet.add(actionType);
        }

        //这一步就是在组装 Post 的视图，当然也就包含了前面设置的 currentUser 对 profileUser 的互动情况
        List<HitPostVO> result = new ArrayList<>();
        for (HitPost post : posts) {
            //有互动，就返回 actionTypes
            Set<HitActionType> actionTypes = actionsMap.getOrDefault(post.getId(), Set.of());

            HitPostVO postVO = new HitPostVO();
            postVO.setPostId(post.getId());
            postVO.setUserId(post.getPostUserId());
            postVO.setDisplayName(profileUser.getDisplayName());
            postVO.setAvatar(userImageUrlResolver.resolveAvatar(profileUser.getAvatarObjectKey()));
            postVO.setContent(post.getContent());
            postVO.setTags(readTags(post.getTagsJson())); // post 中的标签展示
            postVO.setCommentCount(post.getCommentCount() == null ? 0 : post.getCommentCount());
            postVO.setLikeCount(post.getLikeCount() == null ? 0 : post.getLikeCount());
            postVO.setFavoriteCount(post.getFavoriteCount() == null ? 0 : post.getFavoriteCount());
            postVO.setRepostCount(post.getRepostCount() == null ? 0 : post.getRepostCount());
            postVO.setLiked(actionTypes.contains(HitActionType.LIKE));
            postVO.setFavorited(actionTypes.contains(HitActionType.FAVORITE));
            postVO.setReposted(actionTypes.contains(HitActionType.REPOST));
            postVO.setCreatedTime(post.getCreatedTime());
            result.add(postVO);
        }
        return result;
    }

    @Override
    @Transactional
    public BlockResultVO blockByCurrentUser(Long currentUserId, Long profileUserId, BlockActionDTO dto) {
        //不需要检查 dto.getBlockStatus() 是否属于 BlockStatus 枚举类，因为 enum 的特殊机制，Java 枚举不能由外部随意实例化（构造方法私有 且是 final class）
        //只可能是 BlockStatus.ACTIVATE, BlockStatus.DEACTIVATE 或 null
        //如果前端传入的 blockStatus 非已存在枚举常量，那么在 converter 转换到 枚举常量的过程中就会抛异常，例如当前端传入 3 时，没有对应常量，反序列化就会失败

        if (profileUserId == null || dto == null || dto.getBlockStatus() == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        if (currentUserId == null) {
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_NOT_AUTH);
        }

        // 公开主页只属于正常启用的用户，先校验可避免继续查询无效用户的关联数据。
        UserInfo profileUser = userInfoMapper.selectById(profileUserId);

        if (profileUser == null || profileUser.getStatus() != UserInfoStatus.ACTIVE) {
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_USER_NOT_EXIST);
        }

        BlockResultVO vo = new BlockResultVO();
        boolean self = Objects.equals(currentUserId, profileUserId);

        boolean blocked = false; //blocked 声明在外层方法代码块中，作用域一直持续到方法结束。所以if(){}里的 blocked 的结果的变化，对 blocked 始终有效，直到 blockByCurrentUser 方法结束
        if (!self) { //不能拉黑自己
            //因为blockerId和blockedId是唯一索引，因此先检查user_block表里是否存在block记录
            UserBlock existing = userBlockMapper.selectIncludingDeletedForUpdate(profileUserId, currentUserId);
            if (existing != null && Boolean.TRUE.equals(existing.getDeleted())) {
                if (dto.getBlockStatus() == BlockStatus.ACTIVATE) { //此时的 DEACTIVATE 不需要操作，因为已经是逻辑删除状态了
                    int result = userBlockMapper.restoreUserBlock(profileUserId, currentUserId);
                    if (result != 1) {
                        throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
                    }
                    blocked = true;
                }
            } else if (existing != null && Boolean.FALSE.equals(existing.getDeleted())) {
                if (dto.getBlockStatus() == BlockStatus.DEACTIVATE) {
                    int result = userBlockMapper.blockByCurrentUser(profileUserId, currentUserId);
                    if (result != 1) {
                        throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
                    }
                }
                blocked = true;
            } else {//如果没有，就创建一个新的
                //如果对一个从未拉黑过的用户进行 DEACTIVATE 操作，就是不做任何操作，因为你没拉黑过他，而且你还准备解除拉黑，那么就相当于什么都不用做
                if (dto.getBlockStatus() == BlockStatus.ACTIVATE) {
                    UserBlock userBlock = new UserBlock();
                    userBlock.setBlockerUserId(currentUserId);
                    userBlock.setBlockedUserId(profileUserId);
                    int result = userBlockMapper.insert(userBlock);
                    if (result != 1) { //依然通过报错控制返回值是否是 success
                        throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
                    }
                    blocked = true;
                }
            }
        }
        vo.setProfileUserId(profileUserId);
        vo.setSelf(self);

        //这里属于你基础知识掌握的不牢固了
        //vo.setBlocked(blocked) 就应该设置在全局作用域内，因为 boolean blocked 是 blockByCurrentUser 方法内部的全局作用域 局部变量
        //所以即便你在 if()内修改了 block，block 在全局作用域内依然有效
        //因为你把 blocked 定义在 blockByCurrentUser 的大作用域里面，所以只要还没出去这个大作用域， blocked 的栈内存就一直有效
        vo.setBlocked(blocked);
        vo.setBlockStatus(dto.getBlockStatus());

        return vo;
    }


    //因为公共主页现在需要展示用户发布的 Post，而每个 Post 可能包含标签。
    private List<String> readTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(tagsJson, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException ignored) {
            return List.of();
        }
    }
}
