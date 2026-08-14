package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.Gender;
import com.homework.model.enums.UserInfoStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_info")
public class UserInfo extends BaseEntity {

    @Schema(description = "用户账号ID")
    private String accountNo;

    private String displayName;

    /** 用户主动上传的私有 COS 对象 key。 */
    private String avatarObjectKey;

    private String bannerObjectKey;

    /** 1.active;2.disabled;3.banned */
    private UserInfoStatus status;

    //主攻的技术方向
    private Long subTechDirectionId;

    private String companyOrSchool;

    private Gender gender;

    //个人简介
    private String introduction;

    /** 乐观锁版本。 */
    @Version
    private Integer version;
}
