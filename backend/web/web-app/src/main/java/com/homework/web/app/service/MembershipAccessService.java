package com.homework.web.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.BaseVipRecord;
import com.homework.model.entity.MembershipAccessSuspension;
import com.homework.model.entity.SvipRecord;
import com.homework.model.enums.MembershipStatus;
import com.homework.model.enums.MembershipType;
import com.homework.web.app.mapper.BaseVipRecordMapper;
import com.homework.web.app.mapper.MembershipAccessSuspensionMapper;
import com.homework.web.app.mapper.SvipRecordMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class MembershipAccessService { //查询 BaseVipRecord 和 SvipRecord，并计算当前实际会员身份（只做这一件事，因此不需要用 接口+实现类 这个思路）

    private final BaseVipRecordMapper baseVipRecordMapper;
    private final SvipRecordMapper svipRecordMapper;
    private final MembershipAccessSuspensionMapper membershipAccessSuspensionMapper;

    /**
     * 根据两张会员台账，计算用户在“当前时刻”真正生效的会员身份。
     *
     * <p>一个用户可能同时拥有一条 Premium 基础会员记录和一条 Premium Plus 高级会员记录，
     * 但业务接口只需要一个最终身份，因此这里按照 PREMIUM_PLUS &gt; PREMIUM &gt; FREE 的优先级
     * 统一解释两张表，并返回不可变的 {@link MembershipAccessSnapshot}。</p>
     *
     * <p>到期时间必须严格晚于当前时间才算有效；等于当前时间也视为已经到期。
     * 这个方法只计算会员等级，不检查 membership_access_suspension 中的暂停记录；
     * 需要同时校验暂停状态的业务应调用 {@link #requireActiveMembership(Long)}。</p>
     *
     * @param userId 要计算会员身份的用户 ID；为 null 时按游客/免费用户处理
     * @return 当前有效会员身份、当前等级及其到期时间，以及可能存在的基础会员冻结到期时间
     */
    public MembershipAccessSnapshot getAccess(Long userId) {
        // 没有用户 ID 就无法查询会员台账。这里不抛参数异常，而是返回 FREE 快照，
        // 使只需要“展示当前会员状态”的调用方可以统一按免费用户处理。
        if (userId == null) {
            return new MembershipAccessSnapshot(MembershipStatus.FREE, null, null, null);
        }

        // user_id 在两张会员表中都有唯一约束，所以每张表最多返回一条记录。
        // 即使记录已经到期也先查出来，是否有效统一在下面通过 expireTime 判断。
        LambdaQueryWrapper<SvipRecord> svipQuery = new LambdaQueryWrapper<>();
        svipQuery.eq(SvipRecord::getUserId, userId);
        SvipRecord svip = svipRecordMapper.selectOne(svipQuery);

        LambdaQueryWrapper<BaseVipRecord> baseVipQuery = new LambdaQueryWrapper<>();
        baseVipQuery.eq(BaseVipRecord::getUserId, userId);
        BaseVipRecord baseVip = baseVipRecordMapper.selectOne(baseVipQuery);

        // Premium Plus 优先级最高：只要高级会员存在、到期时间存在，并且尚未到期，
        // 就直接把当前有效身份判定为 PREMIUM_PLUS，不再让基础 Premium 覆盖它。
        if (svip != null && svip.getExpireTime() != null && svip.getExpireTime().isAfter(LocalDateTime.now())) {
            // 如果基础 Premium 的最终到期时间晚于 Premium Plus 到期时间，说明高级会员结束后
            // 仍有一段基础会员权益可以继续生效。快照把这个时间作为“冻结的基础会员到期时间”返回，
            // 供会员页面展示；这里只计算并返回结果，不会修改任何会员台账。
            // 若基础会员不存在、没有到期时间，或不晚于高级会员到期时间，则没有需要单独展示的剩余基础权益。
            LocalDateTime frozenBaseExpire = null;
            if(baseVip != null && baseVip.getExpireTime() != null && baseVip.getExpireTime().isAfter(svip.getExpireTime())) {
                frozenBaseExpire = baseVip.getExpireTime();
            }

            //这个 MembershipAccessSnapshot 因为是record，它其实是一个 设定参数的逻辑
            //也就是说，方法名虽然叫 getAccess，但实际上是层层查看当前 userId 的权限满足 哪一层
            //如果满足 Svip，那么就 new 一个 Svip 的 MembershipAccessSnapshot，把参数类型都按照 Svip 的枚举实例进行注入
            //也就是，它不是一个查询逻辑，它是主动一个设定逻辑
            MembershipAccessSnapshot membershipAccessSnapshot = new MembershipAccessSnapshot(
                    MembershipStatus.PREMIUM_PLUS, MembershipType.PREMIUM_PLUS, svip.getExpireTime(), frozenBaseExpire);
            return membershipAccessSnapshot;
        }

        // 能执行到这里，说明 Premium Plus 不存在或已经到期。
        // 此时再判断基础 Premium；只要其到期时间严格晚于 now，就把基础会员作为当前有效身份，并且不需要冻结的会员时间
        if (baseVip != null && baseVip.getExpireTime() != null && baseVip.getExpireTime().isAfter(LocalDateTime.now())) {
            return new MembershipAccessSnapshot(MembershipStatus.PREMIUM, MembershipType.PREMIUM, baseVip.getExpireTime(), null);
        }

        // 两张台账都不存在，或者已有记录均已到期时，最终身份就是 FREE。
        // 免费用户没有当前会员类型、当前到期时间或被冻结的基础会员到期时间，因此后三项均为 null。
        return new MembershipAccessSnapshot(MembershipStatus.FREE, null, null, null);
    }

    public MembershipAccessSnapshot requireActiveMembership(Long userId) {
        Long suspensionCount = membershipAccessSuspensionMapper.selectCount(
                new LambdaQueryWrapper<MembershipAccessSuspension>()
                        .eq(MembershipAccessSuspension::getUserId, userId)
                        .isNull(MembershipAccessSuspension::getResumedTime));
        if (suspensionCount > 0) {
            throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_SUSPENDED);
        }
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
