package com.homework.web.app.vo;

import com.homework.model.enums.BillingType;
import com.homework.model.enums.MembershipPlanAction;
import com.homework.model.enums.MembershipType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MembershipPlanOptionVO {

    private Long planId;

    private MembershipType membershipType;

    private BillingType billingType;

    private BigDecimal price;

    private String currency;

    private MembershipPlanAction action;

    /** Unused current-period value credited only for an immediate upgrade. */
    private BigDecimal creditAmount;

    /** Amount shown on the purchase/upgrade button. */
    private BigDecimal amountDue;

    /** Null for immediate actions; period end for scheduled changes. */
    private LocalDateTime effectiveAt;

    /** Reason shown when this plan cannot be selected from the current plan. */
    private String unavailableReason;

    private Boolean interviewBanksEnabled;

    private Boolean certificateBanksEnabled;

    private Boolean aiEvaluationEnabled;

    private Boolean aiFollowUpEnabled;
}
