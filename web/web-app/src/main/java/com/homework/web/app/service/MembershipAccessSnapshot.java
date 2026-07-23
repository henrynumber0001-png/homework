package com.homework.web.app.service;

import com.homework.model.enums.MembershipStatus;
import com.homework.model.enums.MembershipType;
import java.time.LocalDateTime;

/** 应用层最终判定的唯一生效会员身份。 */
public record MembershipAccessSnapshot(
        MembershipStatus status,
        MembershipType membershipType,
        LocalDateTime currentExpireTime,
        LocalDateTime baseFreezeExpireTime
) {
}
