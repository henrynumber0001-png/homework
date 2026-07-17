package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.PremiumOrderScope;
import com.homework.model.enums.PremiumStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("premium_user_info")
public class PremiumUserInfo extends BaseEntity {

    private Long userId;

    /** 当前权益类型。 */
    private PremiumOrderScope premiumScope;

    /** 最近一次成功扩展当前权益的已支付订单。 */
    private Long latestPaidOrderId;

    /** 当前连续权益的开始时间。 */
    private LocalDateTime startTime;

    /** 当前合并后的最终到期时间。 */
    private LocalDateTime expiredTime;

    private PremiumStatus status;
}
/*
premium_order.startTime/expiredTime：这笔订单购买的服务区间。
premium_user_info.startTime/expiredTime：用户当前合并后的实际权益区间。

二者的含义和权限不同，所以两个表的这两个字段都要保留。

然后，UNIQUE KEY uk_user_premium_scope (user_id, premium_scope)
数据库 唯一索引，约束“一个用户每种会员只能有一条当前权益记录”，这样能保证数据的稳定性
 */