package com.homework.web.app.vo;

import com.homework.model.enums.BillingType;
import com.homework.model.enums.MembershipType;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MembershipPlanChangeVO {

    private MembershipType membershipType;

    private BillingType billingType;

    private LocalDateTime effectiveAt;
}
