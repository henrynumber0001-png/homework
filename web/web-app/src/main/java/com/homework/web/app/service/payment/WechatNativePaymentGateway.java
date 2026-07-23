package com.homework.web.app.service.payment;

import com.homework.web.app.config.WechatPayProperties;
import com.homework.web.app.dto.MembershipPaymentConfirmationDTO;
import com.wechat.pay.java.core.RSAPublicKeyConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 微信支付 API v3 Native（电脑网页扫码）实现。 */
@Service
@ConditionalOnProperty(
        prefix = "payment.wechat",
        name = "enabled",
        havingValue = "true"
)
public class WechatNativePaymentGateway {

    private static final DateTimeFormatter WECHAT_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final WechatPayProperties properties;
    private final ZoneId paymentZone;
    private final NativePayService nativePayService;
    private final NotificationParser notificationParser;

    public WechatNativePaymentGateway(WechatPayProperties properties) {
        if (!StringUtils.hasText(properties.getAppId())
                || !StringUtils.hasText(properties.getMerchantId())
                || !StringUtils.hasText(properties.getMerchantSerialNumber())
                || !StringUtils.hasText(properties.getMerchantPrivateKeyPath())
                || !StringUtils.hasText(properties.getWechatPayPublicKeyId())
                || !StringUtils.hasText(properties.getWechatPayPublicKeyPath())
                || !StringUtils.hasText(properties.getApiV3Key())
                || !StringUtils.hasText(properties.getNotifyUrl())) {
            throw new IllegalStateException("Wechat payment configuration is incomplete");
        }

        this.properties = properties;
        this.paymentZone = ZoneId.of(properties.getZoneId());
        RSAPublicKeyConfig config = new RSAPublicKeyConfig.Builder()
                .merchantId(properties.getMerchantId())
                .privateKeyFromPath(properties.getMerchantPrivateKeyPath())
                .merchantSerialNumber(properties.getMerchantSerialNumber())
                .publicKeyId(properties.getWechatPayPublicKeyId())
                .publicKeyFromPath(properties.getWechatPayPublicKeyPath())
                .apiV3Key(properties.getApiV3Key())
                .build();
        this.nativePayService = new NativePayService.Builder()
                .config(config)
                .build();
        this.notificationParser = new NotificationParser(config);
    }

    /** 调用微信预下单接口，返回前端生成二维码所需的 code_url。 */
    public String prepay(
            String orderNo,
            BigDecimal amountDue,
            String currency,
            LocalDateTime paymentExpiredTime
    ) {
        if (!StringUtils.hasText(orderNo)
                || amountDue == null
                || amountDue.signum() <= 0
                || paymentExpiredTime == null
                || !"CNY".equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("Invalid Wechat prepay request");
        }

        Amount amount = new Amount();
        amount.setTotal(amountDue
                .setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2)
                .intValueExact());
        amount.setCurrency("CNY");

        String description = properties.getProductDescription().trim();
        PrepayRequest request = new PrepayRequest();
        request.setAppid(properties.getAppId());
        request.setMchid(properties.getMerchantId());
        request.setDescription(description.length() <= 127
                ? description
                : description.substring(0, 127));
        request.setOutTradeNo(orderNo);
        request.setTimeExpire(paymentExpiredTime
                .atZone(paymentZone)
                .toOffsetDateTime()
                .format(WECHAT_TIME_FORMAT));
        request.setNotifyUrl(properties.getNotifyUrl());
        request.setAmount(amount);

        PrepayResponse response = nativePayService.prepay(request);
        if (response == null || !StringUtils.hasText(response.getCodeUrl())) {
            throw new IllegalStateException("Wechat returned an empty code_url");
        }
        return response.getCodeUrl();
    }

    /** 验签并解密微信支付通知，输出会员服务可以信任的支付结果。 */
    public MembershipPaymentConfirmationDTO parsePaymentNotification(
            String serialNumber,
            String nonce,
            String signature,
            String timestamp,
            String signatureType,
            String rawBody
    ) {
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(serialNumber)
                .nonce(nonce)
                .signature(signature)
                .timestamp(timestamp)
                .signType(signatureType)
                .body(rawBody)
                .build();
        Transaction transaction = notificationParser.parse(
                requestParam,
                Transaction.class
        );

        if (transaction == null
                || !properties.getAppId().equals(transaction.getAppid())
                || !properties.getMerchantId().equals(transaction.getMchid())
                || transaction.getTradeState() != Transaction.TradeStateEnum.SUCCESS
                || !StringUtils.hasText(transaction.getOutTradeNo())
                || !StringUtils.hasText(transaction.getTransactionId())
                || transaction.getAmount() == null
                || transaction.getAmount().getTotal() == null
                || !"CNY".equalsIgnoreCase(transaction.getAmount().getCurrency())
                || !StringUtils.hasText(transaction.getSuccessTime())) {
            throw new IllegalArgumentException("Invalid Wechat payment notification");
        }

        MembershipPaymentConfirmationDTO confirmation =
                new MembershipPaymentConfirmationDTO();
        confirmation.setOrderNo(transaction.getOutTradeNo());
        confirmation.setProviderTradeNo(transaction.getTransactionId());
        confirmation.setPaidAmount(
                BigDecimal.valueOf(transaction.getAmount().getTotal(), 2)
        );
        confirmation.setCurrency(transaction.getAmount().getCurrency());
        confirmation.setPaidTime(
                OffsetDateTime.parse(transaction.getSuccessTime())
                        .atZoneSameInstant(paymentZone)
                        .toLocalDateTime()
        );
        return confirmation;
    }
}
