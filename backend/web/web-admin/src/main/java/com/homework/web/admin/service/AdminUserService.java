package com.homework.web.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.PageResult;
import com.homework.common.result.ResultCodeEnum;
import com.homework.common.storage.UserImageUrlResolver;
import com.homework.model.entity.BaseVipRecord;
import com.homework.model.entity.HitComment;
import com.homework.model.entity.HitPost;
import com.homework.model.entity.SvipRecord;
import com.homework.model.entity.UserAuthIdentity;
import com.homework.model.entity.UserCommunityRestriction;
import com.homework.model.entity.UserInfo;
import com.homework.model.enums.CommunityRestrictionScope;
import com.homework.model.enums.MembershipStatus;
import com.homework.model.enums.UserInfoStatus;
import com.homework.web.admin.context.AdminContext;
import com.homework.web.admin.dto.UserAccountActionDTO;
import com.homework.web.admin.dto.UserCommunityAccessDTO;
import com.homework.model.enums.UserAccountAction;
import com.homework.web.admin.mapper.BaseVipRecordMapper;
import com.homework.web.admin.mapper.HitCommentMapper;
import com.homework.web.admin.mapper.HitPostMapper;
import com.homework.web.admin.mapper.SvipRecordMapper;
import com.homework.web.admin.mapper.UserAuthIdentityMapper;
import com.homework.web.admin.mapper.UserCommunityRestrictionMapper;
import com.homework.web.admin.mapper.UserInfoMapper;
import com.homework.web.admin.vo.ActionResultVO;
import com.homework.web.admin.vo.UserCommunityRestrictionVO;
import com.homework.web.admin.vo.UserDetailVO;
import com.homework.web.admin.vo.UserIdentityVO;
import com.homework.web.admin.vo.UserRowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 后台 App 用户查询、状态和社区访问权限管理。 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserInfoMapper userMapper;
    private final UserAuthIdentityMapper identityMapper;
    private final UserCommunityRestrictionMapper restrictionMapper;
    private final BaseVipRecordMapper baseVipMapper;
    private final SvipRecordMapper svipMapper;
    private final HitPostMapper postMapper;
    private final HitCommentMapper commentMapper;
    private final AdminAuditService auditService;
    private final UserImageUrlResolver userImageUrlResolver;

    public PageResult<UserRowVO> list(
            String keyword,
            UserInfoStatus status,
            Integer pageNum,
            Integer pageSize
    ) {
        int normalizedPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int normalizedSize = pageSize == null ? 20 : Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<UserInfo> query = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            query.and(wrapper -> wrapper.like(UserInfo::getAccountNo, keyword.trim())
                    .or().like(UserInfo::getDisplayName, keyword.trim()));
        }
        query.eq(status != null, UserInfo::getStatus, status);
        query.orderByDesc(UserInfo::getCreatedTime).orderByDesc(UserInfo::getId);
        Page<UserInfo> page = userMapper.selectPage(new Page<>(normalizedPage, normalizedSize), query);
        PageResult<UserRowVO> result = new PageResult<>();
        result.setRecords(page.getRecords().stream().map(this::toRow).toList());
        result.setTotal(page.getTotal());
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        return result;
    }

    public UserDetailVO get(Long userId) {
        UserInfo user = userMapper.selectById(userId);
        if (user == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_USER_STATE_INVALID);
        }
        UserRowVO row = toRow(user);
        UserDetailVO detail = new UserDetailVO();
        org.springframework.beans.BeanUtils.copyProperties(row, detail);
        List<UserIdentityVO> identities = new ArrayList<>();
        for (UserAuthIdentity identity : identityMapper.selectList(
                new LambdaQueryWrapper<UserAuthIdentity>()
                        .eq(UserAuthIdentity::getUserId, userId))) {
            UserIdentityVO identityVO = new UserIdentityVO();
            identityVO.setProvider(identity.getProvider());
            String account = identity.getAccount();
            if (account == null || account.length() <= 4) {
                identityVO.setMaskedIdentifier("****");
            } else if (account.contains("@")) {
                int at = account.indexOf('@');
                identityVO.setMaskedIdentifier(account.substring(0, 1) + "***" + account.substring(at));
            } else {
                identityVO.setMaskedIdentifier(account.substring(0, 2)
                        + "***" + account.substring(account.length() - 2));
            }
            identityVO.setStatus(identity.getStatus());
            identityVO.setLastUsedTime(identity.getLastUsedTime());
            identities.add(identityVO);
        }
        detail.setIdentities(identities);
        UserCommunityRestriction restriction = restrictionMapper.selectOne(
                new LambdaQueryWrapper<UserCommunityRestriction>()
                        .eq(UserCommunityRestriction::getUserId, userId)
                        .eq(UserCommunityRestriction::getActive, true)
                        .and(wrapper -> wrapper.isNull(UserCommunityRestriction::getEndTime)
                                .or().gt(UserCommunityRestriction::getEndTime, LocalDateTime.now()))
                        .orderByDesc(UserCommunityRestriction::getCreatedTime)
                        .last("LIMIT 1"));
        if (restriction != null) {
            UserCommunityRestrictionVO restrictionVO = new UserCommunityRestrictionVO();
            restrictionVO.setScope(restriction.getScope());
            restrictionVO.setStartTime(restriction.getStartTime());
            restrictionVO.setEndTime(restriction.getEndTime());
            restrictionVO.setReason(restriction.getReason());
            detail.setCommunityRestriction(restrictionVO);
        }
        detail.setPostCount(postMapper.selectCount(
                new LambdaQueryWrapper<HitPost>().eq(HitPost::getPostUserId, userId)));
        detail.setCommentCount(commentMapper.selectCount(
                new LambdaQueryWrapper<HitComment>().eq(HitComment::getCommentUserId, userId)));
        return detail;
    }

    @Transactional
    public ActionResultVO action(Long userId, UserAccountActionDTO dto) {
        UserInfo user = userMapper.selectById(userId);
        if (user == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_USER_STATE_INVALID);
        }
        if (!user.getVersion().equals(dto.getVersion())) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
        }
        UserAccountAction action = dto.getAction();
        UserInfoStatus targetStatus;
        if (action == UserAccountAction.DISABLE && user.getStatus() == UserInfoStatus.ACTIVE) {
            targetStatus = UserInfoStatus.DISABLED;
        } else if (action == UserAccountAction.ACTIVATE && user.getStatus() == UserInfoStatus.DISABLED) {
            targetStatus = UserInfoStatus.ACTIVE;
        } else if (action == UserAccountAction.BAN
                && (user.getStatus() == UserInfoStatus.ACTIVE || user.getStatus() == UserInfoStatus.DISABLED)) {
            targetStatus = UserInfoStatus.BANNED;
        } else if (action == UserAccountAction.UNBAN && user.getStatus() == UserInfoStatus.BANNED) {
            targetStatus = UserInfoStatus.ACTIVE;
        } else {
            throw new HomeworkException(ResultCodeEnum.ADMIN_USER_STATE_INVALID);
        }
        UserInfo before = new UserInfo();
        org.springframework.beans.BeanUtils.copyProperties(user, before);
        user.setStatus(targetStatus);
        if (userMapper.updateById(user) == 0) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
        }
        UserInfo updated = userMapper.selectById(userId);
        auditService.record("USER", action.name(), "USER", userId, dto.getReason(), before, updated);
        ActionResultVO result = new ActionResultVO();
        result.setTargetId(userId);
        result.setAction(action.getCode());
        result.setStatus(updated.getStatus().getCode());
        result.setVersion(updated.getVersion());
        result.setUpdatedTime(updated.getUpdatedTime());
        return result;
    }

    @Transactional
    public UserDetailVO updateCommunityAccess(Long userId, UserCommunityAccessDTO dto) {
        UserInfo user = userMapper.selectById(userId);
        if (user == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_USER_STATE_INVALID);
        }
        if (!user.getVersion().equals(dto.getVersion())
                || userMapper.updateVersion(userId, dto.getVersion()) == 0) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
        }
        List<UserCommunityRestriction> activeRestrictions = restrictionMapper.selectList(
                new LambdaQueryWrapper<UserCommunityRestriction>()
                        .eq(UserCommunityRestriction::getUserId, userId)
                        .eq(UserCommunityRestriction::getActive, true));
        for (UserCommunityRestriction restriction : activeRestrictions) {
            restriction.setActive(false);
            restrictionMapper.updateById(restriction);
        }
        if (Boolean.TRUE.equals(dto.getRestricted())) {
            CommunityRestrictionScope scope = dto.getScope();
            if (scope == null) {
                throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
            }
            if (dto.getReason() == null || dto.getReason().isBlank()
                    || dto.getEndTime() != null && !dto.getEndTime().isAfter(LocalDateTime.now())) {
                throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
            }
            UserCommunityRestriction restriction = new UserCommunityRestriction();
            restriction.setUserId(userId);
            restriction.setScope(scope);
            restriction.setStartTime(LocalDateTime.now());
            restriction.setEndTime(dto.getEndTime());
            restriction.setActive(true);
            restriction.setReason(dto.getReason().trim());
            restriction.setAdminId(AdminContext.getAdminId());
            restriction.setVersion(0);
            restrictionMapper.insert(restriction);
        }
        auditService.record(
                "USER",
                Boolean.TRUE.equals(dto.getRestricted()) ? "RESTRICT_COMMUNITY" : "RESTORE_COMMUNITY",
                "USER",
                userId,
                dto.getReason(),
                activeRestrictions,
                dto
        );
        return get(userId);
    }

    public UserRowVO toRow(UserInfo user) {
        UserRowVO vo = new UserRowVO();
        vo.setId(user.getId());
        vo.setAccountNo(user.getAccountNo());
        vo.setDisplayName(user.getDisplayName());
        vo.setAvatar(userImageUrlResolver.resolveAvatar(user.getAvatarObjectKey()));
        vo.setStatus(user.getStatus());
        LocalDateTime now = LocalDateTime.now();
        SvipRecord svip = svipMapper.selectOne(new LambdaQueryWrapper<SvipRecord>()
                .eq(SvipRecord::getUserId, user.getId())
                .orderByDesc(SvipRecord::getExpireTime)
                .last("LIMIT 1"));
        BaseVipRecord baseVip = baseVipMapper.selectOne(new LambdaQueryWrapper<BaseVipRecord>()
                .eq(BaseVipRecord::getUserId, user.getId())
                .orderByDesc(BaseVipRecord::getExpireTime)
                .last("LIMIT 1"));
        if (svip != null && svip.getExpireTime() != null && svip.getExpireTime().isAfter(now)) {
            vo.setMembershipType(MembershipStatus.PREMIUM_PLUS);
        } else if (baseVip != null && baseVip.getExpireTime() != null && baseVip.getExpireTime().isAfter(now)) {
            vo.setMembershipType(MembershipStatus.PREMIUM);
        } else {
            vo.setMembershipType(MembershipStatus.FREE);
        }
        vo.setRegisteredTime(user.getCreatedTime());
        vo.setVersion(user.getVersion());
        return vo;
    }
}
