package com.homework.web.app.vo;

import com.homework.model.enums.MembershipType;
import lombok.Data;

@Data
public class UserCenterPageVO {

    //返回Banner的图片和图片上的文字
    private GraphInfoVO graphInfoVO;

    //个人信息栏（包含会员信息）
    private UserInfoVO userInfoVO;

    private boolean membershipActive;

    private MembershipType membershipType;

    private boolean aiFeaturesEnabled;

    private UserCenterCountsVO countsVO;
}
