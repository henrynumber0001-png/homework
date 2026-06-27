package com.homework.web.app.dto;

import lombok.Data;

@Data
public class PremiumOrderCreateDTO {
    private Long userId;
    private Long planId;
    /** 1.interview;2.certification */
    private Integer premiumScope;
    /** 1.monthly;2.yearly */
    private Integer type;
}
