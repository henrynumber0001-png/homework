package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.DiffReservationStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("membership_diff_reservation")
public class MembershipDiffReservation extends BaseEntity {

    private Long orderId;

    private Long userId;

    private Integer reservedDays;

    private DiffReservationStatus reservationStatus;

    private LocalDateTime expiresTime;
}
