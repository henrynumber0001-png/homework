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
public class MembershipCheckoutService { //负责组织整个支付下单流程，包括创建本地订单，调用GateWay向微信申请支付二维码，返回code_url给前端，处理预支付失败

    private final MembershipService membershipService;
    private final MembershipOrderMapper membershipOrderMapper;
    private final List<WechatNativePaymentGateway> wechatGateways;

    public MembershipOrderCreateVO createOrder(String idempotencyKey, MembershipOrderCreateDTO dto) {
        if (dto == null || dto.getPlanId() == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        if (wechatGateways.isEmpty()) {
            throw new HomeworkException(
                    ResultCodeEnum.MEMBERSHIP_PAYMENT_CHANNEL_UNAVAILABLE
            );
        }

        //调用 membershipService 创建订单，然后装配 二维码url，返回给前端
        MembershipOrderCreateVO orderResponse = membershipService.createOrder(idempotencyKey, dto);
        if (orderResponse.getOrderStatus() != MembershipOrderStatus.PENDING
                || StringUtils.hasText(orderResponse.getCodeUrl())) {
            return orderResponse;
        }

        try {
            //调用 wechatGateways 获取 微信二维码，返回给前端
            String codeUrl = wechatGateways.get(0).prepay(orderResponse.getOrderNo(), orderResponse.getAmountDue(), orderResponse.getCurrency(), orderResponse.getPaymentExpiredTime());
            MembershipOrder order = membershipOrderMapper.selectOne(
                    new LambdaQueryWrapper<MembershipOrder>()
                            .eq(MembershipOrder::getOrderNo, orderResponse.getOrderNo())
                            .last("LIMIT 1")
            );
            order.setPaymentCodeUrl(codeUrl);
            membershipOrderMapper.updateById(order);
            orderResponse.setCodeUrl(codeUrl); //补全 创建订单的最后一个环节，就是二维码url，然后返回给前端，供用户扫描
            return orderResponse;
        } catch (RuntimeException exception) {
            MembershipOrder failedOrder = membershipOrderMapper.selectOne(
                    new LambdaQueryWrapper<MembershipOrder>()
                            .eq(MembershipOrder::getOrderNo, orderResponse.getOrderNo())
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
