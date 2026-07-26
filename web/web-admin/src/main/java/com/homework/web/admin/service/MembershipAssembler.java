package com.homework.web.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.BaseVipRecord;
import com.homework.model.entity.MembershipAccessSuspension;
import com.homework.model.entity.MembershipChangeRecord;
import com.homework.model.entity.SvipRecord;
import com.homework.model.entity.UserInfo;
import com.homework.web.admin.mapper.BaseVipRecordMapper;
import com.homework.web.admin.mapper.MembershipAccessSuspensionMapper;
import com.homework.web.admin.mapper.MembershipChangeRecordMapper;
import com.homework.web.admin.mapper.SvipRecordMapper;
import com.homework.web.admin.mapper.UserInfoMapper;
import com.homework.web.admin.vo.MembershipChangeVO;
import com.homework.web.admin.vo.MembershipDetailVO;
import com.homework.web.admin.vo.MembershipRowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/** 汇总用户双会员台账、暂停记录和管理员变更流水。 */
@Service
@RequiredArgsConstructor
public class MembershipAssembler {

    private final UserInfoMapper userMapper;
    private final BaseVipRecordMapper baseVipMapper;
    private final SvipRecordMapper svipMapper;
    private final MembershipAccessSuspensionMapper suspensionMapper;
    private final MembershipChangeRecordMapper changeMapper;

    public MembershipDetailVO toDetail(Long userId) {
        UserInfo user = userMapper.selectById(userId);
        if (user == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_USER_STATE_INVALID);
        }
        BaseVipRecord baseVip = findBaseVip(userId);
        SvipRecord svip = findSvip(userId);
        MembershipAccessSuspension suspension = findActiveSuspension(userId);
        LocalDateTime now = LocalDateTime.now();
        MembershipDetailVO vo = new MembershipDetailVO();
        vo.setUserId(userId);
        vo.setAccountNo(user.getAccountNo());
        vo.setDisplayName(user.getDisplayName());
        vo.setPremiumExpireTime(baseVip == null ? null : baseVip.getExpireTime());
        vo.setPremiumPlusExpireTime(svip == null ? null : svip.getExpireTime());
        if (svip != null && svip.getExpireTime() != null && svip.getExpireTime().isAfter(now)) {
            vo.setCurrentType("PREMIUM_PLUS");
        } else if (baseVip != null && baseVip.getExpireTime() != null && baseVip.getExpireTime().isAfter(now)) {
            vo.setCurrentType("PREMIUM");
        }
        vo.setSuspended(suspension != null);
        if (suspension != null) {
            vo.setAccessStatus("SUSPENDED");
        } else if (vo.getCurrentType() != null) {
            vo.setAccessStatus("ACTIVE");
        } else {
            vo.setAccessStatus("EXPIRED");
        }
        vo.setLedgerVersion(getLedgerVersion(userId, baseVip, svip));
        List<MembershipChangeVO> changes = changeMapper.selectList(
                        new LambdaQueryWrapper<MembershipChangeRecord>()
                                .eq(MembershipChangeRecord::getUserId, userId)
                                .orderByDesc(MembershipChangeRecord::getCreatedTime)
                                .orderByDesc(MembershipChangeRecord::getId)
                                .last("LIMIT 20"))
                .stream()
                .map(change -> {
                    MembershipChangeVO changeVO = new MembershipChangeVO();
                    changeVO.setChangeType(change.getChangeType().name());
                    changeVO.setMembershipType(
                            change.getMembershipType() == null ? null : change.getMembershipType().name());
                    changeVO.setDurationMonths(change.getDurationMonths());
                    changeVO.setReason(change.getReason());
                    changeVO.setAdminId(change.getAdminId());
                    changeVO.setCreatedTime(change.getCreatedTime());
                    return changeVO;
                })
                .toList();
        vo.setRecentChanges(changes);
        return vo;
    }

    public MembershipRowVO toRow(Long userId) {
        MembershipDetailVO detail = toDetail(userId);
        MembershipRowVO row = new MembershipRowVO();
        org.springframework.beans.BeanUtils.copyProperties(detail, row);
        return row;
    }

    public BaseVipRecord findBaseVip(Long userId) {
        return baseVipMapper.selectOne(new LambdaQueryWrapper<BaseVipRecord>()
                .eq(BaseVipRecord::getUserId, userId)
                .orderByDesc(BaseVipRecord::getId)
                .last("LIMIT 1"));
    }

    public SvipRecord findSvip(Long userId) {
        return svipMapper.selectOne(new LambdaQueryWrapper<SvipRecord>()
                .eq(SvipRecord::getUserId, userId)
                .orderByDesc(SvipRecord::getId)
                .last("LIMIT 1"));
    }

    public MembershipAccessSuspension findActiveSuspension(Long userId) {
        return suspensionMapper.selectOne(new LambdaQueryWrapper<MembershipAccessSuspension>()
                .eq(MembershipAccessSuspension::getUserId, userId)
                .isNull(MembershipAccessSuspension::getResumedTime)
                .orderByDesc(MembershipAccessSuspension::getId)
                .last("LIMIT 1"));
    }

    public int getLedgerVersion(Long userId, BaseVipRecord baseVip, SvipRecord svip) {
        long changeCount = changeMapper.selectCount(new LambdaQueryWrapper<MembershipChangeRecord>()
                .eq(MembershipChangeRecord::getUserId, userId));
        int baseVersion = baseVip == null || baseVip.getVersion() == null ? 0 : baseVip.getVersion();
        int svipVersion = svip == null || svip.getVersion() == null ? 0 : svip.getVersion();
        return Math.toIntExact(changeCount + baseVersion + svipVersion);
    }
}
