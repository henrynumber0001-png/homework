package com.homework.web.app.service;

import com.homework.model.entity.MembershipOrder;
import com.homework.model.enums.MembershipOrderStatus;
import com.homework.web.app.dto.MembershipOrderCreateDTO;
import com.homework.web.app.mapper.MembershipOrderMapper;
import com.homework.web.app.service.payment.WechatNativePaymentGateway;
import com.homework.web.app.vo.MembershipOrderCreateVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipCheckoutServiceTest {

    @Mock
    private MembershipService membershipService;
    @Mock
    private MembershipOrderMapper orderMapper;
    @Mock
    private WechatNativePaymentGateway wechatGateway;

    private MembershipCheckoutService checkoutService;

    @BeforeEach
    void setUp() {
        checkoutService = new MembershipCheckoutService(
                membershipService,
                orderMapper,
                List.of(wechatGateway)
        );
    }

    @Test
    void pendingOrderGetsWechatCodeUrl() {
        MembershipOrderCreateVO localOrder = pendingOrder();
        MembershipOrder savedOrder = new MembershipOrder();
        savedOrder.setOrderNo("order-1");
        savedOrder.setOrderStatus(MembershipOrderStatus.PENDING);
        when(membershipService.createOrder(any(), any())).thenReturn(localOrder);
        when(wechatGateway.prepay(
                "order-1",
                new BigDecimal("129.00"),
                "CNY",
                localOrder.getPaymentExpiredTime()
        )).thenReturn("weixin://wxpay/example");
        when(orderMapper.selectOne(any())).thenReturn(savedOrder);

        MembershipOrderCreateDTO dto = new MembershipOrderCreateDTO();
        dto.setPlanId(3L);
        MembershipOrderCreateVO result = checkoutService.createOrder("key-1", dto);

        assertEquals("weixin://wxpay/example", result.getCodeUrl());
        assertEquals("weixin://wxpay/example", savedOrder.getPaymentCodeUrl());
        verify(orderMapper).updateById(savedOrder);
    }

    @Test
    void idempotentRetryReturnsStoredCodeUrl() {
        MembershipOrderCreateVO localOrder = pendingOrder();
        localOrder.setCodeUrl("weixin://wxpay/stored");
        when(membershipService.createOrder(any(), any())).thenReturn(localOrder);

        MembershipOrderCreateDTO dto = new MembershipOrderCreateDTO();
        dto.setPlanId(3L);
        MembershipOrderCreateVO result = checkoutService.createOrder("key-1", dto);

        assertEquals("weixin://wxpay/stored", result.getCodeUrl());
        verify(wechatGateway, never()).prepay(any(), any(), any(), any());
    }

    private MembershipOrderCreateVO pendingOrder() {
        MembershipOrderCreateVO order = new MembershipOrderCreateVO();
        order.setOrderNo("order-1");
        order.setOrderStatus(MembershipOrderStatus.PENDING);
        order.setAmountDue(new BigDecimal("129.00"));
        order.setCurrency("CNY");
        order.setPaymentExpiredTime(LocalDateTime.now().plusMinutes(15));
        return order;
    }
}
