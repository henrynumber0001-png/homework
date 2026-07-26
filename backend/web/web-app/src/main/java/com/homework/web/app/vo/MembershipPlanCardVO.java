package com.homework.web.app.vo;

import com.homework.model.enums.MembershipType;
import java.util.List;
import lombok.Data;

/** Premium 或 Premium Plus 的全款套餐卡片。 */
@Data
public class MembershipPlanCardVO {

    private MembershipType membershipType;

    private List<MembershipSkuVO> fullPurchaseOptions;
}
