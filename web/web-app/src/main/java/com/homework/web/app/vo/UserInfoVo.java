package com.homework.web.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "返回前端的用户信息")
public class UserInfoVo {
    @Schema(description = "账户ID")
    private String accountNo;

    @Schema(description = "用户昵称")
    private String displayName;

    @Schema(description = "用户头像")
    private String avatar;
}
