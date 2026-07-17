package com.homework.web.app.vo;

import com.homework.model.enums.PremiumOrderScope;
import com.homework.model.enums.PremiumStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PremiumUserInfoVO {
//    private PremiumOrderScope premiumScope;


    private PremiumStatus status;

    private LocalDateTime startTime;
    private LocalDateTime expiredTime;
}
