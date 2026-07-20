package com.homework.web.app.service;

import com.homework.model.enums.BillingType;
import com.homework.model.enums.MembershipType;
import java.time.LocalDateTime;

public record MembershipAccessSnapshot(
        boolean active,
        MembershipType membershipType,
        BillingType billingType,
        LocalDateTime currentPeriodEnd
) {
    public boolean premium() {
        return active && membershipType == MembershipType.PREMIUM;
    }
}
