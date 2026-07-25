package com.homework.web.app.controller;

import com.homework.web.app.controller.membership.WechatPaymentNotificationController;
import com.homework.web.app.dto.MembershipPaymentConfirmationDTO;
import com.homework.web.app.service.MembershipService;
import com.homework.web.app.service.payment.WechatNativePaymentGateway;
import com.wechat.pay.java.core.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WechatPaymentNotificationControllerTest {

    @Mock
    private WechatNativePaymentGateway wechatGateway;
    @Mock
    private MembershipService membershipService;

    @Test
    void verifiedNotificationConfirmsMembershipAndReturnsHttp200() {
        MembershipPaymentConfirmationDTO confirmation =
                new MembershipPaymentConfirmationDTO();
        when(wechatGateway.parsePaymentNotification(
                anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString()
        ))
                .thenReturn(confirmation);
        WechatPaymentNotificationController controller =
                new WechatPaymentNotificationController(
                        wechatGateway,
                        membershipService
                );

        ResponseEntity<Void> response = controller.paymentNotification(
                "serial",
                "nonce",
                "signature",
                "timestamp",
                "WECHATPAY2-SHA256-RSA2048",
                "{\"event_type\":\"TRANSACTION.SUCCESS\"}"
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(membershipService).confirmPayment(confirmation);
    }

    @Test
    void invalidWechatSignatureReturnsHttp401() {
        when(wechatGateway.parsePaymentNotification(
                anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString()
        ))
                .thenThrow(new ValidationException("bad signature"));
        WechatPaymentNotificationController controller =
                new WechatPaymentNotificationController(
                        wechatGateway,
                        membershipService
                );

        ResponseEntity<Void> response = controller.paymentNotification(
                "serial",
                "nonce",
                "signature",
                "timestamp",
                "WECHATPAY2-SHA256-RSA2048",
                "{}"
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
