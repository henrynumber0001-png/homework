package com.homework.web.app.vo;

import com.homework.model.enums.BillingType;
import com.homework.model.enums.MembershipOrderAction;
import com.homework.model.enums.MembershipOrderStatus;
import com.homework.model.enums.MembershipType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MembershipOrderHistoryVO {

    private String orderNo;

    private MembershipOrderAction action;

    private MembershipType membershipType;

    private BillingType billingType;

    private Integer durationMonths;

    private BigDecimal payAmount;

    private String currency;

    private MembershipOrderStatus orderStatus;

    private LocalDateTime periodEnd;

    private LocalDateTime payTime;
}
