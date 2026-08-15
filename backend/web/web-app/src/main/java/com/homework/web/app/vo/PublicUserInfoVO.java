package com.homework.web.app.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.homework.model.enums.Gender;
import lombok.Data;

/** 公共主页用户资料；拉黑关系存在时，仅填充头像、封面和展示名称。 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicUserInfoVO {
    private String accountNo;
    private String displayName;
    private String avatarUrl;
    private String bannerUrl;
    private Long subTechDirectionId;
    private String companyOrSchool;
    private Gender gender;
    private String introduction;
}
