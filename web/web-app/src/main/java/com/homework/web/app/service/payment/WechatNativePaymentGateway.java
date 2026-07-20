package com.homework.web.app.service.payment;

import com.homework.model.enums.MembershipOrderPayType;
import com.homework.web.app.config.WechatPayProperties;
import com.homework.web.app.dto.MembershipPaymentConfirmationDTO;
import com.homework.web.app.vo.PaymentPayloadVO;
import com.wechat.pay.java.core.RSAPublicKeyConfig;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.CloseOrderRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import com.wechat.pay.java.service.payments.nativepay.model.QueryOrderByOutTradeNoRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 微信支付 API v3 Native（电脑网页扫码）适配器。
 *
 * <p>官方 SDK 负责请求签名、微信响应验签、回调验签和 AES-GCM 解密。本类只负责
 * 把会员订单转换成微信模型，并验证回调中的商户、应用、币种和交易状态。
 */
@Slf4j
@Service
@ConditionalOnProperty(
        prefix = "payment.wechat",
        name = "enabled",
        havingValue = "true"
)
public class WechatNativePaymentGateway implements PaymentGateway {

    private static final DateTimeFormatter WECHAT_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final Set<String> ORDER_NOT_FOUND_CODES =
            Set.of("ORDER_NOT_EXIST", "ORDER_NOT_EXISTS");

    private final WechatPayProperties properties;
    private final ZoneId paymentZone;
    private final NativePayService nativePayService;
    private final NotificationParser notificationParser;

