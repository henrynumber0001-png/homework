package com.homework.web.app.vo;

import com.homework.model.enums.MembershipOrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MembershipOrderCreateVO {

    private String orderNo;

    private MembershipOrderStatus orderStatus;

    private BigDecimal amountDue;

    private String currency;

    private LocalDateTime paymentExpiredTime;

    /** 微信 Native 支付二维码内容。 */
    private String codeUrl;
}
