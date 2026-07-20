package com.homework.web.app.vo;

import com.homework.model.enums.MembershipOrderAction;
import com.homework.model.enums.MembershipOrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MembershipOrderCreateVO {

    private String orderNo;

    private MembershipOrderAction action;

    private MembershipOrderStatus orderStatus;

    private BigDecimal originalAmount;

    private BigDecimal creditAmount;

    private BigDecimal amountDue;

    private String currency;

    private LocalDateTime paymentExpiredTime;

    /**
     * 拉起收银台所需的服务端可信参数。
     * Native 微信支付返回 codeUrl，前端只需把它渲染成二维码。
     */
    private PaymentPayloadVO paymentPayload;
}
