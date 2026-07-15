package com.homework.model.entity;

import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.PremiumStatus;
import lombok.Data;

@Data
public class PremiumUserInfo extends BaseEntity {

    private Long userId;

    private Long orderId;

    //active,disabled
    private PremiumStatus status;

}
