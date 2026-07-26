package com.homework.web.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** 创建会员套餐的请求。 */
@Data
public class MembershipPlanCreateDTO {

    @NotBlank
    private String membershipType;

    @NotBlank
    private String purchaseType;

    @NotNull
    private Integer durationMonths;

    private String billingType;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;

    @NotBlank
    @Size(max = 10)
    private String currency;

    @NotNull
    private Boolean enabled;

    @NotBlank
    @Size(max = 500)
    private String reason;
}
