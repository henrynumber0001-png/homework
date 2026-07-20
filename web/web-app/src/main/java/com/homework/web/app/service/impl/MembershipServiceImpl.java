package com.homework.web.app.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.MembershipOrder;
import com.homework.model.entity.MembershipPlan;
import com.homework.model.entity.MembershipSubscription;
import com.homework.model.entity.UserInfo;
import com.homework.model.enums.BillingType;
import com.homework.model.enums.MembershipOrderAction;
import com.homework.model.enums.MembershipOrderPayType;
import com.homework.model.enums.MembershipOrderStatus;
import com.homework.model.enums.MembershipPlanAction;
import com.homework.model.enums.MembershipSubscriptionStatus;
import com.homework.model.enums.MembershipType;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.dto.MembershipOrderCreateDTO;
import com.homework.web.app.dto.MembershipPaymentConfirmationDTO;
import com.homework.web.app.dto.MembershipPlanChangeDTO;
import com.homework.web.app.mapper.MembershipOrderMapper;
import com.homework.web.app.mapper.MembershipPlanMapper;
import com.homework.web.app.mapper.MembershipSubscriptionMapper;
import com.homework.web.app.mapper.UserInfoMapper;
import com.homework.web.app.service.MembershipService;
import com.homework.web.app.vo.MembershipOrderCreateVO;
import com.homework.web.app.vo.MembershipOrderHistoryVO;
import com.homework.web.app.vo.MembershipOrderStatusVO;
import com.homework.web.app.vo.MembershipPageVO;
import com.homework.web.app.vo.MembershipPlanChangeVO;
import com.homework.web.app.vo.MembershipPlanOptionVO;
import com.homework.web.app.vo.MembershipSubscriptionVO;
import com.homework.web.app.vo.PaymentPayloadVO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 会员体系的核心业务服务。
 *
 * <p>理解本类时先记住三个对象的职责：
 * MembershipPlan 表示“平台卖什么”，MembershipOrder 表示“用户付了什么钱”，
 * MembershipSubscription 表示“用户现在拥有什么权益”。创建订单不等于获得权益，
 * 只有可信支付回调确认成功后，才会更新 Subscription。
 */
