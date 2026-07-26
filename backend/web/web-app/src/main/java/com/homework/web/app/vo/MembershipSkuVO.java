package com.homework.web.app.vo;

import com.homework.model.enums.BillingType;
import com.homework.model.enums.MembershipPurchaseType;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class MembershipSkuVO {

    private Long planId;

    private MembershipPurchaseType purchaseType;

    private BillingType billingType;

    private Integer durationMonths;

    private BigDecimal price;

    private String currency;

}
