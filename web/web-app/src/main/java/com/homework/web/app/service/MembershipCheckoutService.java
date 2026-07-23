package com.homework.web.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.MembershipOrder;
import com.homework.model.enums.MembershipOrderStatus;
import com.homework.web.app.dto.MembershipOrderCreateDTO;
import com.homework.web.app.mapper.MembershipOrderMapper;
import com.homework.web.app.service.payment.WechatNativePaymentGateway;
import com.homework.web.app.vo.MembershipOrderCreateVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 创建本地会员订单，再向微信申请 Native 支付二维码。 */
@Service
@RequiredArgsConstructor
public class MembershipCheckoutService {

    private final MembershipService membershipService;
    private final MembershipOrderMapper membershipOrderMapper;
    private final List<WechatNativePaymentGateway> wechatGateways;

    public MembershipOrderCreateVO createOrder(
            String idempotencyKey,
            MembershipOrderCreateDTO dto
    ) {
        if (dto == null || dto.getPlanId() == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        if (wechatGateways.isEmpty()) {
            throw new HomeworkException(
                    ResultCodeEnum.MEMBERSHIP_PAYMENT_CHANNEL_UNAVAILABLE
            );
        }

        MembershipOrderCreateVO order = membershipService.createOrder(
                idempotencyKey,
                dto
        );
        if (order.getOrderStatus() != MembershipOrderStatus.PENDING
                || StringUtils.hasText(order.getCodeUrl())) {
            return order;
        }

        try {
            String codeUrl = wechatGateways.get(0).prepay(
                    order.getOrderNo(),
                    order.getAmountDue(),
                    order.getCurrency(),
                    order.getPaymentExpiredTime()
            );
            MembershipOrder savedOrder = membershipOrderMapper.selectOne(
                    new LambdaQueryWrapper<MembershipOrder>()
                            .eq(MembershipOrder::getOrderNo, order.getOrderNo())
                            .last("LIMIT 1")
            );
            savedOrder.setPaymentCodeUrl(codeUrl);
            membershipOrderMapper.updateById(savedOrder);
            order.setCodeUrl(codeUrl);
            return order;
        } catch (RuntimeException exception) {
            MembershipOrder failedOrder = membershipOrderMapper.selectOne(
                    new LambdaQueryWrapper<MembershipOrder>()
                            .eq(MembershipOrder::getOrderNo, order.getOrderNo())
                            .last("LIMIT 1")
            );
            if (failedOrder != null
                    && failedOrder.getOrderStatus() == MembershipOrderStatus.PENDING) {
                failedOrder.setOrderStatus(MembershipOrderStatus.PAY_FAILED);
                membershipOrderMapper.updateById(failedOrder);
            }
            throw new HomeworkException(
                    ResultCodeEnum.MEMBERSHIP_PAYMENT_GATEWAY_ERROR,
                    exception
            );
        }
    }
}
