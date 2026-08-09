package com.homework.web.app.vo;

import com.homework.model.enums.MembershipType;
import lombok.Data;

@Data
public class UserCenterPageVO {

    private UserInfoVO userInfoVO;

    private boolean membershipActive;

    private MembershipType membershipType;

    private boolean aiFeaturesEnabled;

    private UserCenterCountsVO countsVO;
}
