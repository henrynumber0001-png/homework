package com.homework.web.admin.vo;

import com.homework.model.enums.MembershipChangeType;
import com.homework.model.enums.MembershipType;
import lombok.Data;

import java.time.LocalDateTime;

/** 管理员会员变更流水。 */
@Data
public class MembershipChangeVO {

    /** 变更类型名称。 */
    private MembershipChangeType changeType;

    /** 涉及的会员等级。 */
    private MembershipType membershipType;

    /** 发放月数。 */
    private Integer durationMonths;

    /** 操作原因。 */
    private String reason;

    /** 操作管理员 ID。 */
    private Long adminId;

    /** 操作时间。 */
    private LocalDateTime createdTime;
}
