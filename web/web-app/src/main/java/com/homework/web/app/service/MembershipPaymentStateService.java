package com.homework.web.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.model.entity.MembershipOrder;
import com.homework.model.entity.MembershipSubscription;
import com.homework.model.entity.UserInfo;
import com.homework.model.enums.MembershipOrderPayType;
import com.homework.model.enums.MembershipOrderStatus;
import com.homework.web.app.mapper.MembershipOrderMapper;
import com.homework.web.app.mapper.MembershipSubscriptionMapper;
import com.homework.web.app.mapper.UserInfoMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 支付适配层对会员订单状态的最小写入入口。
 *
 * <p>微信网络调用不放在本类事务中。本类只在平台调用结束后短暂加锁并更新数据库，
 * 避免持有数据库行锁等待外部网络。
 */
@Service
@RequiredArgsConstructor
public class MembershipPaymentStateService {

    private final MembershipOrderMapper membershipOrderMapper;
    private final MembershipSubscriptionMapper membershipSubscriptionMapper;
    private final UserInfoMapper userInfoMapper;

    /**
     * 保存 Native code_url，并返回最终持久化的值，支持相同幂等请求并发重试。
     */
    @Transactional
    public String recordWechatCodeUrl(String orderNo, String codeUrl) {
        MembershipOrder order = lockOrderInUserOrder(orderNo);
        if (order == null
                || (order.getOrderStatus() != MembershipOrderStatus.PENDING
                && order.getOrderStatus() != MembershipOrderStatus.PAID)) {
            return null;
        }
        if (StringUtils.hasText(order.getPaymentCodeUrl())) {
            return order.getPaymentCodeUrl();
        }
        order.setPaymentCodeUrl(codeUrl);
        membershipOrderMapper.updateById(order);
        return codeUrl;
    }

    @Transactional(readOnly = true)
    public List<String> findExpiredPendingWechatOrderNumbers(
            LocalDateTime now
    ) {
        return membershipOrderMapper.selectList(
                        new LambdaQueryWrapper<MembershipOrder>()
                                .eq(
                                        MembershipOrder::getPayType,
                                        MembershipOrderPayType.WECHAT
                                )
                                .eq(
                                        MembershipOrder::getOrderStatus,
                                        MembershipOrderStatus.PENDING
                                )
                                .le(
                                        MembershipOrder::getPaymentExpiredTime,
                                        now
                                )
                                .orderByAsc(MembershipOrder::getId)
                ).stream()
                .map(MembershipOrder::getOrderNo)
                .toList();
    }

    @Transactional
    public void markExpired(String orderNo) {
        finishPendingOrder(orderNo, MembershipOrderStatus.EXPIRED);
    }

    @Transactional
    public void markPrepayFailed(String orderNo) {
        finishPendingOrder(orderNo, MembershipOrderStatus.PAY_FAILED);
    }

    private void finishPendingOrder(
            String orderNo,
            MembershipOrderStatus finalStatus
    ) {
        MembershipOrder order = lockOrderInUserOrder(orderNo);
        if (order == null || order.getOrderStatus() != MembershipOrderStatus.PENDING) {
            return;
        }

        order.setOrderStatus(finalStatus);
        membershipOrderMapper.updateById(order);
        releasePendingSubscriptionOrder(order);
    }

    /**
     * 加锁顺序与 MembershipServiceImpl.confirmPayment() 保持一致：
     * 先锁 user_info，再锁 membership_order，降低支付回调和过期任务的死锁风险。
     */
    private MembershipOrder lockOrderInUserOrder(String orderNo) {
        MembershipOrder preview = membershipOrderMapper.selectOne(
                new LambdaQueryWrapper<MembershipOrder>()
                        .eq(MembershipOrder::getOrderNo, orderNo)
                        .last("LIMIT 1")
        );
        if (preview == null) {
            return null;
        }
        userInfoMapper.selectOne(
                new LambdaQueryWrapper<UserInfo>()
                        .eq(UserInfo::getId, preview.getUserId())
                        .last("LIMIT 1 FOR UPDATE")
        );
        return membershipOrderMapper.selectOne(
                new LambdaQueryWrapper<MembershipOrder>()
                        .eq(MembershipOrder::getOrderNo, orderNo)
                        .last("LIMIT 1 FOR UPDATE")
        );
    }

    private void releasePendingSubscriptionOrder(MembershipOrder order) {
        MembershipSubscription subscription =
                membershipSubscriptionMapper.selectOne(
                        new LambdaQueryWrapper<MembershipSubscription>()
                                .eq(
                                        MembershipSubscription::getUserId,
                                        order.getUserId()
                                )
                                .last("LIMIT 1 FOR UPDATE")
                );
        if (subscription != null
                && order.getId().equals(subscription.getPendingOrderId())) {
            subscription.setPendingOrderId(null);
            membershipSubscriptionMapper.updateById(subscription);
        }
    }
}
