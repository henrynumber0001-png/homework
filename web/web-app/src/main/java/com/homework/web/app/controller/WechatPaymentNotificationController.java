package com.homework.web.app.controller;

import com.homework.web.app.dto.MembershipPaymentConfirmationDTO;
import com.homework.web.app.service.MembershipService;
import com.homework.web.app.service.payment.WechatNativePaymentGateway;
import com.homework.web.app.service.payment.WechatNotificationRequest;
import com.wechat.pay.java.core.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信支付异步通知入口。
 *
 * <p>该地址不接受登录 Token，也不相信前端。只有通过微信签名验证和 AES-GCM
 * 解密后的交易，才会交给 MembershipService 发放权益。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "payment.wechat",
        name = "enabled",
        havingValue = "true"
)
public class WechatPaymentNotificationController {

    private final WechatNativePaymentGateway wechatGateway;
    private final MembershipService membershipService;

    @PostMapping("/api/payment/wechat/native/notify")
    public ResponseEntity<Void> paymentNotification(
            @RequestHeader("Wechatpay-Serial") String serialNumber,
            @RequestHeader("Wechatpay-Nonce") String nonce,
            @RequestHeader("Wechatpay-Signature") String signature,
            @RequestHeader("Wechatpay-Timestamp") String timestamp,
            @RequestHeader(
                    "Wechatpay-Signature-Type"
            ) String signatureType,
            // 必须保留微信发送的原始字符串；重新序列化 JSON 会导致验签失败。
            @RequestBody String rawBody
    ) {
        try {
            MembershipPaymentConfirmationDTO confirmation =
                    wechatGateway.parsePaymentNotification(
                            new WechatNotificationRequest(
                                    serialNumber,
                                    nonce,
                                    signature,
                                    timestamp,
                                    signatureType,
                                    rawBody
                            )
                    );
            membershipService.confirmPayment(confirmation);
            return ResponseEntity.ok().build();
        } catch (ValidationException exception) {
            log.warn("Wechat payment notification signature validation failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IllegalArgumentException exception) {
            log.warn(
                    "Wechat payment notification content validation failed: {}",
                    exception.getMessage()
            );
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException exception) {
            // 返回 5xx，微信会按照通知策略重试；不要错误地返回 200。
            log.error("Wechat payment notification processing failed", exception);
            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR
            ).build();
        }
    }
}
