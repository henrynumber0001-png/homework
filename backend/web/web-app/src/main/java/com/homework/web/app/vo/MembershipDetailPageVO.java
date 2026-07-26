package com.homework.web.app.vo;

import com.homework.model.enums.MembershipStatus;
import com.homework.model.enums.MembershipType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class MembershipDetailPageVO {

    private MembershipStatus memberStatus;

    private MembershipType currentMembershipType;

    private LocalDateTime currentExpireTime;

    private LocalDateTime baseFreezeExpireTime;

    private List<MembershipPlanCardVO> fullPurchaseCards;

    //是否展示补差升级功能
    private boolean diffUpgradeAvailable;

    //最大可展示的补差升级月份数
    private int maxDiffUpgradeMonths;

    private List<MembershipSkuVO> diffUpgradeOptions;
}
