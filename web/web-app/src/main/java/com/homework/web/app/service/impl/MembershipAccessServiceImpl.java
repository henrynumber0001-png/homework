package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.MembershipSubscription;
import com.homework.model.enums.MembershipSubscriptionStatus;
import com.homework.web.app.mapper.MembershipSubscriptionMapper;
import com.homework.web.app.service.MembershipAccessService;
import com.homework.web.app.service.MembershipAccessSnapshot;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 会员权限判断的统一入口。
 *
 * <p>题库、考试、AI 评分和 AI 追问都通过本服务判断权限，不要在各业务模块中
 * 分别复制“查询订阅、检查状态、检查到期时间”的代码，否则不同模块容易产生
 * 不一致的会员有效规则。
 *
 * <p>本类只读取会员权益，不负责购买、升级、续费或修改 Subscription。
 */
@Service
@RequiredArgsConstructor
public class MembershipAccessServiceImpl implements MembershipAccessService {

    private final MembershipSubscriptionMapper membershipSubscriptionMapper;

    @Override
    public MembershipAccessSnapshot getAccess(Long userId) {
        // 第 1 步：没有 userId 表示当前请求没有可识别的登录用户。
        // getAccess 是“查询型”方法，因此不抛异常，而是返回一份无权限快照。
        if (userId == null) {
            return new MembershipAccessSnapshot(false, null, null, null);
        }

        // 第 2 步：一个用户只允许有一条当前订阅，所以按 userId 查询一条即可。
        // 这里是普通权限读取，不修改数据，因此不需要 SELECT ... FOR UPDATE。
        MembershipSubscription subscription = membershipSubscriptionMapper.selectOne(
                new LambdaQueryWrapper<MembershipSubscription>()
                        .eq(MembershipSubscription::getUserId, userId)
                        .last("LIMIT 1")
        );

        // 第 3 步：会员有效必须同时满足四个条件：
        // 1）订阅记录存在；
        // 2）数据库状态是 ACTIVE；
        // 3）到期时间不为空；
        // 4）到期时间晚于服务器当前时间。
        //
        // 不能只检查 status，因为定时任务可能还没来得及把已经到期的记录改为 EXPIRED。
        boolean active = subscription != null
                && subscription.getStatus() == MembershipSubscriptionStatus.ACTIVE
                && subscription.getCurrentPeriodEnd() != null
                && subscription.getCurrentPeriodEnd().isAfter(LocalDateTime.now());

        // 第 4 步：没有有效会员时只返回 active=false。
        // 不返回过期会员的 type 和 billingType，避免调用方误把历史套餐当成当前权限。
        if (active == false) { // !就是取反的意思，所以 active 永远不可能 等于 !active
            return new MembershipAccessSnapshot(false, null, null, null);
        }

        // 第 5 步：有效会员返回一份不可变的权限快照。
        // 调用方只读取这份快照，不直接接触或修改 MembershipSubscription 实体。
        return new MembershipAccessSnapshot(true, subscription.getMembershipType(), subscription.getBillingType(), subscription.getCurrentPeriodEnd());
    }

    @Override
    public MembershipAccessSnapshot requireActiveMembership(Long userId) {
        // 先复用 getAccess 的统一判断规则，再将“查询结果”转换为“访问要求”。
        // Standard 和 Premium 都能通过该检查，因此适用于题库内容和考试入口。
        MembershipAccessSnapshot access = getAccess(userId);
        if (!access.active()) {
            // 抛出业务异常后，Controller 不会继续执行受保护功能，
            // 前端可根据错误码引导用户进入会员购买页面。
            throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_REQUIRED);
        }
        return access;
    }

    @Override
    public MembershipAccessSnapshot requirePremium(Long userId) {
        // Premium 检查分成两层：
        // 第一层先保证用户至少有有效会员；第二层再检查会员等级。
        // 这样非会员和 Standard 用户能够得到不同、更加准确的错误提示。
        MembershipAccessSnapshot access = requireActiveMembership(userId);
        if (!access.premium()) {
            // Standard 可以使用全部题库，但 AI 评分和 AI 追问需要 Premium。
            throw new HomeworkException(ResultCodeEnum.PREMIUM_MEMBERSHIP_REQUIRED);
        }
        return access;
    }
}