@Service
@RequiredArgsConstructor
public class MembershipServiceImpl implements MembershipService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final String DEFAULT_CURRENCY = "CNY";

    private final MembershipPlanMapper membershipPlanMapper;
    private final MembershipSubscriptionMapper membershipSubscriptionMapper;
    private final MembershipOrderMapper membershipOrderMapper;
    private final UserInfoMapper userInfoMapper;

    @Override
    public MembershipPageVO getMembershipPage() {
        // 第 1 步：查询用户当前唯一的一条订阅，并根据状态和到期时间判断是否有效。
        Long userId = requireLoginUser();
        MembershipSubscription subscription = findSubscription(userId, false); //首先只是查询当前用户是不是会员，还没到购买页面，所以 forUpdate = false
        boolean active = isActive(subscription);

        // 第 2 步：套餐价格和上下架状态以服务端 membership_plan 为准，不能相信前端传价。
        List<MembershipPlan> plans = membershipPlanMapper.selectList(
                new LambdaQueryWrapper<MembershipPlan>()
                        .eq(MembershipPlan::getEnabled, Boolean.TRUE)
                        .orderByAsc(MembershipPlan::getMembershipType)
                        .orderByAsc(MembershipPlan::getBillingType)
        );

        // 第 3 步：把当前订阅和每个套餐可执行的动作一起返回。
        // 前端只需根据 action 渲染“购买、升级、当前套餐、预约变更或不可用”。
        MembershipPageVO page = new MembershipPageVO();
        page.setCurrentSubscription(toSubscriptionVO(subscription));
        List<MembershipPlanOptionVO> options = new ArrayList<>(plans.size());
        for (MembershipPlan plan : plans) {
            options.add(toPlanOption(plan, subscription, active));
        }
        page.setPlans(options);
        return page;
    }

    @Override
    public MembershipSubscriptionVO getCurrentSubscription(Long userId) {
        // 该方法供 UserCenter 等模块复用，统一输出会员等级、周期和 AI 权限。
        if (userId == null) {
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_NOT_AUTH);
        }
        return toSubscriptionVO(findSubscription(userId,false));
    }

    @Transactional
    @Override
    public MembershipOrderCreateVO createOrder(String idempotencyKey, MembershipOrderCreateDTO dto) {
        // 第 1 步：校验登录状态、幂等键、目标套餐和支付方式。
        Long userId = requireLoginUser();
        if (!StringUtils.hasText(idempotencyKey) || idempotencyKey.length() > 64 || dto == null
                || dto.getPlanId() == null || dto.getPayType() == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        // 第 2 步：锁住 user_info 行。同一用户的购买、升级和支付回调按顺序执行，
        // 避免两个并发请求同时判断“当前没有待支付订单”并各自创建一笔订单。
        lockUser(userId);

        // 第 3 步：幂等处理。前端双击或网络重试使用同一个 Idempotency-Key 时，
        // 返回原订单，而不是重复扣款。
        MembershipOrder existing = membershipOrderMapper.selectOne(
                new LambdaQueryWrapper<MembershipOrder>()
                        .eq(MembershipOrder::getUserId, userId)
                        .eq(MembershipOrder::getIdempotencyKey, idempotencyKey)
                        .last("LIMIT 1")
        );
        if (existing != null) {
            return toOrderCreateVO(existing); //已经找到使用相同幂等键创建的订单，不再创建新订单，直接把原订单返回给前端。
        }

        // 第 4 步：先关闭已超时的 PENDING 订单，再确保当前没有另一笔待支付订单。
        expireStalePendingOrders(userId);
        Long pendingCount = membershipOrderMapper.selectCount(
                new LambdaQueryWrapper<MembershipOrder>()
                        .eq(MembershipOrder::getUserId, userId)
                        .eq(MembershipOrder::getOrderStatus, MembershipOrderStatus.PENDING)
        );
        if (pendingCount > 0) {
            throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_CHANGE_IN_PROGRESS);
        }

        // 第 5 步：从数据库读取目标套餐和当前订阅。价格、类型和 BillingType 都由后端确定。
        MembershipPlan targetPlan = requireEnabledPlan(dto.getPlanId());
        MembershipSubscription subscription = findSubscription(userId, true);
        boolean active = isActive(subscription);

        // 第 6 步：决定这次是首次购买还是即时升级。
        // 降级和同等级 Monthly/Yearly 切换不在这里收费，而是在本期结束时预约生效。
        MembershipOrderAction action;
        BigDecimal appliedCredit = ZERO;
        if (!active) {
            action = MembershipOrderAction.PURCHASE;
        } else if (subscription.getMembershipType() == MembershipType.STANDARD
                && targetPlan.getMembershipType() == MembershipType.PREMIUM) {
            // 升级期间不能同时存在另一笔升级订单或预约套餐变更。
            if (subscription.getPendingPlanId() != null || subscription.getPendingOrderId() != null) {
                throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_CHANGE_IN_PROGRESS);
            }
            // 财务规则：只允许 Monthly -> Monthly、Yearly -> Yearly 的同周期升级。
            if (subscription.getBillingType() != targetPlan.getBillingType()) {
                throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_BILLING_TYPE_MISMATCH);
            }
            action = MembershipOrderAction.UPGRADE;
            // ChatGPT 风格的即时升级：旧 Standard 周期尚未使用的价值抵扣 Premium 首期费用。
            appliedCredit = calculateUnusedCurrentPeriodValue(subscription, LocalDateTime.now());
            if (appliedCredit.compareTo(money(targetPlan.getPrice())) > 0) {
                // With same-billing upgrades, Premium must remain more expensive
                // than Standard. Reject a bad catalogue instead of creating a
                // negative invoice or an off-ledger carried balance.
                throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_INVALID_CHANGE);
            }
        } else {
            // Current plan, downgrade and monthly/yearly switches never create an
            // immediate charge. They use the scheduled plan-change endpoint.
            throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_INVALID_CHANGE);
        }

        // 第 7 步：生成 PENDING 订单。这里绝不能直接写 PAID，也不能提前发放会员权益。
        LocalDateTime now = LocalDateTime.now();
        MembershipOrder order = new MembershipOrder();
        order.setUserId(userId);
        order.setOrderNo(IdUtil.getSnowflakeNextIdStr());
        order.setAction(action);
        order.setFromPlanId(active ? subscription.getPlanId() : null);
        order.setToPlanId(targetPlan.getId());
        order.setMembershipType(targetPlan.getMembershipType());
        order.setBillingType(targetPlan.getBillingType());
        order.setOriginalAmount(money(targetPlan.getPrice()));
        order.setCreditAmount(appliedCredit);
        order.setPayAmount(money(targetPlan.getPrice().subtract(appliedCredit)));
        order.setCurrency(normalizeCurrency(targetPlan.getCurrency()));
        order.setPayType(dto.getPayType());
        order.setOrderStatus(MembershipOrderStatus.PENDING);
        order.setIdempotencyKey(idempotencyKey);
        order.setPaymentExpiredTime(now.plusMinutes(15));
        // 记录创建订单时订阅的版本。回调到达时如果版本已变化，说明订单已过时。
        order.setSourceSubscriptionVersion(active ? versionOf(subscription) : null);
        membershipOrderMapper.insert(order);

        // 第 8 步：升级订单占用当前订阅，阻止用户在支付完成前再次发起会员变更。
        if (action == MembershipOrderAction.UPGRADE) {
            subscription.setPendingOrderId(order.getId());
            membershipSubscriptionMapper.updateById(subscription);
        }

        // 理论上同周期 Premium 价格应高于 Standard；仍保留 0 元订单的内部完成能力，
        // 但不会伪造第三方支付交易。
        if (order.getPayAmount().compareTo(ZERO) == 0) {
            completePaidOrder(order, now, "internal-credit-" + order.getOrderNo());
        }
        return toOrderCreateVO(order);
    }

    @Transactional
    @Override
    public MembershipOrderCreateVO createRenewalOrder(
            Long userId,
            String idempotencyKey,
            MembershipOrderPayType payType
    ) {
        // 第 1 步：这是供自动续费任务/支付适配器调用的内部入口，不是普通用户重复购买接口。
        if (userId == null || !StringUtils.hasText(idempotencyKey)
                || idempotencyKey.length() > 64 || payType == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        // 与 createOrder、confirmPayment 保持相同的“先锁用户”顺序，降低死锁风险。
        lockUser(userId);

        // 第 2 步：同一续费批次重复执行时返回原订单，防止定时任务重复扣款。
        MembershipOrder existing = membershipOrderMapper.selectOne(
                new LambdaQueryWrapper<MembershipOrder>()
                        .eq(MembershipOrder::getUserId, userId)
                        .eq(MembershipOrder::getIdempotencyKey, idempotencyKey)
                        .last("LIMIT 1")
        );
        if (existing != null) {
            return toOrderCreateVO(existing);
        }

        // 第 3 步：订阅必须已经到期、开启自动续费，并且没有其他待处理订单。
        expireStalePendingOrders(userId);
        MembershipSubscription subscription = findSubscription(userId, true);
        LocalDateTime now = LocalDateTime.now();
        if (subscription == null
                || subscription.getStatus() == MembershipSubscriptionStatus.CANCELLED
                || !Boolean.TRUE.equals(subscription.getAutoRenew())
                || subscription.getCurrentPeriodEnd() == null
                || subscription.getCurrentPeriodEnd().isAfter(now)
                || subscription.getPendingOrderId() != null) {
            throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_INVALID_CHANGE);
        }

        // 第 4 步：如果用户预约过降级/周期切换，续费目标使用 pendingPlanId；
        // 否则继续购买当前 planId。预约变更因此只在新周期真正付款后生效。
        Long targetPlanId = subscription.getPendingPlanId() == null
                ? subscription.getPlanId()
                : subscription.getPendingPlanId();
        MembershipPlan targetPlan = requireEnabledPlan(targetPlanId);

        // 第 5 步：续费是一个完整的新周期，不做旧周期剩余价值抵扣。
        MembershipOrder order = new MembershipOrder();
        order.setUserId(userId);
        order.setOrderNo(IdUtil.getSnowflakeNextIdStr());
        order.setAction(MembershipOrderAction.RENEWAL);
        order.setFromPlanId(subscription.getPlanId());
        order.setToPlanId(targetPlan.getId());
        order.setMembershipType(targetPlan.getMembershipType());
        order.setBillingType(targetPlan.getBillingType());
        order.setOriginalAmount(money(targetPlan.getPrice()));
        order.setCreditAmount(ZERO);
        order.setPayAmount(money(targetPlan.getPrice()));
        order.setCurrency(normalizeCurrency(targetPlan.getCurrency()));
        order.setPayType(payType);
        order.setOrderStatus(MembershipOrderStatus.PENDING);
        order.setIdempotencyKey(idempotencyKey);
        order.setPaymentExpiredTime(now.plusMinutes(15));
        order.setSourceSubscriptionVersion(versionOf(subscription));
        membershipOrderMapper.insert(order);

        // 第 6 步：记录正在处理的续费订单。支付成功后 completePaidOrder 会清除此字段。
        subscription.setPendingOrderId(order.getId());
        membershipSubscriptionMapper.updateById(subscription);
        if (order.getPayAmount().compareTo(ZERO) == 0) {
            completePaidOrder(order, now, "internal-credit-" + order.getOrderNo());
        }
        return toOrderCreateVO(order);
    }

    @Transactional
    @Override
    public MembershipPlanChangeVO schedulePlanChange(MembershipPlanChangeDTO dto) {
        // 第 1 步：锁定并读取当前有效订阅，确保预约变更不会和支付回调并发覆盖。
        Long userId = requireLoginUser();
        if (dto == null || dto.getPlanId() == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        lockUser(userId);
        MembershipSubscription subscription = findSubscription(userId, true);
        if (!isActive(subscription)) {
            throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_REQUIRED);
        }
        if (subscription.getPendingOrderId() != null) {
            throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_CHANGE_IN_PROGRESS);
        }

        // 第 2 步：验证目标套餐。当前套餐不能重复预约。
        MembershipPlan targetPlan = requireEnabledPlan(dto.getPlanId());
        if (targetPlan.getId().equals(subscription.getPlanId())) {
            throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_INVALID_CHANGE);
        }
        if (subscription.getMembershipType() == MembershipType.STANDARD
                && targetPlan.getMembershipType() == MembershipType.PREMIUM) {
            // Standard -> Premium 必须走即时升级订单，不能借预约接口绕过同 BillingType 校验。
            throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_INVALID_CHANGE);
        }

        // 第 3 步：只登记“下一周期用什么套餐”，当前周期的等级和权限完全不变。
        // 例如 Premium Yearly 降级 Standard Yearly：本年度继续使用 Premium，且本期不退款。
        subscription.setPendingPlanId(targetPlan.getId());
        subscription.setPendingChangeTime(subscription.getCurrentPeriodEnd());
        // 订阅状态发生变化时版本 +1，使此前基于旧版本创建的订单无法覆盖新状态。
        subscription.setSubscriptionVersion(versionOf(subscription) + 1);
        membershipSubscriptionMapper.updateById(subscription);

        MembershipPlanChangeVO vo = new MembershipPlanChangeVO();
        vo.setMembershipType(targetPlan.getMembershipType());
        vo.setBillingType(targetPlan.getBillingType());
        vo.setEffectiveAt(subscription.getCurrentPeriodEnd());
        return vo;
    }

    @Transactional
    @Override
    public void cancelScheduledPlanChange() {
        // 取消的只是“下周期变更计划”，不会取消当前会员权益。
        Long userId = requireLoginUser();
        lockUser(userId);
        MembershipSubscription subscription = findSubscription(userId, true);
        if (!isActive(subscription)) {
            throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_REQUIRED);
        }
        if (subscription.getPendingPlanId() == null) {
            // 幂等：没有预约时重复取消也视为成功。
            return;
        }
        subscription.setPendingPlanId(null);
        subscription.setPendingChangeTime(null);
        subscription.setSubscriptionVersion(versionOf(subscription) + 1);
        membershipSubscriptionMapper.updateById(subscription);
    }

    @Override
    public List<MembershipOrderHistoryVO> getOrderHistory() {
        // 历史记录来自订单快照，不从当前套餐反推，避免套餐改价后历史账单跟着变化。
        Long userId = requireLoginUser();
        List<MembershipOrder> orders = membershipOrderMapper.selectList(
                new LambdaQueryWrapper<MembershipOrder>()
                        .eq(MembershipOrder::getUserId, userId)
                        .orderByDesc(MembershipOrder::getId)
        );
        return orders.stream().map(this::toOrderHistoryVO).toList();
    }

    @Transactional
    @Override
    public MembershipOrderStatusVO getOrderStatus(String orderNo) {
        // 前端支付页面轮询此接口获得 PENDING、PAID、EXPIRED 等最终状态。
        Long userId = requireLoginUser();
        if (!StringUtils.hasText(orderNo)) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        // 查询前先锁用户并清理超时订单，确保返回给前端的状态不是已经失效的 PENDING。
        lockUser(userId);
        expireStalePendingOrders(userId);
        MembershipOrder order = membershipOrderMapper.selectOne(
                new LambdaQueryWrapper<MembershipOrder>()
                        .eq(MembershipOrder::getUserId, userId)
                        .eq(MembershipOrder::getOrderNo, orderNo)
                        .last("LIMIT 1")
        );
        if (order == null) {
            throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_ORDER_NOT_FOUND);
        }
        MembershipOrderStatusVO vo = new MembershipOrderStatusVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderStatus(order.getOrderStatus());
        return vo;
    }

    @Transactional
    @Override
    public void confirmPayment(MembershipPaymentConfirmationDTO confirmation) {
        // 第 1 步：该 DTO 必须来自“已经验签”的微信/支付宝回调适配器，
        // 不能直接暴露给前端，否则用户可以伪造支付成功。
        if (confirmation == null || !StringUtils.hasText(confirmation.getOrderNo())
                || !StringUtils.hasText(confirmation.getProviderTradeNo())
                || confirmation.getPaidAmount() == null
                || !StringUtils.hasText(confirmation.getCurrency())
                || confirmation.getPayType() == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        // 第 2 步：先预读订单获得 userId，再按“用户 -> 订单”的固定顺序加锁。
        // createOrder 也先锁用户，统一加锁顺序可以降低数据库死锁风险。
        MembershipOrder orderPreview = membershipOrderMapper.selectOne(
                new LambdaQueryWrapper<MembershipOrder>()
                        .eq(MembershipOrder::getOrderNo, confirmation.getOrderNo())
                        .last("LIMIT 1")
        );
        if (orderPreview == null) {
            throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_ORDER_NOT_FOUND);
        }
        lockUser(orderPreview.getUserId());
        MembershipOrder order = membershipOrderMapper.selectOne(
                new LambdaQueryWrapper<MembershipOrder>()
                        .eq(MembershipOrder::getOrderNo, confirmation.getOrderNo())
                        .last("LIMIT 1 FOR UPDATE")
        );
        if (order.getOrderStatus() == MembershipOrderStatus.PAID) {
            // 支付平台可能重复通知。订单已经完成时直接返回，保证回调幂等。
            return;
        }
        // 第 3 步：验证订单状态和支付发生时间。使用支付平台给出的 paidTime，
        // 避免“用户按时付款，但回调网络延迟”被误判为超时。
        LocalDateTime paidTime =
                confirmation.getPaidTime() == null ? LocalDateTime.now() : confirmation.getPaidTime();
        if (order.getOrderStatus() != MembershipOrderStatus.PENDING
                || order.getPaymentExpiredTime() == null
                || paidTime.isAfter(order.getPaymentExpiredTime())) {
            throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_ORDER_STATE_ERROR);
        }
        if (money(order.getPayAmount()).compareTo(money(confirmation.getPaidAmount())) != 0
                || order.getPayType() != confirmation.getPayType()
                || !normalizeCurrency(order.getCurrency())
                .equals(normalizeCurrency(confirmation.getCurrency()))) {
            // 金额、币种和支付渠道都必须与服务端订单快照完全一致。
            throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_PAYMENT_MISMATCH);
        }
        // 第 4 步：订单校验完成后，统一完成订单并发放/切换订阅权益。
        completePaidOrder(
                order,
                paidTime,
                confirmation.getProviderTradeNo()
        );
    }

    private void completePaidOrder(MembershipOrder order, LocalDateTime paidTime, String providerTradeNo) {
        // 第 1 步：重新读取订单购买的目标套餐。订单只保存 planId 和快照，
        // 权益发放仍需确认目标套餐真实存在。
        MembershipPlan targetPlan = membershipPlanMapper.selectById(order.getToPlanId());
        if (targetPlan == null) {
            throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_PLAN_NOT_FOUND);
        }

        // 第 2 步：锁住当前订阅，并按订单类型执行最后一次防御性校验。
        MembershipSubscription subscription = findSubscription(order.getUserId(), true);
        if (order.getAction() == MembershipOrderAction.UPGRADE) {
            // 升级必须仍然是 Standard -> Premium、BillingType 相同，
            // pendingOrderId 和 subscriptionVersion 也必须与创建订单时一致。
            if (!isActive(subscription)
                    || subscription.getMembershipType() != MembershipType.STANDARD
                    || targetPlan.getMembershipType() != MembershipType.PREMIUM
                    || subscription.getBillingType() != targetPlan.getBillingType()
                    || order.getMembershipType() != targetPlan.getMembershipType()
                    || order.getBillingType() != targetPlan.getBillingType()
                    || !order.getId().equals(subscription.getPendingOrderId())
                    || !versionOf(subscription).equals(order.getSourceSubscriptionVersion())) {
                throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_ORDER_STATE_ERROR);
            }
        } else if (order.getAction() == MembershipOrderAction.RENEWAL) {
            // 续费必须对应当前等待处理的订单，并使用当前 planId 或用户预约的 pendingPlanId。
            if (subscription == null
                    || subscription.getStatus() == MembershipSubscriptionStatus.CANCELLED
                    || !order.getId().equals(subscription.getPendingOrderId())
                    || !versionOf(subscription).equals(order.getSourceSubscriptionVersion())
                    || (subscription.getPendingPlanId() == null
                    && !order.getToPlanId().equals(subscription.getPlanId()))
                    || (subscription.getPendingPlanId() != null
                    && !order.getToPlanId().equals(subscription.getPendingPlanId()))) {
                throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_ORDER_STATE_ERROR);
            }
        } else if (isActive(subscription)) {
            // PURCHASE 只适用于没有有效订阅的用户，防止用普通购买覆盖现有会员。
            throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_ORDER_STATE_ERROR);
        }

        // 第 3 步：支付成功时间是新周期起点；Monthly 加一个月，Yearly 加一年。
        LocalDateTime periodEnd = addBillingPeriod(paidTime, targetPlan.getBillingType());
        order.setPayTime(paidTime);
        order.setProviderTradeNo(providerTradeNo);
        order.setOrderStatus(MembershipOrderStatus.PAID);
        order.setPeriodStart(paidTime);
        order.setPeriodEnd(periodEnd);
        membershipOrderMapper.updateById(order);

        // 第 4 步：首次购买没有订阅记录，因此创建一条；一个用户始终只保留一条当前订阅。
        if (subscription == null) {
            subscription = new MembershipSubscription();
            subscription.setUserId(order.getUserId());
            subscription.setSubscriptionVersion(1L);
            subscription.setAutoRenew(Boolean.TRUE);
            applyPlan(subscription, targetPlan, order, paidTime, periodEnd);
            membershipSubscriptionMapper.insert(subscription);
            return;
        }

        // 第 5 步：升级或续费更新原订阅，并清空已完成的预约/待支付状态。
        // 版本 +1，之后迟到的旧支付回调会因为版本不匹配而被拒绝。
        subscription.setSubscriptionVersion(versionOf(subscription) + 1);
        subscription.setAutoRenew(Boolean.TRUE);
        subscription.setPendingPlanId(null);
        subscription.setPendingChangeTime(null);
        subscription.setPendingOrderId(null);
        applyPlan(subscription, targetPlan, order, paidTime, periodEnd);
        membershipSubscriptionMapper.updateById(subscription);
    }

    private void applyPlan(
            MembershipSubscription subscription,
            MembershipPlan plan,
            MembershipOrder order,
            LocalDateTime periodStart,
            LocalDateTime periodEnd
    ) {
        // 将“支付成功后的最终套餐状态”集中写入 Subscription，
        // 避免购买、升级、续费三个分支各自遗漏某个字段。
        subscription.setPlanId(plan.getId());
        subscription.setMembershipType(plan.getMembershipType());
        subscription.setBillingType(plan.getBillingType());
        subscription.setStatus(MembershipSubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(periodStart);
        subscription.setCurrentPeriodEnd(periodEnd);
        subscription.setCurrentPeriodAmount(money(plan.getPrice()));
        subscription.setLatestPaidOrderId(order.getId());
    }

    private MembershipPlanOptionVO toPlanOption(
            MembershipPlan plan,
            MembershipSubscription subscription,
            boolean active
    ) {
        // 先填充套餐的固定展示信息。两种会员都能访问全部题库，
        // Premium 额外开放 AI 评分和 AI 追问。
        MembershipPlanOptionVO vo = new MembershipPlanOptionVO();
        vo.setPlanId(plan.getId());
        vo.setMembershipType(plan.getMembershipType());
        vo.setBillingType(plan.getBillingType());
        vo.setPrice(money(plan.getPrice()));
        vo.setCurrency(normalizeCurrency(plan.getCurrency()));
        vo.setInterviewBanksEnabled(Boolean.TRUE);
        vo.setCertificateBanksEnabled(Boolean.TRUE);
        boolean premium = plan.getMembershipType() == MembershipType.PREMIUM;
        vo.setAiEvaluationEnabled(premium);
        vo.setAiFollowUpEnabled(premium);

        // 决策顺序 1：没有有效会员时，所有上架套餐都是首次购买。
        if (!active) {
            vo.setAction(MembershipPlanAction.PURCHASE);
            vo.setCreditAmount(ZERO);
            vo.setAmountDue(money(plan.getPrice()));
            return vo;
        }
        // 决策顺序 2：用户已经预约该套餐，按钮显示“已预约”及生效时间。
        if (plan.getId().equals(subscription.getPendingPlanId())) {
            vo.setAction(MembershipPlanAction.SCHEDULED);
            vo.setEffectiveAt(subscription.getPendingChangeTime());
            return vo;
        }
        // 决策顺序 3：当前套餐不可重复购买。
        if (plan.getId().equals(subscription.getPlanId())) {
            vo.setAction(MembershipPlanAction.CURRENT);
            return vo;
        }
        // 决策顺序 4：Standard -> Premium 属于升级。
        if (subscription.getMembershipType() == MembershipType.STANDARD
                && plan.getMembershipType() == MembershipType.PREMIUM) {
            // 跨 BillingType 不允许即时升级，前端应根据 UNAVAILABLE 将按钮置灰。
            if (subscription.getBillingType() != plan.getBillingType()) {
                vo.setAction(MembershipPlanAction.UNAVAILABLE);
                vo.setUnavailableReason(
                        "Monthly 会员只能升级为 Premium Monthly，Yearly 会员只能升级为 Premium Yearly"
                );
                return vo;
            }
            // 同 BillingType 升级时，展示旧套餐剩余价值和本次应补金额。
            BigDecimal appliedCredit =
                    calculateUnusedCurrentPeriodValue(subscription, LocalDateTime.now());
            if (appliedCredit.compareTo(money(plan.getPrice())) > 0) {
                vo.setAction(MembershipPlanAction.UNAVAILABLE);
                vo.setUnavailableReason("套餐价格配置异常，请联系客服");
                return vo;
            }
            vo.setAction(MembershipPlanAction.UPGRADE);
            vo.setCreditAmount(appliedCredit);
            vo.setAmountDue(money(plan.getPrice().subtract(appliedCredit)));
            return vo;
        }
        // 决策顺序 5：其余情况是到期后变更，例如降级或 Monthly/Yearly 切换。
        vo.setAction(MembershipPlanAction.SCHEDULE_CHANGE);
        vo.setEffectiveAt(subscription.getCurrentPeriodEnd());
        return vo;
    }

    private MembershipSubscriptionVO toSubscriptionVO(MembershipSubscription subscription) {
        // Subscription 即使数据库状态仍为 ACTIVE，只要 currentPeriodEnd 已经过期，
        // 对外也必须按无有效权益处理。
        MembershipSubscriptionVO vo = new MembershipSubscriptionVO();
        boolean active = isActive(subscription);
        vo.setActive(active);
        if (subscription == null) {
            return vo;
        }
        vo.setMembershipType(subscription.getMembershipType());
        vo.setBillingType(subscription.getBillingType());
        vo.setStatus(active ? subscription.getStatus() : MembershipSubscriptionStatus.EXPIRED);
        vo.setCurrentPeriodStart(subscription.getCurrentPeriodStart());
        vo.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd());
        vo.setAutoRenew(subscription.getAutoRenew());
        boolean premium = active && subscription.getMembershipType() == MembershipType.PREMIUM;
        vo.setAiEvaluationEnabled(premium);
        vo.setAiFollowUpEnabled(premium);
        if (subscription.getPendingPlanId() != null) {
            // 额外返回下周期套餐，方便前端显示“将在某日变更为某套餐”。
            MembershipPlan pending = membershipPlanMapper.selectById(subscription.getPendingPlanId());
            if (pending != null) {
                vo.setPendingMembershipType(pending.getMembershipType());
                vo.setPendingBillingType(pending.getBillingType());
                vo.setPendingChangeTime(subscription.getPendingChangeTime());
            }
        }
        return vo;
    }

    private MembershipOrderCreateVO toOrderCreateVO(MembershipOrder order) {
        // 创建订单响应返回服务端账单数据；微信预下单成功后还会包含二维码 codeUrl。
        MembershipOrderCreateVO vo = new MembershipOrderCreateVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setAction(order.getAction());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setOriginalAmount(money(order.getOriginalAmount()));
        vo.setCreditAmount(money(order.getCreditAmount()));
        vo.setAmountDue(money(order.getPayAmount()));
        vo.setCurrency(order.getCurrency());
        vo.setPaymentExpiredTime(order.getPaymentExpiredTime());
        if (order.getPayType() == MembershipOrderPayType.WECHAT
                && StringUtils.hasText(order.getPaymentCodeUrl())) {
            vo.setPaymentPayload(new PaymentPayloadVO(
                    MembershipOrderPayType.WECHAT,
                    "NATIVE",
                    order.getPaymentCodeUrl()
            ));
        }
        return vo;
    }

    private MembershipOrderHistoryVO toOrderHistoryVO(MembershipOrder order) {
        // 使用订单自己的类型、价格和周期快照，保证历史账单不可被当前套餐配置污染。
        MembershipOrderHistoryVO vo = new MembershipOrderHistoryVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setAction(order.getAction());
        vo.setMembershipType(order.getMembershipType());
        vo.setBillingType(order.getBillingType());
        vo.setOriginalAmount(money(order.getOriginalAmount()));
        vo.setCreditAmount(money(order.getCreditAmount()));
        vo.setPayAmount(money(order.getPayAmount()));
        vo.setCurrency(order.getCurrency());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setPeriodStart(order.getPeriodStart());
        vo.setPeriodEnd(order.getPeriodEnd());
        vo.setPayTime(order.getPayTime());
        return vo;
    }

    private BigDecimal calculateUnusedCurrentPeriodValue(
            MembershipSubscription subscription,
            LocalDateTime now
    ) {
        // 即时升级抵扣公式：
        // 未使用价值 = 当前周期实付金额 × 剩余毫秒数 ÷ 周期总毫秒数。
        // 例如 49 元月费使用一半后升级，可抵扣约 24.50 元。
        if (!isActive(subscription)
                || subscription.getCurrentPeriodStart() == null
                || subscription.getCurrentPeriodAmount() == null) {
            return ZERO;
        }
        long totalMillis = Duration.between(
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd()
        ).toMillis();
        long remainingMillis = Math.max(
                0,
                Duration.between(now, subscription.getCurrentPeriodEnd()).toMillis()
        );
        if (totalMillis <= 0 || remainingMillis <= 0) {
            return ZERO;
        }
        return money(subscription.getCurrentPeriodAmount()
                .multiply(BigDecimal.valueOf(remainingMillis))
                .divide(BigDecimal.valueOf(totalMillis), 2, RoundingMode.HALF_UP));
    }

    private void expireStalePendingOrders(Long userId) {
        // 支付窗口超过 15 分钟仍未成功的订单，从 PENDING 改为 EXPIRED。
        LocalDateTime now = LocalDateTime.now();
        List<MembershipOrder> staleOrders = membershipOrderMapper.selectList(
                new LambdaQueryWrapper<MembershipOrder>()
                        .eq(MembershipOrder::getUserId, userId)
                        .eq(MembershipOrder::getOrderStatus, MembershipOrderStatus.PENDING)
                        // 微信 Native 订单必须先向微信查单/关单，再由对账任务改为 EXPIRED。
                        // 这样不会把“用户按时付款但回调延迟”的订单错误地提前关闭。
                        .ne(MembershipOrder::getPayType, MembershipOrderPayType.WECHAT)
                        .le(MembershipOrder::getPaymentExpiredTime, now)
        );
        if (!staleOrders.isEmpty()) {
            for (MembershipOrder order : staleOrders) {
                order.setOrderStatus(MembershipOrderStatus.EXPIRED);
                membershipOrderMapper.updateById(order);
            }
            // 若超时订单曾占用 subscription.pendingOrderId，也必须释放，
            // 否则用户以后无法再次升级或续费。
            MembershipSubscription subscription = findSubscription(userId, true);
            if (subscription != null && subscription.getPendingOrderId() != null
                    && staleOrders.stream().anyMatch(order -> order.getId().equals(subscription.getPendingOrderId()))) {
                subscription.setPendingOrderId(null);
                membershipSubscriptionMapper.updateById(subscription);
            }
        }
    }

    private MembershipPlan requireEnabledPlan(Long planId) {
        // 所有收费入口都通过该方法读取套餐，确保套餐存在、已上架且价格合法。
        MembershipPlan plan = membershipPlanMapper.selectOne(
                new LambdaQueryWrapper<MembershipPlan>()
                        .eq(MembershipPlan::getId, planId)
                        .eq(MembershipPlan::getEnabled, Boolean.TRUE)
                        .last("LIMIT 1")
        );
        if (plan == null || plan.getPrice() == null || plan.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_PLAN_NOT_FOUND);
        }
        return plan;
    }

    private MembershipSubscription findSubscription(Long userId, boolean forUpdate) {
        // forUpdate=true 时使用行锁，供购买、续费、预约变更和支付回调的事务使用。
        // 普通页面查询不加锁，减少数据库锁竞争。
        LambdaQueryWrapper<MembershipSubscription> query =
                new LambdaQueryWrapper<MembershipSubscription>()
                        .eq(MembershipSubscription::getUserId, userId)
                        .last(forUpdate ? "LIMIT 1 FOR UPDATE" : "LIMIT 1");//查询这条订阅，并锁住它。在当前事务提交或回滚之前，其他事务不能同时修改这条记录。
        return membershipSubscriptionMapper.selectOne(query);
    }

    private void lockUser(Long userId) {
        // Subscription 在首次购买前可能还不存在，无法锁住“不存在的行”。
        // user_info 一定存在，因此用用户行作为该用户所有会员写操作的统一互斥锁。
        UserInfo user = userInfoMapper.selectOne(
                new LambdaQueryWrapper<UserInfo>()
                        .eq(UserInfo::getId, userId)
                        .last("LIMIT 1 FOR UPDATE")
        );
        if (user == null) {
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_USER_NOT_EXIST);
        }
    }

    private boolean isActive(MembershipSubscription subscription) {
        // 会员有效必须同时满足：有记录、状态 ACTIVE、到期时间存在且晚于服务器当前时间。
        return subscription != null
                && subscription.getStatus() == MembershipSubscriptionStatus.ACTIVE
                && subscription.getCurrentPeriodEnd() != null
                && subscription.getCurrentPeriodEnd().isAfter(LocalDateTime.now());
    }

    private LocalDateTime addBillingPeriod(LocalDateTime start, BillingType billingType) {
        // 统一计算新周期结束时间，避免不同业务分支各自计算产生不一致。
        if (billingType == BillingType.MONTHLY) {
            return start.plusMonths(1);
        }
        if (billingType == BillingType.YEARLY) {
            return start.plusYears(1);
        }
        throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
    }

    private Long versionOf(MembershipSubscription subscription) {
        // 兼容历史空值；新订阅从版本 1 开始。
        return subscription.getSubscriptionVersion() == null ? 0L : subscription.getSubscriptionVersion();
    }

    private BigDecimal money(BigDecimal amount) {
        // 所有金额统一保留两位小数并采用 HALF_UP，避免不同接口返回不同精度。
        return amount == null ? ZERO : amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeCurrency(String currency) {
        // 旧套餐未配置币种时按人民币处理。
        return StringUtils.hasText(currency) ? currency : DEFAULT_CURRENCY;
    }

    private Long requireLoginUser() {
        // 会员页面和用户主动操作都要求登录；支付回调通过订单找到用户，不依赖登录上下文。
        Long userId = LoginUserHolder.getUserId();
        if (userId == null) {
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_NOT_AUTH);
        }
        return userId;
    }
}
