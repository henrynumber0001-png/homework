package com.homework.web.app.service.impl;

import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.enums.MembershipOrderStatus;
import com.homework.web.app.dto.MembershipOrderCreateDTO;
import com.homework.web.app.service.MembershipCheckoutService;
import com.homework.web.app.service.MembershipPaymentStateService;
import com.homework.web.app.service.MembershipService;
import com.homework.web.app.service.payment.PaymentGateway;
import com.homework.web.app.service.payment.PaymentGatewayRegistry;
import com.homework.web.app.service.payment.PaymentPrepayRequest;
import com.homework.web.app.service.payment.PaymentReconciliationResult;
import com.homework.web.app.service.payment.PaymentReconciliationStatus;
import com.homework.web.app.vo.MembershipOrderCreateVO;
import com.homework.web.app.vo.PaymentPayloadVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 将本地会员订单和外部支付预下单串联起来。
 *
 * <p>该类故意不加 @Transactional：membershipService.createOrder() 返回时本地事务已经提交，
 * 随后才访问微信网络，避免长事务和长时间数据库行锁。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipCheckoutServiceImpl implements MembershipCheckoutService {

    private final MembershipService membershipService;
    private final MembershipPaymentStateService paymentStateService;
    private final PaymentGatewayRegistry paymentGatewayRegistry;

    @Override
    public MembershipOrderCreateVO createOrder(
            String idempotencyKey,
            MembershipOrderCreateDTO dto
    ) {
        if (dto == null || dto.getPayType() == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        // 在创建本地订单之前确认渠道已启用；支付宝尚未接入时不会留下无效订单。
        PaymentGateway gateway = paymentGatewayRegistry.require(dto.getPayType());

        MembershipOrderCreateVO order =
                membershipService.createOrder(idempotencyKey, dto);
        if (order.getOrderStatus() != MembershipOrderStatus.PENDING
                || order.getAmountDue().signum() == 0
                || order.getPaymentPayload() != null) {
            return order;
        }

        try {
            PaymentPayloadVO payload = gateway.prepay(
                    new PaymentPrepayRequest(
                            order.getOrderNo(),
                            order.getAmountDue(),
                            order.getCurrency(),
                            order.getPaymentExpiredTime(),
                            null
                    )
            );
            String persistedCodeUrl = paymentStateService.recordWechatCodeUrl(
                    order.getOrderNo(),
                    payload.getCodeUrl()
            );
            if (!StringUtils.hasText(persistedCodeUrl)) {
                throw new IllegalStateException(
                        "Membership order left PENDING before code_url was saved"
                );
            }
            payload.setCodeUrl(persistedCodeUrl);
            order.setPaymentPayload(payload);
            return order;
        } catch (RuntimeException prepayFailure) {
            PaymentReconciliationStatus reconciledStatus =
                    reconcileAfterPrepayFailure(gateway, order.getOrderNo());
            if (reconciledStatus == PaymentReconciliationStatus.PAID) {
                // 预下单响应可能丢失，但主动查单已确认付款成功；不要向前端误报失败。
                order.setOrderStatus(MembershipOrderStatus.PAID);
                return order;
            }
            throw new HomeworkException(
                    ResultCodeEnum.MEMBERSHIP_PAYMENT_GATEWAY_ERROR,
                    prepayFailure
            );
        }
    }

    /**
     * 预下单网络失败时，订单在微信侧可能已经创建成功，不能直接改成 PAY_FAILED。
     * 先向微信查单；只有微信确认订单不存在/已关闭，才释放本地订单。
     */
    private PaymentReconciliationStatus reconcileAfterPrepayFailure(
            PaymentGateway gateway,
            String orderNo
    ) {
        try {
            PaymentReconciliationResult result =
                    gateway.reconcileExpiredOrder(orderNo);
            if (result.status() == PaymentReconciliationStatus.PAID) {
                membershipService.confirmPayment(result.confirmation());
            } else if (result.status() == PaymentReconciliationStatus.CLOSED) {
                paymentStateService.markPrepayFailed(orderNo);
            }
            return result.status();
        } catch (RuntimeException reconciliationFailure) {
            log.error(
                    "Failed to reconcile payment after prepay error, orderNo={}",
                    orderNo,
                    reconciliationFailure
            );
            return PaymentReconciliationStatus.PENDING;
        }
    }
}
