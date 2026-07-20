package com.homework.web.app.service;

import com.homework.web.app.dto.MembershipOrderCreateDTO;
import com.homework.web.app.dto.MembershipPaymentConfirmationDTO;
import com.homework.web.app.dto.MembershipPlanChangeDTO;
import com.homework.web.app.vo.MembershipOrderCreateVO;
import com.homework.web.app.vo.MembershipOrderHistoryVO;
import com.homework.web.app.vo.MembershipOrderStatusVO;
import com.homework.web.app.vo.MembershipPageVO;
import com.homework.web.app.vo.MembershipPlanChangeVO;
import com.homework.web.app.vo.MembershipSubscriptionVO;
import com.homework.model.enums.MembershipOrderPayType;
import java.util.List;

public interface MembershipService {

    MembershipPageVO getMembershipPage();

    MembershipSubscriptionVO getCurrentSubscription(Long userId);

    MembershipOrderCreateVO createOrder(String idempotencyKey, MembershipOrderCreateDTO dto);

    /**
     * Internal renewal entry used by a recurring-payment adapter after the
     * current period ends. A scheduled downgrade/billing switch is selected here.
     */
    MembershipOrderCreateVO createRenewalOrder(
            Long userId,
            String idempotencyKey,
            MembershipOrderPayType payType
    );

    MembershipPlanChangeVO schedulePlanChange(MembershipPlanChangeDTO dto);

    void cancelScheduledPlanChange();

    List<MembershipOrderHistoryVO> getOrderHistory();

    MembershipOrderStatusVO getOrderStatus(String orderNo);

    /**
     * Payment-provider adapters call this only after signature verification.
     * This method must never be wired to an unauthenticated client callback.
     */
    void confirmPayment(MembershipPaymentConfirmationDTO confirmation);
}
