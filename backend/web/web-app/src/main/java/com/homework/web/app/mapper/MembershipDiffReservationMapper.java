package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.MembershipDiffReservation;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface MembershipDiffReservationMapper extends BaseMapper<MembershipDiffReservation> {


    @Select("""
            SELECT COALESCE(SUM(reserved_days), 0)
            FROM membership_diff_reservation
            WHERE user_id = #{userId}
            AND is_deleted = 0
            AND reservation_status = 1
            AND expires_time > DATE_SUB(NOW(), INTERVAL 1 MINUTE)""")
    int getSumReservationDays(@Param("userId") Long userId);
    /*
14:45:00  创建补差订单 A，预占 31 天
14:59:59  用户完成订单 A 的微信付款
15:00:00  订单 A 到达支付截止时间
15:00:10  用户发起第二次补差购买，创建订单 B
15:00:30  微信才把订单 A 的成功支付回调发送给后端

如果使用：expires_time > NOW()

那么在 15:00:10：
- A 的 expires_time 是 15:00:00；
- 当前时间是 15:00:10；
- 15:00:00 > 15:00:10 不成立；
- A 预占的 31 天不会参与计算；
- 用户可能成功创建订单 B，再次预占这 31 天。
随后 A 的微信回调在 15:00:30 到达，因为实际支付时间是 14:59:59，后端仍可能确认 A 支付成功。
这就造成 A 已消费 31 天，同时 B 又预占了同一批 31 天。
     */


    @Update("""
            UPDATE membership_diff_reservation
            SET reservation_status = 3
            WHERE order_id = #{orderId}
            AND is_deleted = 0
            AND user_id = #{userId}
            AND reservation_status = 1
            """
            )
    int releaseByOrderId(@Param("orderId") Long orderId, @Param("userId") Long userId);
}
