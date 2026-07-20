package com.homework.web.app.service.payment;

/**
 * 微信回调验签所需的原始 HTTP 信息。
 * body 必须是原始请求体，不能先反序列化再重新生成 JSON。
 */
public record WechatNotificationRequest(
        String serialNumber,
        String nonce,
        String signature,
        String timestamp,
        String signatureType,
        String body
) {
}
