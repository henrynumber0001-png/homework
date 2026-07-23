package com.homework.web.app.service;

import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.BaseVipRecord;
import com.homework.model.entity.MembershipOrder;
import com.homework.model.entity.MembershipPlan;
import com.homework.model.entity.SvipRecord;
import com.homework.model.enums.BillingType;
import com.homework.model.enums.MembershipOrderAction;
import com.homework.model.enums.MembershipOrderStatus;
import com.homework.model.enums.MembershipPurchaseType;
import com.homework.model.enums.MembershipStatus;
import com.homework.model.enums.MembershipType;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.dto.MembershipOrderCreateDTO;
import com.homework.web.app.dto.MembershipPaymentConfirmationDTO;
import com.homework.web.app.mapper.BaseVipRecordMapper;
import com.homework.web.app.mapper.MembershipOrderMapper;
import com.homework.web.app.mapper.MembershipPlanMapper;
import com.homework.web.app.mapper.SvipRecordMapper;
import com.homework.web.app.service.impl.MembershipServiceImpl;
import com.homework.web.app.vo.MembershipOrderCreateVO;
import com.homework.web.app.vo.MembershipDetailPageVO;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    @Mock
    private MembershipPlanMapper planMapper;
    @Mock
    private BaseVipRecordMapper baseVipMapper;
    @Mock
    private SvipRecordMapper svipMapper;
    @Mock
    private MembershipOrderMapper orderMapper;

    private MembershipService service;

    @BeforeEach
    void setUp() {
        service = new MembershipServiceImpl(
                planMapper,
                baseVipMapper,
                svipMapper,
                orderMapper
        );
        LoginUserHolder.setUserId(7L);
    }

    @AfterEach
    void tearDown() {
        LoginUserHolder.removeUserId();
    }

    @Test
    void membershipPageUsesSvipFirstAndOffersOnlyAffordableDiffMonths() {
        LocalDateTime now = LocalDateTime.now();
        SvipRecord svip = svip(now.plusDays(10));
        BaseVipRecord baseVip = baseVip(now.plusDays(80));
        when(svipMapper.selectOne(any())).thenReturn(svip);
        when(baseVipMapper.selectOne(any())).thenReturn(baseVip);
        when(planMapper.selectList(any())).thenReturn(List.of(
                fullPlan(1L, MembershipType.PREMIUM, BillingType.MONTHLY, 1, "99.00"),
                fullPlan(2L, MembershipType.PREMIUM_PLUS, BillingType.MONTHLY, 1, "129.00"),
                diffPlan(10L, 1),
                diffPlan(11L, 2),
                diffPlan(12L, 3)
        ));

        MembershipDetailPageVO page = service.getMembershipPage();

        assertEquals(MembershipStatus.PREMIUM_PLUS, page.getMemberStatus());
        assertEquals(svip.getExpireTime(), page.getCurrentExpireTime());
        assertEquals(baseVip.getExpireTime(), page.getBaseFreezeExpireTime());
        assertEquals(2, page.getMaxDiffUpgradeMonths());
        assertEquals(2, page.getDiffUpgradeOptions().size());
        assertEquals(
                1,
                page.getFullPurchaseCards().get(0).getFullPurchaseOptions().size()
        );
        assertEquals(
                1,
                page.getFullPurchaseCards().get(1).getFullPurchaseOptions().size()
        );
    }

    @Test
    void membershipPageHidesDiffUpgradeWithoutThirtyOneFullDays() {
        when(svipMapper.selectOne(any())).thenReturn(null);
        when(baseVipMapper.selectOne(any())).thenReturn(
                baseVip(LocalDateTime.now().plusDays(30))
        );
        when(planMapper.selectList(any())).thenReturn(List.of(diffPlan(10L, 1)));

        MembershipDetailPageVO page = service.getMembershipPage();

        assertEquals(MembershipStatus.PREMIUM, page.getMemberStatus());
        assertFalse(page.isDiffUpgradeAvailable());
        assertTrue(page.getDiffUpgradeOptions().isEmpty());
    }

    @Test
    void createFullPurchaseOrderUsesServerPlanPrice() {
        when(orderMapper.selectOne(any())).thenReturn(null);
        when(orderMapper.selectList(any())).thenReturn(List.of());
        when(orderMapper.selectCount(any())).thenReturn(0L);
        when(planMapper.selectOne(any())).thenReturn(
                fullPlan(2L, MembershipType.PREMIUM_PLUS, BillingType.QUARTERLY, 3, "349.00")
        );

        MembershipOrderCreateVO result = service.createOrder("full-1", dto(2L));

        ArgumentCaptor<MembershipOrder> captor = ArgumentCaptor.forClass(MembershipOrder.class);
        verify(orderMapper).insert(captor.capture());
        MembershipOrder order = captor.getValue();
        assertEquals(MembershipOrderAction.FULL_PURCHASE, order.getAction());
        assertEquals(MembershipType.PREMIUM_PLUS, order.getMembershipType());
        assertEquals(3, order.getDurationMonths());
        assertEquals(new BigDecimal("349.00"), result.getAmountDue());
    }

    @Test
    void createDiffUpgradeRejectsDurationBeyondPaidBaseBalance() {
        LocalDateTime now = LocalDateTime.now();
        when(orderMapper.selectOne(any())).thenReturn(null);
        when(orderMapper.selectList(any())).thenReturn(List.of());
        when(orderMapper.selectCount(any())).thenReturn(0L);
        when(planMapper.selectOne(any())).thenReturn(diffPlan(12L, 3));
        when(baseVipMapper.selectOne(any())).thenReturn(baseVip(now.plusDays(92)));
        when(svipMapper.selectOne(any())).thenReturn(null);

        HomeworkException error = assertThrows(
                HomeworkException.class,
                () -> service.createOrder("diff-too-long", dto(12L))
        );

        assertEquals(
                ResultCodeEnum.MEMBERSHIP_DIFF_UPGRADE_UNAVAILABLE,
                error.getResultCodeEnum()
        );
        verify(orderMapper, never()).insert(any(MembershipOrder.class));
    }

    @Test
    void premiumPurchaseDuringSvipStartsAfterSvip() {
        LocalDateTime paidTime = LocalDateTime.of(2030, 1, 10, 12, 0);
        MembershipOrder order = pendingOrder(
                MembershipOrderAction.FULL_PURCHASE,
                MembershipType.PREMIUM,
                BillingType.MONTHLY,
                1,
                "99.00",
                paidTime
        );
        SvipRecord svip = svip(paidTime.plusDays(20));
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(baseVipMapper.selectOne(any())).thenReturn(null);
        when(svipMapper.selectOne(any())).thenReturn(svip);

        service.confirmPayment(confirmation(order, paidTime));

        ArgumentCaptor<BaseVipRecord> captor = ArgumentCaptor.forClass(BaseVipRecord.class);
        verify(baseVipMapper).insert(captor.capture());
        assertEquals(svip.getExpireTime().plusMonths(1), captor.getValue().getExpireTime());
        assertEquals(MembershipOrderStatus.PAID, order.getOrderStatus());
    }

    @Test
    void premiumRenewalStacksFromCurrentBaseExpire() {
        LocalDateTime paidTime = LocalDateTime.of(2030, 1, 10, 12, 0);
        MembershipOrder order = pendingOrder(
                MembershipOrderAction.FULL_PURCHASE,
                MembershipType.PREMIUM,
                BillingType.MONTHLY,
                1,
                "99.00",
                paidTime
        );
        BaseVipRecord baseVip = baseVip(paidTime.plusDays(40));
        baseVip.setId(1L);
        LocalDateTime originalBaseExpire = baseVip.getExpireTime();
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(baseVipMapper.selectOne(any())).thenReturn(baseVip);
        when(svipMapper.selectOne(any())).thenReturn(null);

        service.confirmPayment(confirmation(order, paidTime));

        assertEquals(originalBaseExpire.plusMonths(1), baseVip.getExpireTime());
        verify(baseVipMapper).updateById(baseVip);
    }

    @Test
    void fullSvipPurchaseFreezesBaseForExactlyTheAddedSvipDuration() {
        LocalDateTime paidTime = LocalDateTime.of(2030, 1, 10, 12, 0);
        MembershipOrder order = pendingOrder(
                MembershipOrderAction.FULL_PURCHASE,
                MembershipType.PREMIUM_PLUS,
                BillingType.MONTHLY,
                1,
                "129.00",
                paidTime
        );
        LocalDateTime originalBaseExpire = paidTime.plusDays(100);
        BaseVipRecord baseVip = baseVip(originalBaseExpire);
        baseVip.setId(1L);
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(baseVipMapper.selectOne(any())).thenReturn(baseVip);
        when(svipMapper.selectOne(any())).thenReturn(null);

        service.confirmPayment(confirmation(order, paidTime));

        LocalDateTime svipExpire = paidTime.plusMonths(1);
        Duration addedSvipDuration = Duration.between(paidTime, svipExpire);
        assertEquals(originalBaseExpire.plus(addedSvipDuration), baseVip.getExpireTime());
        ArgumentCaptor<SvipRecord> captor = ArgumentCaptor.forClass(SvipRecord.class);
        verify(svipMapper).insert(captor.capture());
        assertEquals(svipExpire, captor.getValue().getExpireTime());
    }

    @Test
    void svipRenewalStacksAndKeepsBaseDurationFrozen() {
        LocalDateTime paidTime = LocalDateTime.of(2030, 1, 10, 12, 0);
        MembershipOrder order = pendingOrder(
                MembershipOrderAction.FULL_PURCHASE,
                MembershipType.PREMIUM_PLUS,
                BillingType.QUARTERLY,
                3,
                "349.00",
                paidTime
        );
        SvipRecord svip = svip(paidTime.plusDays(20));
        svip.setId(2L);
        BaseVipRecord baseVip = baseVip(svip.getExpireTime().plusDays(50));
        baseVip.setId(1L);
        LocalDateTime originalSvipExpire = svip.getExpireTime();
        LocalDateTime originalBaseExpire = baseVip.getExpireTime();
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(baseVipMapper.selectOne(any())).thenReturn(baseVip);
        when(svipMapper.selectOne(any())).thenReturn(svip);

        service.confirmPayment(confirmation(order, paidTime));

        LocalDateTime newSvipExpire = originalSvipExpire.plusMonths(3);
        Duration addedDuration = Duration.between(originalSvipExpire, newSvipExpire);
        assertEquals(newSvipExpire, svip.getExpireTime());
        assertEquals(originalBaseExpire.plus(addedDuration), baseVip.getExpireTime());
        verify(svipMapper).updateById(svip);
        verify(baseVipMapper).updateById(baseVip);
    }

    @Test
    void diffUpgradeMovesTimeToSvipWithoutChangingBaseFinalExpire() {
        LocalDateTime paidTime = LocalDateTime.of(2030, 1, 10, 12, 0);
        MembershipOrder order = pendingOrder(
                MembershipOrderAction.DIFF_UPGRADE,
                MembershipType.PREMIUM_PLUS,
                null,
                2,
                "60.00",
                paidTime
        );
        BaseVipRecord baseVip = baseVip(paidTime.plusDays(100));
        baseVip.setId(1L);
        LocalDateTime originalBaseExpire = baseVip.getExpireTime();
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(baseVipMapper.selectOne(any())).thenReturn(baseVip);
        when(svipMapper.selectOne(any())).thenReturn(null);

        service.confirmPayment(confirmation(order, paidTime));

        assertEquals(originalBaseExpire, baseVip.getExpireTime());
        verify(baseVipMapper, never()).updateById(any(BaseVipRecord.class));
        ArgumentCaptor<SvipRecord> captor = ArgumentCaptor.forClass(SvipRecord.class);
        verify(svipMapper).insert(captor.capture());
        assertEquals(paidTime.plusDays(62), captor.getValue().getExpireTime());
        assertNull(order.getBillingType());
    }

    @Test
    void repeatedPaymentNotificationDoesNotGrantTwice() {
        LocalDateTime paidTime = LocalDateTime.of(2030, 1, 10, 12, 0);
        MembershipOrder order = pendingOrder(
                MembershipOrderAction.FULL_PURCHASE,
                MembershipType.PREMIUM_PLUS,
                BillingType.MONTHLY,
                1,
                "129.00",
                paidTime
        );
        order.setOrderStatus(MembershipOrderStatus.PAID);
        when(orderMapper.selectOne(any())).thenReturn(order);

        service.confirmPayment(confirmation(order, paidTime));

        verify(baseVipMapper, never()).selectOne(any());
        verify(svipMapper, never()).selectOne(any());
        verify(orderMapper, never()).updateById(any(MembershipOrder.class));
    }

    @Test
    void verifiedPaymentCanRecoverAProvisionallyExpiredOrder() {
        LocalDateTime paidTime = LocalDateTime.of(2030, 1, 10, 12, 0);
        MembershipOrder order = pendingOrder(
                MembershipOrderAction.FULL_PURCHASE,
                MembershipType.PREMIUM,
                BillingType.MONTHLY,
                1,
                "99.00",
                paidTime
        );
        order.setOrderStatus(MembershipOrderStatus.EXPIRED);
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(baseVipMapper.selectOne(any())).thenReturn(null);
        when(svipMapper.selectOne(any())).thenReturn(null);

        service.confirmPayment(confirmation(order, paidTime));

        assertEquals(MembershipOrderStatus.PAID, order.getOrderStatus());
        verify(baseVipMapper).insert(any(BaseVipRecord.class));
    }

    @Test
    void paymentAmountMustMatchOrder() {
        LocalDateTime paidTime = LocalDateTime.of(2030, 1, 10, 12, 0);
        MembershipOrder order = pendingOrder(
                MembershipOrderAction.FULL_PURCHASE,
                MembershipType.PREMIUM_PLUS,
                BillingType.MONTHLY,
                1,
                "129.00",
                paidTime
        );
        MembershipPaymentConfirmationDTO confirmation = confirmation(order, paidTime);
        confirmation.setPaidAmount(new BigDecimal("1.00"));
        when(orderMapper.selectOne(any())).thenReturn(order);

        HomeworkException error = assertThrows(
                HomeworkException.class,
                () -> service.confirmPayment(confirmation)
        );

        assertEquals(
                ResultCodeEnum.MEMBERSHIP_PAYMENT_MISMATCH,
                error.getResultCodeEnum()
        );
        verify(baseVipMapper, never()).selectOne(any());
        verify(svipMapper, never()).selectOne(any());
    }

    private MembershipOrderCreateDTO dto(Long planId) {
        MembershipOrderCreateDTO dto = new MembershipOrderCreateDTO();
        dto.setPlanId(planId);
        return dto;
    }

    private MembershipPlan fullPlan(
            Long id,
            MembershipType membershipType,
            BillingType billingType,
            int durationMonths,
            String price
    ) {
        MembershipPlan plan = new MembershipPlan();
        plan.setId(id);
        plan.setMembershipType(membershipType);
        plan.setPurchaseType(MembershipPurchaseType.FULL);
        plan.setBillingType(billingType);
        plan.setDurationMonths(durationMonths);
        plan.setPrice(new BigDecimal(price));
        plan.setCurrency("CNY");
        plan.setEnabled(true);
        return plan;
    }

    private MembershipPlan diffPlan(Long id, int durationMonths) {
        MembershipPlan plan = new MembershipPlan();
        plan.setId(id);
        plan.setMembershipType(MembershipType.PREMIUM_PLUS);
        plan.setPurchaseType(MembershipPurchaseType.DIFF);
        plan.setDurationMonths(durationMonths);
        plan.setPrice(BigDecimal.valueOf(30L * durationMonths).setScale(2));
        plan.setCurrency("CNY");
        plan.setEnabled(true);
        return plan;
    }

    private BaseVipRecord baseVip(LocalDateTime expireTime) {
        BaseVipRecord record = new BaseVipRecord();
        record.setUserId(7L);
        record.setExpireTime(expireTime);
        return record;
    }

    private SvipRecord svip(LocalDateTime expireTime) {
        SvipRecord record = new SvipRecord();
        record.setUserId(7L);
        record.setExpireTime(expireTime);
        return record;
    }

    private MembershipOrder pendingOrder(
            MembershipOrderAction action,
            MembershipType membershipType,
            BillingType billingType,
            int durationMonths,
            String amount,
            LocalDateTime paidTime
    ) {
        MembershipOrder order = new MembershipOrder();
        order.setId(201L);
        order.setUserId(7L);
        order.setOrderNo("order-201");
        order.setAction(action);
        order.setMembershipType(membershipType);
        order.setBillingType(billingType);
        order.setDurationMonths(durationMonths);
        order.setPayAmount(new BigDecimal(amount));
        order.setCurrency("CNY");
        order.setOrderStatus(MembershipOrderStatus.PENDING);
        order.setPaymentExpiredTime(paidTime.plusMinutes(15));
        return order;
    }

    private MembershipPaymentConfirmationDTO confirmation(
            MembershipOrder order,
            LocalDateTime paidTime
    ) {
        MembershipPaymentConfirmationDTO confirmation =
                new MembershipPaymentConfirmationDTO();
        confirmation.setOrderNo(order.getOrderNo());
        confirmation.setProviderTradeNo("wechat-" + order.getOrderNo());
        confirmation.setPaidAmount(order.getPayAmount());
        confirmation.setCurrency(order.getCurrency());
        confirmation.setPaidTime(paidTime);
        return confirmation;
    }
}
