package com.homework.web.app.dto;

import com.homework.model.enums.UserAuthIdentityProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ThirdPartyLoginDTO {
    @Schema(description = "第三方渠道类型")
    private UserAuthIdentityProvider identityProvider;
    //没有 identifier：Google验证 authCode 后自动发的id_token中的sub/union id/open_id，不说从前端拿的

    @Schema(description = "授权码")
    private String authCode;

    @Schema(description = "turnstile token")
    private String turnstileToken;
}
