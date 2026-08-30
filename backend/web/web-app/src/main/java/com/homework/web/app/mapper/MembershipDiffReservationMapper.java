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
            AND expires_time > NOW()""")
    int getSumReservationDays(@Param("userId") Long userId);


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
