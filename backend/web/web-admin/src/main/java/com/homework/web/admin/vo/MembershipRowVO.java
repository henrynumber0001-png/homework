package com.homework.web.admin.vo;

import com.homework.model.enums.MembershipStatus;
import lombok.Data;

import java.time.LocalDateTime;

/** 后台会员列表行。 */
@Data
public class MembershipRowVO {

    /** App 用户 ID。 */
    private Long userId;

    /** 用户账号编号。 */
    private String accountNo;

    /** 用户昵称。 */
    private String displayName;

    /** 当前最高有效会员等级。 */
    private MembershipStatus currentType;

    /** ACTIVE、SUSPENDED 或 EXPIRED。 */
    private String accessStatus;

    /** Premium 到期时间。 */
    private LocalDateTime premiumExpireTime;

    /** Premium Plus 到期时间。 */
    private LocalDateTime premiumPlusExpireTime;

    /** 是否被管理员暂停访问。 */
    private Boolean suspended;

    /** 合并会员台账版本。 */
    private Integer ledgerVersion;
}
