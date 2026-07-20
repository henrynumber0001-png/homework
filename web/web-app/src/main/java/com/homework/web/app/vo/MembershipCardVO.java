package com.homework.web.app.vo;

import com.homework.model.enums.BillingType;
import com.homework.model.enums.MembershipSubscriptionStatus;
import com.homework.model.enums.MembershipType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MembershipCardVO {
    private Boolean active;

    private MembershipType membershipType;

    private BillingType billingType;

    private MembershipSubscriptionStatus status;

    private LocalDateTime currentPeriodStart;

    private LocalDateTime currentPeriodEnd;
}
