package com.homework.web.app.vo;

import com.homework.model.enums.BillingType;
import com.homework.model.enums.MembershipSubscriptionStatus;
import com.homework.model.enums.MembershipType;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MembershipSubscriptionVO {

    private Boolean active;

    private MembershipType membershipType;

    private BillingType billingType;

    private MembershipSubscriptionStatus status;

    private LocalDateTime currentPeriodStart;

    private LocalDateTime currentPeriodEnd;

    private Boolean autoRenew;

    private Boolean aiEvaluationEnabled; //AI评价

    private Boolean aiFollowUpEnabled; //AI追问

    private MembershipType pendingMembershipType;

    private BillingType pendingBillingType;

    private LocalDateTime pendingChangeTime;
}
