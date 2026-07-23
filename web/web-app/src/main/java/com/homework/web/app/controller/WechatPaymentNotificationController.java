package com.homework.web.app.controller;

import com.homework.web.app.dto.MembershipPaymentConfirmationDTO;
import com.homework.web.app.service.MembershipService;
import com.homework.web.app.service.payment.WechatNativePaymentGateway;
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
 * 微信支付异步通知入口(回调走这里)
 *
 * <p>该地址不接受登录 Token，也不相信前端。只有通过微信签名验证和 AES-GCM
 * 解密后的交易，才会交给 MembershipService 发放权益。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "payment.wechat", name = "enabled", havingValue = "true")
//负责接收微信发送过来的 HTTP 回调请求
public class WechatPaymentNotificationController {

    private final WechatNativePaymentGateway wechatGateway;
    private final MembershipService membershipService;

    //微信能够访问的回调接口，微信支付成功后，会主动向这个 URL 发送 HTTP 请求。
    //但 Controller 自己不解析和验证这些微信协议数据，而是交给 Gateway
    @PostMapping("/api/payment/wechat/native/notify")
    public ResponseEntity<Void> paymentNotification(
            @RequestHeader("Wechatpay-Serial") String serialNumber,
            @RequestHeader("Wechatpay-Nonce") String nonce,
            @RequestHeader("Wechatpay-Signature") String signature,
            @RequestHeader("Wechatpay-Timestamp") String timestamp,
            @RequestHeader("Wechatpay-Signature-Type") String signatureType,
            // 必须保留微信发送的原始字符串；重新序列化 JSON 会导致验签失败。
            @RequestBody String rawBody
    ) {
        try {
            // 验证这个通知确实来自微信(参数都是微信传过来的)
            MembershipPaymentConfirmationDTO confirmation = wechatGateway.parsePaymentNotification(serialNumber, nonce, signature, timestamp, signatureType, rawBody);

            //验证成功后，发给membershipService，正式更新订单和会员台账。
            membershipService.confirmPayment(confirmation);
            return ResponseEntity.ok().build();

            /*
              微信支付
                ↓ HTTP回调
            WechatPaymentNotificationController
                ↓ 交出微信请求头和原始请求体
            WechatNativePaymentGateway
                ↓ 验签、解密、检查、转换
            MembershipPaymentConfirmationDTO
                ↓
            MembershipService.confirmPayment()
                ↓
            完成订单并发放会员权益
             */

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
