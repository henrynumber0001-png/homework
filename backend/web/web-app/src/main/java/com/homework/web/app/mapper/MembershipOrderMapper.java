package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.MembershipOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MembershipOrderMapper extends BaseMapper<MembershipOrder> {


    @Update("""
            UPDATE membership_order
            SET order_status = 4
            WHERE user_id = #{userId}
            AND action = 1
            AND order_status = 1
            AND payment_expired_time <= NOW()""")
    int fullPurchasePendingToExpired(@Param("userId") Long userId);


    @Update("""
UPDATE membership_order
SET order_status = 4
WHERE user_id = #{userId}
AND order_no = #{orderNo}
AND order_status = 1
AND action = 1
AND payment_expired_time <= NOW()""")
    int pendingToExpiredByOrderNo(@Param("userId") Long userId, @Param("orderNo") String orderNo);
}
