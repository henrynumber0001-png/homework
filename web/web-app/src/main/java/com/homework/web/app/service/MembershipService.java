package com.homework.web.app.service;

import com.homework.model.enums.MembershipOrderStatus;
import com.homework.web.app.dto.MembershipOrderCreateDTO;
import com.homework.web.app.dto.MembershipPaymentConfirmationDTO;
import com.homework.web.app.vo.MembershipInfoVO;
import com.homework.web.app.vo.MembershipOrderCreateVO;
import com.homework.web.app.vo.MembershipOrderHistoryVO;
import com.homework.web.app.vo.MembershipDetailPageVO;
import java.util.List;

/** 会员查询、下单、变更和支付确认。 */
public interface MembershipService {

    MembershipDetailPageVO getMembershipDetailPage();

    MembershipOrderCreateVO createOrder(
            String idempotencyKey,
            MembershipOrderCreateDTO dto
    );

    List<MembershipOrderHistoryVO> getOrderHistory();

    MembershipOrderStatus getOrderStatus(String orderNo);

    void confirmPayment(MembershipPaymentConfirmationDTO confirmation);

    MembershipInfoVO getMembershipInfo();
}
