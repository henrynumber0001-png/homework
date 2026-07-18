package com.homework.web.app.vo;

import com.homework.model.entity.PremiumUserInfo;
import com.homework.model.enums.PremiumOrderScope;
import com.homework.model.enums.PremiumStatus;
import lombok.Data;

import java.util.List;

@Data
public class UserCenterPageVO {

    //返回Banner的图片和图片上的文字
    private GraphInfoVO graphInfoVO;

    //个人信息栏（包含会员信息）
    private UserInfoVO userInfoVO;
    //     private List<PremiumUserInfoVO> premiumUserInfoVOList;


    //userCenter首次返回页面，目前的设计修改为：只需要返回 Premium标识，如果是普通Premium(Interview/Certificate)，则返回蓝色标识；如果是FullAccess，则返回皇冠颜色标识
    private boolean isPremium;
    private boolean isSuperPremium;

    private UserCenterCountsVO countsVO;
}
