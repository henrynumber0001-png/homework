package com.homework.web.app.vo;

import com.homework.model.enums.Gender;
import com.homework.model.enums.MembershipType;
import com.homework.model.enums.MembershipStatus;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 当前用户会员中心和公开主页个人信息卡使用的会员信息。
 */
@Data
public class MembershipInfoVO {

    /** 用户展示名称。 */
    private String displayName;

    /** 用户头像地址。 */
    private String avatarUrl;

    /** 当前会员类型。 */
    private MembershipType membershipType;

    /** 当前会员的到期时间。 */
    private LocalDateTime expiredTime;

    /** 当前会员状态。 */
    private MembershipStatus memberStatus;

    /** 基础账户冻结状态的到期时间。 */
    private LocalDateTime baseFreezeExpireTime;

}
