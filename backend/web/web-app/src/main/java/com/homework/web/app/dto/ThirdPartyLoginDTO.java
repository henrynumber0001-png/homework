package com.homework.web.app.dto;

import com.homework.model.enums.UserAuthIdentityProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ThirdPartyLoginDTO {
    @Schema(description = "第三方渠道类型")
    private UserAuthIdentityProvider identityProvider;
    // account 不从前端接收；服务端验证 authCode 后从 id_token 的 sub/unionid/openid 中取得。

    @Schema(description = "授权码")
    private String authCode;

    @Schema(description = "turnstile token")
    private String turnstileToken;
}
