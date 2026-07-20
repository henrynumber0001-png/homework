package com.homework.web.app.service;

import com.homework.web.app.dto.MembershipOrderCreateDTO;
import com.homework.web.app.vo.MembershipOrderCreateVO;

/**
 * 会员结账门面：先完成本地会员建单事务，再调用外部支付平台预下单。
 */
public interface MembershipCheckoutService {

    MembershipOrderCreateVO createOrder(
            String idempotencyKey,
            MembershipOrderCreateDTO dto
    );
}
