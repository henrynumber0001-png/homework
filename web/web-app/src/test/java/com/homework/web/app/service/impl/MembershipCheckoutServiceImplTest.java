package com.homework.web.app.service.impl;

import com.homework.model.enums.MembershipOrderPayType;
import com.homework.model.enums.MembershipOrderStatus;
import com.homework.web.app.dto.MembershipOrderCreateDTO;
import com.homework.web.app.dto.MembershipPaymentConfirmationDTO;
import com.homework.web.app.service.MembershipPaymentStateService;
import com.homework.web.app.service.MembershipService;
import com.homework.web.app.service.payment.PaymentGateway;
import com.homework.web.app.service.payment.PaymentGatewayRegistry;
import com.homework.web.app.service.payment.PaymentPrepayRequest;
import com.homework.web.app.service.payment.PaymentReconciliationResult;
import com.homework.web.app.vo.MembershipOrderCreateVO;
import com.homework.web.app.vo.PaymentPayloadVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipCheckoutServiceImplTest {

    @Mock
    private MembershipService membershipService;
    @Mock
    private MembershipPaymentStateService paymentStateService;
    @Mock
    private PaymentGatewayRegistry paymentGatewayRegistry;
    @Mock
    private PaymentGateway paymentGateway;

    private MembershipCheckoutServiceImpl checkoutService;

    @BeforeEach
    void setUp() {
        checkoutService = new MembershipCheckoutServiceImpl(
                membershipService,
                paymentStateService,
                paymentGatewayRegistry
        );
    }

    @Test
    void pendingOrderCreatesWechatNativePrepayAndReturnsCodeUrl() {
        MembershipOrderCreateDTO dto = new MembershipOrderCreateDTO();
        dto.setPlanId(3L);
        dto.setPayType(MembershipOrderPayType.WECHAT);
        MembershipOrderCreateVO localOrder = pendingOrder();
        PaymentPayloadVO payload = new PaymentPayloadVO(
                MembershipOrderPayType.WECHAT,
                "NATIVE",
                "weixin://wxpay/example"
        );

        when(paymentGatewayRegistry.require(MembershipOrderPayType.WECHAT))
                .thenReturn(paymentGateway);
        when(membershipService.createOrder("checkout-1", dto))
                .thenReturn(localOrder);
        when(paymentGateway.prepay(
                org.mockito.ArgumentMatchers.any(PaymentPrepayRequest.class)
        )).thenReturn(payload);
        when(paymentStateService.recordWechatCodeUrl(
                "order-1",
                "weixin://wxpay/example"
        )).thenReturn("weixin://wxpay/example");

        MembershipOrderCreateVO result =
                checkoutService.createOrder("checkout-1", dto);

        assertEquals("weixin://wxpay/example",
                result.getPaymentPayload().getCodeUrl());
        ArgumentCaptor<PaymentPrepayRequest> requestCaptor =
                ArgumentCaptor.forClass(PaymentPrepayRequest.class);
        verify(paymentGateway).prepay(requestCaptor.capture());
        assertEquals(new BigDecimal("79.00"), requestCaptor.getValue().amount());
        assertEquals("CNY", requestCaptor.getValue().currency());
    }

    @Test
    void idempotentRetryReturnsStoredCodeUrlWithoutCallingWechatAgain() {
        MembershipOrderCreateDTO dto = new MembershipOrderCreateDTO();
        dto.setPlanId(3L);
        dto.setPayType(MembershipOrderPayType.WECHAT);
        MembershipOrderCreateVO localOrder = pendingOrder();
        localOrder.setPaymentPayload(new PaymentPayloadVO(
                MembershipOrderPayType.WECHAT,
                "NATIVE",
                "weixin://wxpay/stored"
        ));

        when(paymentGatewayRegistry.require(MembershipOrderPayType.WECHAT))
                .thenReturn(paymentGateway);
        when(membershipService.createOrder("checkout-1", dto))
                .thenReturn(localOrder);

        MembershipOrderCreateVO result =
                checkoutService.createOrder("checkout-1", dto);

        assertEquals("weixin://wxpay/stored",
                result.getPaymentPayload().getCodeUrl());
        verify(paymentGateway, never()).prepay(
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void lostPrepayResponseButWechatQueryPaidDoesNotReportFailure() {
        MembershipOrderCreateDTO dto = new MembershipOrderCreateDTO();
        dto.setPlanId(3L);
        dto.setPayType(MembershipOrderPayType.WECHAT);
        MembershipOrderCreateVO localOrder = pendingOrder();
        MembershipPaymentConfirmationDTO confirmation =
                new MembershipPaymentConfirmationDTO();

        when(paymentGatewayRegistry.require(MembershipOrderPayType.WECHAT))
                .thenReturn(paymentGateway);
        when(membershipService.createOrder("checkout-1", dto))
                .thenReturn(localOrder);
        when(paymentGateway.prepay(
                org.mockito.ArgumentMatchers.any(PaymentPrepayRequest.class)
        )).thenThrow(new IllegalStateException("response lost"));
        when(paymentGateway.reconcileExpiredOrder("order-1"))
                .thenReturn(PaymentReconciliationResult.paid(confirmation));

        MembershipOrderCreateVO result =
                checkoutService.createOrder("checkout-1", dto);

        assertEquals(MembershipOrderStatus.PAID, result.getOrderStatus());
        verify(membershipService).confirmPayment(confirmation);
    }

    private MembershipOrderCreateVO pendingOrder() {
        MembershipOrderCreateVO order = new MembershipOrderCreateVO();
        order.setOrderNo("order-1");
        order.setOrderStatus(MembershipOrderStatus.PENDING);
        order.setAmountDue(new BigDecimal("79.00"));
        order.setCurrency("CNY");
        order.setPaymentExpiredTime(LocalDateTime.now().plusMinutes(15));
        return order;
    }
}
