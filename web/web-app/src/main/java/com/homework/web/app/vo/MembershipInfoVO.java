package com.homework.web.app.vo;

import com.homework.model.enums.MembershipType;
import com.homework.model.enums.MembershipStatus;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MembershipInfoVO {

    private String displayName;

    private String avatarUrl;

    private MembershipType membershipType;

    private LocalDateTime expiredTime;

    private MembershipStatus memberStatus;

    private LocalDateTime baseFreezeExpireTime;

}
