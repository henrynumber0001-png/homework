package com.homework.web.app.service.payment;

import com.homework.web.app.service.MembershipPaymentStateService;
import com.homework.web.app.service.MembershipService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 微信支付兜底对账任务。
 *
 * <p>回调可能因网络暂时延迟，因此到达本地截止时间后不能直接改为 EXPIRED：
 * 先向微信查单；SUCCESS 补发权益，NOTPAY 关单后才标记本地过期。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "payment.wechat",
        name = "enabled",
        havingValue = "true"
)
public class WechatPendingOrderReconciliationJob {

    private final WechatNativePaymentGateway wechatGateway;
    private final MembershipPaymentStateService paymentStateService;
    private final MembershipService membershipService;

    @Scheduled(
            fixedDelayString =
                    "${payment.wechat.reconciliation-delay-ms:30000}"
    )
    public void reconcileExpiredOrders() {
        List<String> orderNumbers =
                paymentStateService.findExpiredPendingWechatOrderNumbers(
                        LocalDateTime.now()
                );
        for (String orderNo : orderNumbers) {
            try {
                PaymentReconciliationResult result =
                        wechatGateway.reconcileExpiredOrder(orderNo);
                if (result.status() == PaymentReconciliationStatus.PAID) {
                    membershipService.confirmPayment(result.confirmation());
                } else if (result.status()
                        == PaymentReconciliationStatus.CLOSED) {
                    paymentStateService.markExpired(orderNo);
                }
            } catch (RuntimeException exception) {
                // 单笔异常不能阻断整个批次；保留 PENDING，下一轮继续核对。
                log.error(
                        "Failed to reconcile expired Wechat order {}",
                        orderNo,
                        exception
                );
            }
        }
    }
}
