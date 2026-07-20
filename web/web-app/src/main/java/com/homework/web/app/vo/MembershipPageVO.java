package com.homework.web.app.vo;

import java.util.List;
import lombok.Data;

@Data
public class MembershipPageVO {

    private MembershipSubscriptionVO currentSubscription;

    private List<MembershipPlanOptionVO> plans;
}