    public WechatNativePaymentGateway(WechatPayProperties properties) {
        this.properties = properties;
        validateConfiguration(properties);
        this.paymentZone = ZoneId.of(properties.getZoneId());

        // 微信支付公钥模式符合当前 API v3 接入方式；配置作为单例复用。
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

    @Override
    public MembershipOrderPayType payType() {
        return MembershipOrderPayType.WECHAT;
    }

    @Override
    public PaymentPayloadVO prepay(PaymentPrepayRequest paymentRequest) {
        requireCny(paymentRequest.currency());

        Amount amount = new Amount();
        amount.setTotal(toFen(paymentRequest.amount()));
        amount.setCurrency("CNY");

        PrepayRequest request = new PrepayRequest();
        request.setAppid(properties.getAppId());
        request.setMchid(properties.getMerchantId());
        request.setDescription(limitDescription(paymentRequest.description()));
        request.setOutTradeNo(paymentRequest.orderNo());
        request.setTimeExpire(toWechatTime(paymentRequest.expiresAt()));
        request.setNotifyUrl(properties.getNotifyUrl());
        request.setAmount(amount);

        PrepayResponse response = nativePayService.prepay(request);
        if (response == null || !StringUtils.hasText(response.getCodeUrl())) {
            throw new IllegalStateException("Wechat Native prepay returned an empty code_url");
        }
        return new PaymentPayloadVO(
                MembershipOrderPayType.WECHAT,
                "NATIVE",
                response.getCodeUrl()
        );
    }

    /**
     * 使用微信回调的原始 body 和 Wechatpay-* 请求头完成验签、解密。
     */
    public MembershipPaymentConfirmationDTO parsePaymentNotification(
            WechatNotificationRequest notification
    ) {
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(notification.serialNumber())
                .nonce(notification.nonce())
                .signature(notification.signature())
                .timestamp(notification.timestamp())
                .signType(notification.signatureType())
                .body(notification.body())
                .build();

        Transaction transaction =
                notificationParser.parse(requestParam, Transaction.class);
        return toPaidConfirmation(transaction);
    }

    @Override
    public PaymentReconciliationResult reconcileExpiredOrder(String orderNo) {
        QueryOrderByOutTradeNoRequest query = new QueryOrderByOutTradeNoRequest();
        query.setMchid(properties.getMerchantId());
        query.setOutTradeNo(orderNo);

        final Transaction transaction;
        try {
            transaction = nativePayService.queryOrderByOutTradeNo(query);
        } catch (ServiceException exception) {
            if (ORDER_NOT_FOUND_CODES.contains(exception.getErrorCode())) {
                // 微信侧从未创建成功，可以安全地结束本地订单。
                return PaymentReconciliationResult.closed();
            }
            log.warn(
                    "Failed to query Wechat order {}, code={}",
                    orderNo,
                    exception.getErrorCode()
            );
            return PaymentReconciliationResult.pending();
        } catch (RuntimeException exception) {
            // 网络或验签异常时保留 PENDING，绝不能把状态不明的订单误关。
            log.warn("Failed to query Wechat order {}", orderNo, exception);
            return PaymentReconciliationResult.pending();
        }

        validateMerchantIdentity(transaction);
        if (transaction.getTradeState() == null) {
            // 响应内容不完整时状态未知，保留 PENDING 等待下次对账。
            return PaymentReconciliationResult.pending();
        }
        if (transaction.getTradeState() == Transaction.TradeStateEnum.SUCCESS) {
            return PaymentReconciliationResult.paid(
                    toPaidConfirmation(transaction)
            );
        }
        if (transaction.getTradeState() == Transaction.TradeStateEnum.NOTPAY) {
            return closeUnpaidOrder(orderNo)
                    ? PaymentReconciliationResult.closed()
                    : PaymentReconciliationResult.pending();
        }
        if (transaction.getTradeState() == Transaction.TradeStateEnum.USERPAYING
                || transaction.getTradeState() == Transaction.TradeStateEnum.ACCEPT) {
            return PaymentReconciliationResult.pending();
        }
        // CLOSED、REVOKED、PAYERROR、REFUND 都不应继续保留本地待支付状态。
        return PaymentReconciliationResult.closed();
    }

    private boolean closeUnpaidOrder(String orderNo) {
        CloseOrderRequest request = new CloseOrderRequest();
        request.setMchid(properties.getMerchantId());
        request.setOutTradeNo(orderNo);
        try {
            nativePayService.closeOrder(request);
            return true;
        } catch (ServiceException exception) {
            if (ORDER_NOT_FOUND_CODES.contains(exception.getErrorCode())) {
                return true;
            }
            // ORDERPAID 等状态不能标记为本地 EXPIRED，等待下次查单或支付回调。
            log.warn(
                    "Failed to close Wechat order {}, code={}",
                    orderNo,
                    exception.getErrorCode()
            );
            return false;
        } catch (RuntimeException exception) {
            log.warn("Failed to close Wechat order {}", orderNo, exception);
            return false;
        }
    }

    private MembershipPaymentConfirmationDTO toPaidConfirmation(
            Transaction transaction
    ) {
        validateMerchantIdentity(transaction);
        if (transaction.getTradeState() != Transaction.TradeStateEnum.SUCCESS
                || !StringUtils.hasText(transaction.getTransactionId())
                || transaction.getAmount() == null
                || transaction.getAmount().getTotal() == null
                || !StringUtils.hasText(transaction.getSuccessTime())) {
            throw new IllegalArgumentException(
                    "Wechat notification is not a complete successful transaction"
            );
        }

        String currency = transaction.getAmount().getCurrency();
        requireCny(currency);

        MembershipPaymentConfirmationDTO confirmation =
                new MembershipPaymentConfirmationDTO();
        confirmation.setOrderNo(transaction.getOutTradeNo());
        confirmation.setProviderTradeNo(transaction.getTransactionId());
        confirmation.setPaidAmount(
                BigDecimal.valueOf(transaction.getAmount().getTotal(), 2)
        );
        confirmation.setCurrency(currency);
        confirmation.setPayType(MembershipOrderPayType.WECHAT);
        confirmation.setPaidTime(parseWechatTime(transaction.getSuccessTime()));
        return confirmation;
    }

    private void validateMerchantIdentity(Transaction transaction) {
        if (transaction == null
                || !properties.getAppId().equals(transaction.getAppid())
                || !properties.getMerchantId().equals(transaction.getMchid())
                || !StringUtils.hasText(transaction.getOutTradeNo())) {
            throw new IllegalArgumentException(
                    "Wechat transaction does not belong to this merchant application"
            );
        }
    }

    private int toFen(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Wechat payment amount must be positive");
        }
        return amount
                .setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2)
                .intValueExact();
    }

    private String toWechatTime(LocalDateTime time) {
        if (time == null) {
            throw new IllegalArgumentException("Wechat payment expiry time is required");
        }
        return time.atZone(paymentZone)
                .toOffsetDateTime()
                .format(WECHAT_TIME_FORMAT);
    }

    private LocalDateTime parseWechatTime(String value) {
        return OffsetDateTime.parse(value)
                .atZoneSameInstant(paymentZone)
                .toLocalDateTime();
    }

    private void requireCny(String currency) {
        if (!"CNY".equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException(
                    "Wechat Native payment currently supports CNY membership orders only"
            );
        }
    }

    private String limitDescription(String description) {
        String value = StringUtils.hasText(description)
                ? description.trim()
                : properties.getProductDescription();
        return value.length() <= 127 ? value : value.substring(0, 127);
    }

    private static void validateConfiguration(WechatPayProperties properties) {
        if (!StringUtils.hasText(properties.getAppId())
                || !StringUtils.hasText(properties.getMerchantId())
                || !StringUtils.hasText(properties.getMerchantSerialNumber())
                || !StringUtils.hasText(properties.getMerchantPrivateKeyPath())
                || !StringUtils.hasText(properties.getWechatPayPublicKeyId())
                || !StringUtils.hasText(properties.getWechatPayPublicKeyPath())
                || !StringUtils.hasText(properties.getApiV3Key())
                || !StringUtils.hasText(properties.getNotifyUrl())
                || !StringUtils.hasText(properties.getZoneId())) {
            throw new IllegalStateException(
                    "Wechat payment is enabled but API v3 configuration is incomplete"
            );
        }
    }
}
