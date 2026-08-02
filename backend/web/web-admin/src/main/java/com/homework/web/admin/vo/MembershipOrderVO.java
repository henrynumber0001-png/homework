package com.homework.web.admin.vo;

import com.homework.model.enums.MembershipOrderStatus;
import com.homework.model.enums.MembershipType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 后台会员订单列表行。 */
@Data
public class MembershipOrderVO {

    /** 订单编号。 */
    private String orderNo;

    /** App 用户 ID。 */
    private Long userId;

    /** 会员等级快照。 */
    private MembershipType membershipType;

    /** 套餐时长快照。 */
    private Integer durationMonths;

    /** 支付金额。 */
    private BigDecimal payAmount;

    /** 币种。 */
    private String currency;

    /** 订单状态名称。 */
    private MembershipOrderStatus orderStatus;

    /** 支付时间。 */
    private LocalDateTime payTime;

    /** 当前版本不开放退款，固定为 false。 */
    private Boolean refundable;
}
