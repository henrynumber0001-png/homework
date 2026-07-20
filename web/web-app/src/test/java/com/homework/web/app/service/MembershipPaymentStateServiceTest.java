package com.homework.web.app.service;

import com.homework.model.entity.MembershipOrder;
import com.homework.model.entity.MembershipSubscription;
import com.homework.model.entity.UserInfo;
import com.homework.model.enums.MembershipOrderStatus;
import com.homework.web.app.mapper.MembershipOrderMapper;
import com.homework.web.app.mapper.MembershipSubscriptionMapper;
import com.homework.web.app.mapper.UserInfoMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipPaymentStateServiceTest {

    @Mock
    private MembershipOrderMapper membershipOrderMapper;
    @Mock
    private MembershipSubscriptionMapper membershipSubscriptionMapper;
    @Mock
    private UserInfoMapper userInfoMapper;

    @Test
    void expiredWechatOrderReleasesPendingSubscriptionOrder() {
        MembershipOrder order = new MembershipOrder();
        order.setId(88L);
        order.setUserId(7L);
        order.setOrderNo("order-88");
        order.setOrderStatus(MembershipOrderStatus.PENDING);

        MembershipSubscription subscription = new MembershipSubscription();
        subscription.setUserId(7L);
        subscription.setPendingOrderId(88L);

        when(membershipOrderMapper.selectOne(any()))
                .thenReturn(order, order);
        when(userInfoMapper.selectOne(any())).thenReturn(new UserInfo());
        when(membershipSubscriptionMapper.selectOne(any()))
                .thenReturn(subscription);

        MembershipPaymentStateService service =
                new MembershipPaymentStateService(
                        membershipOrderMapper,
                        membershipSubscriptionMapper,
                        userInfoMapper
                );
        service.markExpired("order-88");

        assertEquals(MembershipOrderStatus.EXPIRED, order.getOrderStatus());
        assertNull(subscription.getPendingOrderId());
        verify(membershipOrderMapper).updateById(order);
        verify(membershipSubscriptionMapper).updateById(subscription);
    }
}
