package com.homework.web.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.BaseVipRecord;
import com.homework.model.entity.SvipRecord;
import com.homework.model.enums.MembershipStatus;
import com.homework.model.enums.MembershipType;
import com.homework.web.app.mapper.BaseVipRecordMapper;
import com.homework.web.app.mapper.SvipRecordMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class MembershipAccessService { //查询 BaseVipRecord 和 SvipRecord，并计算当前实际会员身份（只做这一件事，因此不需要用 接口+实现类 这个思路）

    private final BaseVipRecordMapper baseVipRecordMapper;
    private final SvipRecordMapper svipRecordMapper;

    public MembershipAccessSnapshot getAccess(Long userId) {
        if (userId == null) {
            return new MembershipAccessSnapshot(
                    MembershipStatus.FREE, null, null, null
            );
        }

        LocalDateTime now = LocalDateTime.now();
        SvipRecord svip = svipRecordMapper.selectOne(
                new LambdaQueryWrapper<SvipRecord>()
                        .eq(SvipRecord::getUserId, userId)
                        .last("LIMIT 1")
        );
        BaseVipRecord baseVip = baseVipRecordMapper.selectOne(
                new LambdaQueryWrapper<BaseVipRecord>()
                        .eq(BaseVipRecord::getUserId, userId)
                        .last("LIMIT 1")
        );

        if (svip != null
                && svip.getExpireTime() != null
                && svip.getExpireTime().isAfter(now)) {
            LocalDateTime frozenBaseExpire = baseVip != null
                    && baseVip.getExpireTime() != null
                    && baseVip.getExpireTime().isAfter(svip.getExpireTime())
                    ? baseVip.getExpireTime()
                    : null;
            return new MembershipAccessSnapshot(
                    MembershipStatus.PREMIUM_PLUS,
                    MembershipType.PREMIUM_PLUS,
                    svip.getExpireTime(),
                    frozenBaseExpire
            );
        }

        if (baseVip != null
                && baseVip.getExpireTime() != null
                && baseVip.getExpireTime().isAfter(now)) {
            return new MembershipAccessSnapshot(
                    MembershipStatus.PREMIUM,
                    MembershipType.PREMIUM,
                    baseVip.getExpireTime(),
                    null
            );
        }

        return new MembershipAccessSnapshot(
                MembershipStatus.FREE, null, null, null
        );
    }

    public MembershipAccessSnapshot requireActiveMembership(Long userId) {
        MembershipAccessSnapshot access = getAccess(userId);
        if (access.status() == MembershipStatus.FREE) {
            throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_REQUIRED);
        }
        return access;
    }

    public MembershipAccessSnapshot requirePremiumPlus(Long userId) {
        MembershipAccessSnapshot access = requireActiveMembership(userId);
        if (access.status() != MembershipStatus.PREMIUM_PLUS) {
            throw new HomeworkException(
                    ResultCodeEnum.PREMIUM_PLUS_MEMBERSHIP_REQUIRED
            );
        }
        return access;
    }
}
