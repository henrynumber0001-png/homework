package com.homework.web.app.service.impl;

import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.MembershipOrder;
import com.homework.model.entity.MembershipPlan;
import com.homework.model.entity.MembershipSubscription;
import com.homework.model.entity.UserInfo;
import com.homework.model.enums.BillingType;
import com.homework.model.enums.MembershipOrderAction;
import com.homework.model.enums.MembershipOrderPayType;
import com.homework.model.enums.MembershipOrderStatus;
import com.homework.model.enums.MembershipPlanAction;
import com.homework.model.enums.MembershipSubscriptionStatus;
import com.homework.model.enums.MembershipType;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.dto.MembershipOrderCreateDTO;
import com.homework.web.app.dto.MembershipPaymentConfirmationDTO;
import com.homework.web.app.dto.MembershipPlanChangeDTO;
import com.homework.web.app.mapper.MembershipOrderMapper;
import com.homework.web.app.mapper.MembershipPlanMapper;
import com.homework.web.app.mapper.MembershipSubscriptionMapper;
import com.homework.web.app.mapper.UserInfoMapper;
import com.homework.web.app.vo.MembershipOrderCreateVO;
import com.homework.web.app.vo.MembershipPageVO;
import com.homework.web.app.vo.MembershipPlanChangeVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipServiceImplTest {

    @Mock
    private MembershipPlanMapper membershipPlanMapper;
    @Mock
    private MembershipSubscriptionMapper membershipSubscriptionMapper;
    @Mock
    private MembershipOrderMapper membershipOrderMapper;
    @Mock
    private UserInfoMapper userInfoMapper;

    private MembershipServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MembershipServiceImpl(
                membershipPlanMapper,
                membershipSubscriptionMapper,
                membershipOrderMapper,
                userInfoMapper
        );
        LoginUserHolder.setUserId(7L);
    }

    @AfterEach
    void tearDown() {
        LoginUserHolder.removeUserId();
    }

    @Test
    void standardUpgradeUsesUnusedValueAsImmediateCredit() {
        LocalDateTime now = LocalDateTime.now();
        MembershipSubscription current = subscription(
                MembershipType.STANDARD,
                BillingType.MONTHLY,
                now.minusDays(15),
                now.plusDays(15),
                "49.00"
        );
        current.setPlanId(1L);
        MembershipPlan premium = plan(2L, MembershipType.PREMIUM, BillingType.MONTHLY, "79.00");

        when(userInfoMapper.selectOne(any())).thenReturn(new UserInfo());
        when(membershipOrderMapper.selectOne(any())).thenReturn(null);
        when(membershipOrderMapper.selectList(any())).thenReturn(List.of());
        when(membershipOrderMapper.selectCount(any())).thenReturn(0L);
        when(membershipPlanMapper.selectOne(any())).thenReturn(premium);
        when(membershipSubscriptionMapper.selectOne(any())).thenReturn(current);
        when(membershipOrderMapper.insert(any(MembershipOrder.class))).thenAnswer(invocation -> {
            MembershipOrder order = invocation.getArgument(0);
            order.setId(101L);
            return 1;
        });

        MembershipOrderCreateDTO dto = new MembershipOrderCreateDTO();
        dto.setPlanId(2L);
        dto.setPayType(MembershipOrderPayType.ALIPAY);
        MembershipOrderCreateVO result = service.createOrder("upgrade-7-001", dto);

        assertEquals(MembershipOrderAction.UPGRADE, result.getAction());
        assertTrue(result.getCreditAmount().compareTo(new BigDecimal("24.40")) >= 0);
        assertTrue(result.getCreditAmount().compareTo(new BigDecimal("24.60")) <= 0);
        assertTrue(result.getAmountDue().compareTo(new BigDecimal("54.40")) >= 0);
        assertTrue(result.getAmountDue().compareTo(new BigDecimal("54.60")) <= 0);
        assertEquals(101L, current.getPendingOrderId());
    }

    @Test
    void standardMonthlyCannotUpgradeToPremiumYearly() {
        LocalDateTime now = LocalDateTime.now();
        MembershipSubscription current = subscription(
                MembershipType.STANDARD,
                BillingType.MONTHLY,
                now.minusDays(15),
                now.plusDays(15),
                "49.00"
        );
        current.setPlanId(1L);
        MembershipPlan premiumYearly =
                plan(4L, MembershipType.PREMIUM, BillingType.YEARLY, "899.00");

        when(userInfoMapper.selectOne(any())).thenReturn(new UserInfo());
        when(membershipOrderMapper.selectOne(any())).thenReturn(null);
        when(membershipOrderMapper.selectList(any())).thenReturn(List.of());
        when(membershipOrderMapper.selectCount(any())).thenReturn(0L);
        when(membershipPlanMapper.selectOne(any())).thenReturn(premiumYearly);
        when(membershipSubscriptionMapper.selectOne(any())).thenReturn(current);

        MembershipOrderCreateDTO dto = new MembershipOrderCreateDTO();
        dto.setPlanId(4L);
        dto.setPayType(MembershipOrderPayType.ALIPAY);

        HomeworkException error = assertThrows(
                HomeworkException.class,
                () -> service.createOrder("upgrade-7-cross-billing", dto)
        );

        assertEquals(
                ResultCodeEnum.MEMBERSHIP_BILLING_TYPE_MISMATCH,
                error.getResultCodeEnum()
        );
    }

    @Test
    void membershipPageMarksCrossBillingPremiumPlanUnavailable() {
        LocalDateTime now = LocalDateTime.now();
        MembershipSubscription current = subscription(
                MembershipType.STANDARD,
                BillingType.MONTHLY,
                now.minusDays(15),
                now.plusDays(15),
                "49.00"
        );
        current.setPlanId(1L);
        MembershipPlan premiumMonthly =
                plan(3L, MembershipType.PREMIUM, BillingType.MONTHLY, "79.00");
        MembershipPlan premiumYearly =
                plan(4L, MembershipType.PREMIUM, BillingType.YEARLY, "899.00");

        when(membershipSubscriptionMapper.selectOne(any())).thenReturn(current);
        when(membershipPlanMapper.selectList(any()))
                .thenReturn(List.of(premiumMonthly, premiumYearly));

        MembershipPageVO page = service.getMembershipPage();

        assertEquals(MembershipPlanAction.UPGRADE, page.getPlans().get(0).getAction());
        assertEquals(MembershipPlanAction.UNAVAILABLE, page.getPlans().get(1).getAction());
        assertTrue(page.getPlans().get(1).getUnavailableReason().contains("Monthly"));
    }

    @Test
    void premiumDowngradeIsScheduledForCurrentPeriodEnd() {
        LocalDateTime periodEnd = LocalDateTime.now().plusDays(10);
        MembershipSubscription current = subscription(
                MembershipType.PREMIUM,
                BillingType.MONTHLY,
                LocalDateTime.now().minusDays(20),
                periodEnd,
                "79.00"
        );
        current.setPlanId(2L);
        MembershipPlan standard = plan(1L, MembershipType.STANDARD, BillingType.MONTHLY, "49.00");

        when(userInfoMapper.selectOne(any())).thenReturn(new UserInfo());
        when(membershipSubscriptionMapper.selectOne(any())).thenReturn(current);
        when(membershipPlanMapper.selectOne(any())).thenReturn(standard);

        MembershipPlanChangeDTO dto = new MembershipPlanChangeDTO();
        dto.setPlanId(1L);
        MembershipPlanChangeVO result = service.schedulePlanChange(dto);

        assertEquals(MembershipType.STANDARD, result.getMembershipType());
        assertEquals(periodEnd, result.getEffectiveAt());
        assertEquals(1L, current.getPendingPlanId());
        assertEquals(MembershipType.PREMIUM, current.getMembershipType());
    }

    @Test
    void verifiedPaymentActivatesPurchasedPlan() {
        LocalDateTime now = LocalDateTime.now();
        MembershipOrder order = new MembershipOrder();
        order.setId(201L);
        order.setUserId(7L);
        order.setOrderNo("order-201");
        order.setAction(MembershipOrderAction.PURCHASE);
        order.setToPlanId(1L);
        order.setPayAmount(new BigDecimal("49.00"));
        order.setCurrency("CNY");
        order.setPayType(MembershipOrderPayType.ALIPAY);
        order.setOrderStatus(MembershipOrderStatus.PENDING);
        order.setPaymentExpiredTime(now.plusMinutes(10));
        MembershipPlan standard = plan(1L, MembershipType.STANDARD, BillingType.MONTHLY, "49.00");

        when(membershipOrderMapper.selectOne(any())).thenReturn(order);
        when(userInfoMapper.selectOne(any())).thenReturn(new UserInfo());
        when(membershipPlanMapper.selectById(1L)).thenReturn(standard);
        when(membershipSubscriptionMapper.selectOne(any())).thenReturn(null);

        MembershipPaymentConfirmationDTO confirmation = new MembershipPaymentConfirmationDTO();
        confirmation.setOrderNo("order-201");
        confirmation.setProviderTradeNo("alipay-trade-201");
        confirmation.setPaidAmount(new BigDecimal("49.00"));
        confirmation.setCurrency("CNY");
        confirmation.setPayType(MembershipOrderPayType.ALIPAY);
        confirmation.setPaidTime(now);
        service.confirmPayment(confirmation);

        assertEquals(MembershipOrderStatus.PAID, order.getOrderStatus());
        ArgumentCaptor<MembershipSubscription> captor =
                ArgumentCaptor.forClass(MembershipSubscription.class);
        verify(membershipSubscriptionMapper).insert(captor.capture());
        MembershipSubscription activated = captor.getValue();
        assertEquals(MembershipType.STANDARD, activated.getMembershipType());
        assertEquals(MembershipSubscriptionStatus.ACTIVE, activated.getStatus());
        assertEquals(now.plusMonths(1), activated.getCurrentPeriodEnd());
    }

    @Test
    void paymentConfirmationRejectsCrossBillingUpgradeOrder() {
        LocalDateTime now = LocalDateTime.now();
        MembershipSubscription current = subscription(
                MembershipType.STANDARD,
                BillingType.MONTHLY,
                now.minusDays(15),
                now.plusDays(15),
                "49.00"
        );
        current.setPlanId(1L);
        current.setPendingOrderId(401L);

        MembershipOrder order = new MembershipOrder();
        order.setId(401L);
        order.setUserId(7L);
        order.setOrderNo("order-401");
        order.setAction(MembershipOrderAction.UPGRADE);
        order.setToPlanId(4L);
        order.setMembershipType(MembershipType.PREMIUM);
        order.setBillingType(BillingType.YEARLY);
        order.setPayAmount(new BigDecimal("874.50"));
        order.setCurrency("CNY");
        order.setPayType(MembershipOrderPayType.ALIPAY);
        order.setOrderStatus(MembershipOrderStatus.PENDING);
        order.setPaymentExpiredTime(now.plusMinutes(10));
        order.setSourceSubscriptionVersion(1L);
        MembershipPlan premiumYearly =
                plan(4L, MembershipType.PREMIUM, BillingType.YEARLY, "899.00");

        when(membershipOrderMapper.selectOne(any())).thenReturn(order);
        when(userInfoMapper.selectOne(any())).thenReturn(new UserInfo());
        when(membershipPlanMapper.selectById(4L)).thenReturn(premiumYearly);
        when(membershipSubscriptionMapper.selectOne(any())).thenReturn(current);

        MembershipPaymentConfirmationDTO confirmation = new MembershipPaymentConfirmationDTO();
        confirmation.setOrderNo("order-401");
        confirmation.setProviderTradeNo("alipay-trade-401");
        confirmation.setPaidAmount(new BigDecimal("874.50"));
        confirmation.setCurrency("CNY");
        confirmation.setPayType(MembershipOrderPayType.ALIPAY);
        confirmation.setPaidTime(now);

        HomeworkException error = assertThrows(
                HomeworkException.class,
                () -> service.confirmPayment(confirmation)
        );

        assertEquals(ResultCodeEnum.MEMBERSHIP_ORDER_STATE_ERROR, error.getResultCodeEnum());
    }

    @Test
    void renewalAppliesScheduledDowngradeAfterCurrentPeriod() {
        LocalDateTime now = LocalDateTime.now();
        MembershipSubscription current = subscription(
                MembershipType.PREMIUM,
                BillingType.MONTHLY,
                now.minusMonths(1),
                now.minusSeconds(1),
                "79.00"
        );
        current.setPlanId(2L);
        current.setPendingPlanId(1L);
        current.setPendingChangeTime(current.getCurrentPeriodEnd());
        MembershipPlan standard = plan(1L, MembershipType.STANDARD, BillingType.MONTHLY, "49.00");
        AtomicReference<MembershipOrder> createdOrder = new AtomicReference<>();

        when(userInfoMapper.selectOne(any())).thenReturn(new UserInfo());
        when(membershipOrderMapper.selectOne(any())).thenReturn(null);
        when(membershipOrderMapper.selectList(any())).thenReturn(List.of());
        when(membershipSubscriptionMapper.selectOne(any())).thenReturn(current);
        when(membershipPlanMapper.selectOne(any())).thenReturn(standard);
        when(membershipOrderMapper.insert(any(MembershipOrder.class))).thenAnswer(invocation -> {
            MembershipOrder order = invocation.getArgument(0);
            order.setId(301L);
            createdOrder.set(order);
            return 1;
        });

        MembershipOrderCreateVO renewal = service.createRenewalOrder(
                7L,
                "renewal-7-001",
                MembershipOrderPayType.ALIPAY
        );

        assertEquals(MembershipOrderAction.RENEWAL, renewal.getAction());
        assertEquals(301L, current.getPendingOrderId());
        assertEquals(MembershipType.PREMIUM, current.getMembershipType());

        MembershipOrder order = createdOrder.get();
        when(membershipOrderMapper.selectOne(any())).thenReturn(order);
        when(membershipPlanMapper.selectById(1L)).thenReturn(standard);
        MembershipPaymentConfirmationDTO confirmation = new MembershipPaymentConfirmationDTO();
        confirmation.setOrderNo(order.getOrderNo());
        confirmation.setProviderTradeNo("alipay-renewal-301");
        confirmation.setPaidAmount(new BigDecimal("49.00"));
        confirmation.setCurrency("CNY");
        confirmation.setPayType(MembershipOrderPayType.ALIPAY);
        confirmation.setPaidTime(now);
        service.confirmPayment(confirmation);

        assertEquals(MembershipType.STANDARD, current.getMembershipType());
        assertEquals(BillingType.MONTHLY, current.getBillingType());
        assertEquals(null, current.getPendingPlanId());
        assertEquals(now.plusMonths(1), current.getCurrentPeriodEnd());
    }

    private MembershipSubscription subscription(
            MembershipType type,
            BillingType billingType,
            LocalDateTime start,
            LocalDateTime end,
            String amount
    ) {
        MembershipSubscription subscription = new MembershipSubscription();
        subscription.setUserId(7L);
        subscription.setMembershipType(type);
        subscription.setBillingType(billingType);
        subscription.setStatus(MembershipSubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(start);
        subscription.setCurrentPeriodEnd(end);
        subscription.setCurrentPeriodAmount(new BigDecimal(amount));
        subscription.setSubscriptionVersion(1L);
        subscription.setAutoRenew(Boolean.TRUE);
        return subscription;
    }

    private MembershipPlan plan(
            Long id,
            MembershipType type,
            BillingType billingType,
            String price
    ) {
        MembershipPlan plan = new MembershipPlan();
        plan.setId(id);
        plan.setMembershipType(type);
        plan.setBillingType(billingType);
        plan.setPrice(new BigDecimal(price));
        plan.setCurrency("CNY");
        plan.setEnabled(true);
        return plan;
    }
}
