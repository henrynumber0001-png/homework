package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.common.storage.UserImageUrlResolver;
import com.homework.model.entity.UserInfo;
import com.homework.model.enums.UserInfoStatus;
import com.homework.web.app.mapper.UserInfoMapper;
import com.homework.web.app.service.SearchUsersService;
import com.homework.web.app.vo.MentionUserVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchUsersServiceImpl implements SearchUsersService {
    private final UserInfoMapper userInfoMapper;
    private final UserImageUrlResolver userImageUrlResolver;

    public SearchUsersServiceImpl(UserInfoMapper userInfoMapper, UserImageUrlResolver userImageUrlResolver) {
        this.userInfoMapper = userInfoMapper;
        this.userImageUrlResolver = userImageUrlResolver;
    }

    @Override
    public List<MentionUserVO> searchUsers(Long currentUserId, String keyword, Integer limit) {

        if(currentUserId == null) {
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_NOT_AUTH);
        }

        String normalizedKeyword = keyword == null ? "" : keyword.trim();

        if (normalizedKeyword.isEmpty()) {
            return List.of();
        }

        // 联想搜索默认返回 10 条且最多 20 条，兼顾输入体验和数据库查询成本。
        int size = Math.min(limit == null ? 10 : Math.max(limit, 1), 20);

        LambdaQueryWrapper<UserInfo> userInfoQuery = new LambdaQueryWrapper<UserInfo>();
        userInfoQuery.eq(UserInfo::getStatus, UserInfoStatus.ACTIVE)
                .ne(UserInfo::getId, currentUserId)
                .and(
                        wrapper -> wrapper.likeRight(UserInfo::getAccountNo, normalizedKeyword)
                        .or().like(UserInfo::getDisplayName, normalizedKeyword)
                )
                .last("LIMIT " + size);


        List<UserInfo> targetUserInfos = userInfoMapper.selectList(userInfoQuery);
        List<MentionUserVO> mentionUserVOS = targetUserInfos.stream().map(targetUserInfo -> {
            MentionUserVO vo = new MentionUserVO();

            vo.setUserId(targetUserInfo.getId());
            vo.setAccountNo(targetUserInfo.getAccountNo());
            vo.setDisplayName(targetUserInfo.getDisplayName());
            vo.setAvatar(userImageUrlResolver.resolveAvatar(targetUserInfo.getAvatarObjectKey()));

            //Java 不知道你最后到底想把哪个对象交给 map()，所以必须要有返回值
            //Lambda表达式 必须有返回值，只不过多行方法体，不能省略 return 了
            return vo;
        }).toList();
        return mentionUserVOS;
    }
}
