package com.homework.web.admin.vo;

import com.homework.model.enums.MembershipStatus;
import com.homework.model.enums.UserInfoStatus;
import lombok.Data;

import java.time.LocalDateTime;

/** 后台用户列表行。 */
@Data
public class UserRowVO {

    /** App 用户 ID。 */
    private Long id;

    /** 对外账号编号。 */
    private String accountNo;

    /** 用户昵称。 */
    private String displayName;

    /** 用户头像地址。 */
    private String avatar;

    /** 账号状态名称。 */
    private UserInfoStatus status;

    /** 当前最高有效会员等级。 */
    private MembershipStatus membershipType;

    /** 注册时间。 */
    private LocalDateTime registeredTime;

    /** 乐观锁版本。 */
    private Integer version;
}
